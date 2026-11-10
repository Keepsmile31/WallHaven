package com.example.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.R
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BugLogEntry(
    val timestamp: String,
    val timestampMs: Long,
    val errorType: String,
    val errorMessage: String,
    val stackTrace: String,
    val context: String,
    val appVersion: String,
    val deviceInfo: String
)

/**
 * Unified logging function for modules to explicitly record errors with context.
 */
fun logError(error: Throwable?, context: String = "") {
    BugLogManager.logError(error, context)
}

/**
 * Unified logging function for modules to explicitly record message-only errors with context.
 */
fun logError(message: String, context: String = "", errorType: String = "AppError") {
    BugLogManager.logError(message, context, errorType)
}

object BugLogManager {

    const val MAX_LOG_ENTRIES = 50
    private const val TAG = "BugLogManager"
    private const val LOGS_DIR_NAME = "logs"
    private const val LOG_STORE_FILE_NAME = "bug_logs.json"
    private const val MAX_STACK_TRACE_CHARS = 8000

    private val lock = Any()
    private val entries = ArrayDeque<BugLogEntry>(MAX_LOG_ENTRIES)

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var isInitialized = false

    private val _logCount = MutableStateFlow(0)
    val logCount: StateFlow<Int> = _logCount.asStateFlow()

    val coroutineExceptionHandler = CoroutineExceptionHandler { coroutineCtx, throwable ->
        logError(throwable, "CoroutineException[$coroutineCtx]")
    }

    fun init(context: Context) {
        synchronized(lock) {
            appContext = context.applicationContext
            if (!isInitialized) {
                loadFromDiskLocked()
                installUncaughtHandlerLocked()
                isInitialized = true
            }
        }
    }

    private fun installUncaughtHandlerLocked() {
        val current = Thread.getDefaultUncaughtExceptionHandler()
        if (current !is WallhavenCrashHandler) {
            Thread.setDefaultUncaughtExceptionHandler(WallhavenCrashHandler(current))
        }
    }

    fun logError(error: Throwable?, context: String = "") {
        val type = error?.javaClass?.name ?: "UnknownError"
        val message = error?.message?.takeIf { it.isNotBlank() }
            ?: error?.toString()
            ?: "No error message"
        val trace = if (error != null) {
            Log.getStackTraceString(error).ifBlank { error.stackTraceToString() }
        } else {
            Thread.currentThread().stackTrace.joinToString("\n")
        }
        recordEntry(
            errorType = type,
            errorMessage = message,
            stackTrace = trace,
            context = context.ifBlank { "General" }
        )
    }

    fun logError(message: String, context: String = "", errorType: String = "AppError") {
        val trace = Throwable(message).let { Log.getStackTraceString(it) }
        recordEntry(
            errorType = errorType,
            errorMessage = message.ifBlank { "No error message" },
            stackTrace = trace,
            context = context.ifBlank { "General" }
        )
    }

    private fun recordEntry(
        errorType: String,
        errorMessage: String,
        stackTrace: String,
        context: String
    ) {
        val nowMs = System.currentTimeMillis()
        val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
        val entry = BugLogEntry(
            timestamp = isoFormatter.format(Date(nowMs)),
            timestampMs = nowMs,
            errorType = errorType,
            errorMessage = errorMessage,
            stackTrace = stackTrace.take(MAX_STACK_TRACE_CHARS),
            context = context,
            appVersion = currentAppVersion(),
            deviceInfo = currentDeviceInfo()
        )

        synchronized(lock) {
            entries.addLast(entry)
            while (entries.size > MAX_LOG_ENTRIES) {
                entries.removeFirst()
            }
            _logCount.value = entries.size
            persistToDiskLocked()
        }
    }

    fun getLogs(): List<BugLogEntry> {
        synchronized(lock) {
            return entries.toList()
        }
    }

    fun getLogCount(): Int {
        synchronized(lock) {
            return entries.size
        }
    }

    fun clearLogs() {
        synchronized(lock) {
            entries.clear()
            _logCount.value = 0
            persistToDiskLocked()
        }
    }

