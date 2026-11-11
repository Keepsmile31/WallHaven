package com.example.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import com.example.data.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

enum class WallpaperTarget {
    HOME,
    LOCK,
    BOTH
}

class WallpaperSetHelper(private val context: Context) {

    suspend fun setWallpaperFromUrl(url: String, target: WallpaperTarget): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val bitmap = ApiClient.baseOkHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("无法获取壁纸原图 HTTP ${response.code}"))
                    }
                    val inputStream = response.body?.byteStream()
                        ?: return@withContext Result.failure(Exception("壁纸数据流为空"))

                    BitmapFactory.decodeStream(inputStream)
                        ?: return@withContext Result.failure(Exception("无法解析壁纸图像"))
                }

                val wallpaperManager = WallpaperManager.getInstance(context)

                val flags = when (target) {
                    WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                    WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                    WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                wallpaperManager.setBitmap(bitmap, null, true, flags)
                Result.success(true)
            } catch (e: Exception) {
                logError(e, "WallpaperSetHelper.setWallpaperFromUrl(target=$target)")
                Result.failure(e)
            }
        }
}
