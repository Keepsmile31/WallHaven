package com.example.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.Immutable
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.api.ApiClient
import com.example.data.db.AppDatabase
import com.example.data.db.DownloadEntity
import com.example.data.model.WallpaperDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Immutable
data class DownloadStatus(
    val wallpaperId: String,
    val progress: Int,
    val isFinished: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val fileUri: String? = null
)

@Immutable
data class BatchDownloadResult(
    val total: Int,
    val successCount: Int,
    val failCount: Int
)

class WallpaperDownloadManager(
    private val context: Context,
    private val database: AppDatabase,
    private val hapticHelper: HapticHelper
) {
    companion object {
        const val CHANNEL_ID = "wallpaper_downloads"
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val downloadOkHttpClient: OkHttpClient by lazy {
        ApiClient.baseOkHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val _downloadStatusMap = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadStatusMap = _downloadStatusMap.asStateFlow()

    private val _isBatchDownloading = MutableStateFlow(false)
    val isBatchDownloading = _isBatchDownloading.asStateFlow()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_download_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.channel_download_desc)
            setShowBadge(true)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)

        // Clean up legacy "Live Update" notification channels
        try {
            val legacyIds = listOf(
                "wallpaper_live_updates",
                "live_update",
                "live_updates",
                "channel_live_update",
                "live_update_channel",
                "wallpaper_live_update"
            )
            for (id in legacyIds) {
                notificationManager.deleteNotificationChannel(id)
            }
            val existingChannels = notificationManager.notificationChannels
            existingChannels?.forEach { ch ->
                val idLower = ch.id.lowercase()
                val nameLower = ch.name.toString().lowercase()
                if (idLower.contains("live") || nameLower.contains("live") || nameLower.contains("实时更新")) {
                    notificationManager.deleteNotificationChannel(ch.id)
                }
            }
        } catch (_: Exception) {}
    }

    private fun hasNotificationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun downloadWallpaper(
        wallpaper: WallpaperDto,
        enableHaptic: Boolean = true,
        downloadDir: String = "",
        onProgress: ((Int) -> Unit)? = null
    ): Result<Uri> = withContext(Dispatchers.IO) {
        val notifId = wallpaper.id.hashCode()
        updateStatus(wallpaper.id, DownloadStatus(wallpaperId = wallpaper.id, progress = 0))

        val canNotify = hasNotificationPermission()
        val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notifBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("正在下载壁纸 #${wallpaper.id}")
            .setContentText("WallHaven #${wallpaper.id} (${wallpaper.resolution})")
            .setContentIntent(contentPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, 0, false)

        if (canNotify) {
            try {
                notificationManager.notify(notifId, notifBuilder.build())
            } catch (_: SecurityException) {}
        }

        try {
            // Build candidate download URLs with fallback
            val prefix = wallpaper.id.take(2)
            val primaryExt = if (wallpaper.fileType.contains("png", true)) "png" else "jpg"
            val secondaryExt = if (primaryExt == "png") "jpg" else "png"

            val urlCandidates = buildList {
                if (wallpaper.path.isNotBlank()) add(wallpaper.path)
                add("https://w.wallhaven.cc/full/$prefix/wallhaven-${wallpaper.id}.$primaryExt")
                add("https://w.wallhaven.cc/full/$prefix/wallhaven-${wallpaper.id}.$secondaryExt")
            }.distinct()

            var response: Response? = null
            var finalUrl = ""

            for (candidateUrl in urlCandidates) {
                try {
                    val request = Request.Builder()
                        .url(candidateUrl)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                        .header("Referer", "https://wallhaven.cc/")
                        .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                        .build()
                    val resp = downloadOkHttpClient.newCall(request).execute()
                    if (resp.isSuccessful && resp.body != null) {
                        response = resp
                        finalUrl = candidateUrl
                        break
                    } else {
                        resp.close()
                    }
                } catch (_: Exception) {}
            }

            if (response == null || !response.isSuccessful) {
                throw RuntimeException("壁纸源服务器连接失败，请检查网络")
            }

            val body = response.body ?: throw RuntimeException("响应内容为空")
            val totalLength = body.contentLength()
            val isPng = finalUrl.endsWith(".png", true) || wallpaper.fileType.contains("png", true)
            val mimeType = if (isPng) "image/png" else "image/jpeg"
            val extension = if (isPng) "png" else "jpg"
            val displayName = "wallhaven-${wallpaper.id}.$extension"

            // Target Android 14+ (minSdk 34) Scoped Storage
            val uri: Uri = if (downloadDir.contains("Download", ignoreCase = true)) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download/WallHaven")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            } else {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/WallHaven")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            } ?: throw RuntimeException("无法在系统存储中创建媒体文件")

            var bytesCopied = 0L
            var lastProgress = 0
            val inputStream: InputStream = body.byteStream()
            val outputStream: OutputStream = context.contentResolver.openOutputStream(uri)
                ?: throw RuntimeException("无法打开写入流")

            outputStream.use { out ->
                inputStream.use { inp ->
                    val buffer = ByteArray(16 * 1024)
                    var bytes = inp.read(buffer)
                    while (bytes >= 0) {
                        out.write(buffer, 0, bytes)
                        bytesCopied += bytes
                        if (totalLength > 0) {
                            val progress = ((bytesCopied * 100) / totalLength).toInt().coerceIn(0, 99)
                            if (progress != lastProgress && progress % 5 == 0) {
                                lastProgress = progress
                                onProgress?.invoke(progress)
                                updateStatus(
                                    wallpaper.id,
                                    DownloadStatus(wallpaperId = wallpaper.id, progress = progress)
                                )
                                if (canNotify) {
                                    notifBuilder.setProgress(100, progress, false)
                                    try {
                                        notificationManager.notify(notifId, notifBuilder.build())
                                    } catch (_: SecurityException) {}
                                }
                            }
                        }
                        bytes = inp.read(buffer)
                    }
                    out.flush()
                }
            }

            // Mark IS_PENDING = 0
            val finishValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            context.contentResolver.update(uri, finishValues, null, null)

            // Record in Database
            database.downloadDao().insertDownload(
                DownloadEntity(
                    id = wallpaper.id,
                    thumbUrl = wallpaper.thumbs.bestThumb,
                    fullPath = wallpaper.path,
                    resolution = wallpaper.resolution,
                    fileSize = bytesCopied,
                    localUri = uri.toString(),
                    downloadedAt = System.currentTimeMillis()
                )
            )

            // Success notification
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notifId,
                viewIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (canNotify) {
                val finishNotif = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.download_success))
                    .setContentText("wallhaven-${wallpaper.id} 已成功保存至相册")
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setProgress(0, 0, false)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()
                try {
                    notificationManager.notify(notifId, finishNotif)
                } catch (_: SecurityException) {}
            }

            hapticHelper.vibrateSuccess(enableHaptic)
            updateStatus(
                wallpaper.id,
                DownloadStatus(
                    wallpaperId = wallpaper.id,
                    progress = 100,
                    isFinished = true,
                    fileUri = uri.toString()
                )
            )

            Result.success(uri)
        } catch (e: Exception) {
            if (e !is kotlinx.coroutines.CancellationException) {
                logError(e, "WallpaperDownloadManager.downloadWallpaper(id=${wallpaper.id})")
            }
            if (canNotify) {
                val errorNotif = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.download_failed))
                    .setContentText(e.message ?: "下载发生错误")
                    .setAutoCancel(true)
                    .setProgress(0, 0, false)
                    .build()
                try {
                    notificationManager.notify(notifId, errorNotif)
                } catch (_: SecurityException) {}
            }

            updateStatus(
                wallpaper.id,
                DownloadStatus(
                    wallpaperId = wallpaper.id,
                    progress = 0,
                    isError = true,
                    errorMessage = e.message
                )
            )
            Result.failure(e)
        }
    }

    suspend fun batchDownloadWallpapers(
        wallpapers: List<WallpaperDto>,
        concurrentLimit: Int = 3,
        downloadDir: String = "",
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): BatchDownloadResult = withContext(Dispatchers.IO) {
        if (wallpapers.isEmpty()) return@withContext BatchDownloadResult(0, 0, 0)

        _isBatchDownloading.value = true
        val total = wallpapers.size
        val successCounter = AtomicInteger(0)
        val failCounter = AtomicInteger(0)
        val completedCounter = AtomicInteger(0)

        val canNotify = hasNotificationPermission()
        val batchNotifId = 9002

        val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val batchPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val batchNotifBuilder = if (canNotify) {
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("批量下载壁纸 (0/$total)")
                .setContentText("正在开始下载壁纸...")
                .setContentIntent(batchPendingIntent)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, 0, false)
        } else null

        if (batchNotifBuilder != null) {
            try {
                notificationManager.notify(batchNotifId, batchNotifBuilder.build())
            } catch (_: Exception) {}
        }

        val semaphore = Semaphore(concurrentLimit.coerceIn(1, 6))

        coroutineScope {
            val tasks = wallpapers.map { wp ->
                async {
                    semaphore.withPermit {
                        val result = downloadWallpaper(
                            wallpaper = wp,
                            enableHaptic = false,
                            downloadDir = downloadDir
                        )
                        if (result.isSuccess) {
                            successCounter.incrementAndGet()
                        } else {
                            failCounter.incrementAndGet()
                        }
                        val done = completedCounter.incrementAndGet()
                        val progressPercent = (done * 100) / total
                        onProgress(done, total)

                        if (batchNotifBuilder != null) {
                            batchNotifBuilder
                                .setContentTitle("批量下载壁纸 ($done/$total)")
                                .setContentText("已完成 $done / $total 张 ($progressPercent%)")
                                .setProgress(100, progressPercent, false)
                            try {
                                notificationManager.notify(batchNotifId, batchNotifBuilder.build())
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
            tasks.awaitAll()
        }

        if (batchNotifBuilder != null) {
            try {
                notificationManager.cancel(batchNotifId)
            } catch (_: Exception) {}
        }

        _isBatchDownloading.value = false
        val successes = successCounter.get()
        val fails = failCounter.get()

        if (successes > 0 && fails == 0) {
            hapticHelper.vibrateSuccess(true)
        } else if (successes > 0) {
            hapticHelper.vibrateTick(true)
        } else {
            hapticHelper.vibrateClick(true)
        }

        if (canNotify) {
            val summaryNotif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("批量下载已完成")
                .setContentText("成功下载 $successes 张，失败 $fails 张")
                .setContentIntent(batchPendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            try {
                notificationManager.notify(batchNotifId + 1, summaryNotif)
            } catch (_: Exception) {}
        }

        BatchDownloadResult(
            total = total,
            successCount = successes,
            failCount = fails
        )
    }

    private fun updateStatus(id: String, status: DownloadStatus) {
        val current = _downloadStatusMap.value.toMutableMap()
        current[id] = status
        _downloadStatusMap.value = current
    }
}
