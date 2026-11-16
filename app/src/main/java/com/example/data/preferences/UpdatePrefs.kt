package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

/**
 * Local preference utility managing GitHub Releases update check throttling (once per day)
 * and per-version smart ignore state.
 */
class UpdatePrefs(
    context: Context,
    private val todayProvider: () -> String = { LocalDate.now().toString() }
) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLastCheckDate(): String? = prefs.getString(KEY_LAST_CHECK_DATE, null)

    fun setLastCheckDate(date: String) {
        prefs.edit().putString(KEY_LAST_CHECK_DATE, date).apply()
    }

    fun hasCheckedToday(today: String = todayProvider()): Boolean =
        getLastCheckDate() == today

    fun markCheckedToday(today: String = todayProvider()) {
        setLastCheckDate(today)
    }

    /**
     * Cold-start automated checks only execute once per calendar day (yyyy-MM-dd).
     */
    fun shouldAutoCheckOnColdStart(today: String = todayProvider()): Boolean =
        !hasCheckedToday(today)

    fun getIgnoredVersion(): String? = prefs.getString(KEY_IGNORED_VERSION_TAG, null)

    fun setIgnoredVersion(versionTag: String) {
        val normalized = versionTag.trim()
        if (normalized.isNotEmpty()) {
            prefs.edit().putString(KEY_IGNORED_VERSION_TAG, normalized).apply()
        }
    }

    fun clearIgnoredVersion() {
        prefs.edit().remove(KEY_IGNORED_VERSION_TAG).apply()
    }

    /**
     * Returns true if [remoteVersionTag] is equal to or older than the locally ignored version tag.
     * Once a strictly newer version is released on GitHub, prompts automatically resume.
     */
    fun isVersionIgnored(remoteVersionTag: String): Boolean {
        val ignored = getIgnoredVersion()?.takeIf { it.isNotBlank() } ?: return false
        return compareVersions(remoteVersionTag, ignored) <= 0
    }

    fun currentDate(): String = todayProvider()

    companion object {
        private const val PREFS_NAME = "wallhaven_update_prefs"
        private const val KEY_LAST_CHECK_DATE = "last_check_date"
        private const val KEY_IGNORED_VERSION_TAG = "ignored_version_tag"

        fun normalizeVersionTag(raw: String): String =
            raw.trim().removePrefix("v").removePrefix("V")

        /**
         * Compares two semantic-like version strings (e.g. "v2.6.2" vs "2.6.1").
         * Returns > 0 if [remote] > [local], 0 if equal, < 0 if [remote] < [local].
         */
        fun compareVersions(remote: String, local: String): Int {
            val remoteParts = parseNumericSegments(remote)
            val localParts = parseNumericSegments(local)
            val maxLen = maxOf(remoteParts.size, localParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val l = localParts.getOrElse(i) { 0 }
                if (r != l) return r.compareTo(l)
            }
            return 0
        }

        fun isNewerVersion(remote: String, current: String): Boolean =
            compareVersions(remote, current) > 0

        private fun parseNumericSegments(raw: String): List<Int> {
            val core = normalizeVersionTag(raw)
                .substringBefore('-')
                .substringBefore('+')
            if (core.isBlank()) return emptyList()
            return core.split('.').map { segment ->
                segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
            }
        }
    }
}