    fun exportLogsToFile(context: Context): Result<File> {
        return try {
            val ctx = context.applicationContext
            if (appContext == null) {
                init(ctx)
            }
            val logsSnapshot = getLogs()
            val cacheLogsDir = File(ctx.cacheDir, LOGS_DIR_NAME).apply {
                if (!exists()) mkdirs()
            }

            // Clean up older exported .log files to prevent cache bloat
            cacheLogsDir.listFiles { file ->
                file.isFile && file.name.startsWith("wallhaven_debug_logs_") && file.name.endsWith(".log")
            }?.forEach { oldFile ->
                runCatching { oldFile.delete() }
            }

            val fileStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val exportFile = File(cacheLogsDir, "wallhaven_debug_logs_$fileStamp.log")

            val exportTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(Date())
            val content = buildString {
                appendLine("============================================================")
                appendLine("WallHaven Debug & Bug Log Report")
                appendLine("============================================================")
                appendLine("Exported At : $exportTime")
                appendLine("App Version : ${currentAppVersion()}")
                appendLine("Package     : ${BuildConfig.APPLICATION_ID}")
                appendLine("Device Info : ${currentDeviceInfo()}")
                appendLine("Log Entries : ${logsSnapshot.size} / $MAX_LOG_ENTRIES")
                appendLine("============================================================")
                appendLine()

                if (logsSnapshot.isEmpty()) {
                    appendLine("No unhandled exceptions or error logs recorded.")
                } else {
                    logsSnapshot.asReversed().forEachIndexed { index, entry ->
                        appendLine("-------------------- [Entry #${index + 1}] --------------------")
                        appendLine("Timestamp   : ${entry.timestamp} (${entry.timestampMs})")
                        appendLine("Error Type  : ${entry.errorType}")
                        appendLine("Context     : ${entry.context}")
                        appendLine("Message     : ${entry.errorMessage}")
                        appendLine("App Version : ${entry.appVersion}")
                        appendLine("Device Info : ${entry.deviceInfo}")
                        appendLine("Stack Trace :")
                        appendLine(entry.stackTrace.trimEnd())
                        appendLine()
                    }
                }
            }

            exportFile.writeText(content, Charsets.UTF_8)
            Result.success(exportFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export debug logs", e)
            Result.failure(e)
        }
    }

    fun shareDebugLogs(context: Context): Result<Unit> {
        return try {
            val exportResult = exportLogsToFile(context)
            val logFile = exportResult.getOrThrow()
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, logFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "WallHaven Debug Logs v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                )
                clipData = ClipData.newRawUri(logFile.name, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooserTitle = runCatching {
                context.getString(R.string.export_debug_logs)
            }.getOrDefault("Export Debug Logs")
            val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            logError(e, "BugLogManager.shareDebugLogs")
            Result.failure(e)
        }
    }

    private fun currentAppVersion(): String {
        return "${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
    }

    private fun currentDeviceInfo(): String {
        val abis = Build.SUPPORTED_ABIS?.joinToString(",") ?: "unknown"
        val locale = Locale.getDefault().toLanguageTag()
        return "${Build.MANUFACTURER} ${Build.MODEL} (Brand: ${Build.BRAND}, Product: ${Build.PRODUCT}) | " +
            "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}) | ABI: $abis | Locale: $locale"
    }

    private fun getStoreFileLocked(): File? {
        val ctx = appContext ?: return null
        val dir = File(ctx.filesDir, LOGS_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, LOG_STORE_FILE_NAME)
    }

    private fun persistToDiskLocked() {
        val file = getStoreFileLocked() ?: return
        try {
            val array = JSONArray()
            entries.forEach { entry ->
                val obj = JSONObject().apply {
                    put("timestamp", entry.timestamp)
                    put("timestampMs", entry.timestampMs)
                    put("errorType", entry.errorType)
                    put("errorMessage", entry.errorMessage)
                    put("stackTrace", entry.stackTrace)
                    put("context", entry.context)
                    put("appVersion", entry.appVersion)
                    put("deviceInfo", entry.deviceInfo)
                }
                array.put(obj)
            }
            file.writeText(array.toString(), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist bug logs", e)
        }
    }

    private fun loadFromDiskLocked() {
        val file = getStoreFileLocked() ?: return
        if (!file.exists()) {
            _logCount.value = entries.size
            return
        }
        try {
            val raw = file.readText(Charsets.UTF_8)
            if (raw.isBlank()) {
                _logCount.value = entries.size
                return
            }
            val array = JSONArray(raw)
            entries.clear()
            val startIndex = maxOf(0, array.length() - MAX_LOG_ENTRIES)
            for (i in startIndex until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                entries.addLast(
                    BugLogEntry(
                        timestamp = obj.optString("timestamp", ""),
                        timestampMs = obj.optLong("timestampMs", 0L),
                        errorType = obj.optString("errorType", "UnknownError"),
                        errorMessage = obj.optString("errorMessage", ""),
                        stackTrace = obj.optString("stackTrace", ""),
                        context = obj.optString("context", "General"),
                        appVersion = obj.optString("appVersion", currentAppVersion()),
                        deviceInfo = obj.optString("deviceInfo", currentDeviceInfo())
                    )
                )
            }
            _logCount.value = entries.size
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load persisted bug logs", e)
        }
    }

    private class WallhavenCrashHandler(
        private val previousHandler: Thread.UncaughtExceptionHandler?
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            try {
                logError(throwable, "UncaughtException[thread=${thread.name}]")
            } catch (_: Throwable) {
                // Never throw inside an uncaught exception handler
            } finally {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
