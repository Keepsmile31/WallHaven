package com.example.util

import android.content.Context
import coil.Coil
import java.io.File
import java.util.Locale

/**
 * Manages application image and response cache calculation and cleanup.
 */
object CacheManager {

    private fun getCacheSizeBytes(context: Context): Long {
        return getFolderSize(context.cacheDir)
    }

    fun getFormattedCacheSize(context: Context): String {
        val bytes = getCacheSizeBytes(context)
        return formatBytes(bytes)
    }

    private fun getFolderSize(file: File?): Long {
        if (file == null || !file.exists()) return 0L
        if (file.isFile) return file.length()
        var size = 0L
        file.listFiles()?.forEach { child ->
            size += if (child.isDirectory) getFolderSize(child) else child.length()
        }
        return size
    }

    fun clearCache(context: Context): Boolean {
        return try {
            try {
                Coil.imageLoader(context).apply {
                    memoryCache?.clear()
                    diskCache?.clear()
                }
            } catch (_: Exception) {}

            deleteDirContents(context.cacheDir)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun deleteDirContents(dir: File?): Boolean {
        if (dir == null || !dir.exists() || !dir.isDirectory) return false
        var success = true
        dir.listFiles()?.forEach { file ->
            success = if (file.isDirectory) {
                deleteDirContents(file) && file.delete() && success
            } else {
                file.delete() && success
            }
        }
        return success
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 KB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.getDefault(), "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            else -> String.format(Locale.getDefault(), "%.1f KB", kb)
        }
    }
}
