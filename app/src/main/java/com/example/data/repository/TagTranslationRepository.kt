package com.example.data.repository

import androidx.compose.runtime.Immutable
import com.example.data.api.ApiClient
import com.example.data.db.AppDatabase
import com.example.data.db.TagFtsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.Request
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

@Immutable
data class TagSuggestion(
    val englishTag: String,
    val chineseName: String,
    val category: String = ""
)

class TagTranslationRepository(private val database: AppDatabase) {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val englishToChineseMap = ConcurrentHashMap<String, String>()
    private val chineseToEnglishMap = ConcurrentHashMap<String, String>()
    private val pendingTranslations = ConcurrentHashMap.newKeySet<String>()

    private val _tagTranslations = MutableStateFlow<Map<String, String>>(emptyMap())
    val tagTranslations: StateFlow<Map<String, String>> = _tagTranslations.asStateFlow()

    init {
        // Pre-populate in-memory dictionary for instant O(1) synchronous lookup
        val initialMap = mutableMapOf<String, String>()
        DefaultTagsDictionary.TAG_PAIRS.forEach { (en, zh) ->
            val normalizedEn = en.lowercase().trim()
            val cleanZh = zh.trim()
            englishToChineseMap[normalizedEn] = cleanZh
            chineseToEnglishMap[cleanZh] = normalizedEn
            initialMap[normalizedEn] = cleanZh

            // Also map variations without hyphens and underscores
            val variation = normalizedEn.replace('-', ' ').replace('_', ' ')
            if (variation != normalizedEn) {
                englishToChineseMap[variation] = cleanZh
                initialMap[variation] = cleanZh
            }
        }
        _tagTranslations.value = initialMap
    }

    /**
     * Seeds Room FTS4 database asynchronously on startup with all dictionary tags if missing.
     */
    suspend fun initializeDatabaseIfNeeded() = withContext(Dispatchers.IO) {
        try {
            database.tagFtsDao().purgeCorruptedTags()
            val count = database.tagFtsDao().getTagCount()
            if (count < DefaultTagsDictionary.TAG_PAIRS.size) {
                val entities = DefaultTagsDictionary.TAG_PAIRS.mapIndexed { index, (en, zh) ->
                    TagFtsEntity(
                        rowid = index + 1,
                        englishTag = en.lowercase().trim(),
                        chineseName = zh.trim()
                    )
                }
                database.tagFtsDao().insertAll(entities)
            }
        } catch (_: Exception) {
            // FTS table handled gracefully
        }
    }

    /**
     * Translates an English tag into Simplified Chinese synchronously.
     * Uses in-memory cache, morphological singular/plural analysis, and compound rule decomposition.
     */
    fun translateToChinese(englishTag: String): String? {
        val raw = englishTag.lowercase().trim()
        if (raw.isBlank()) return null

        // 1. Direct hit in memory
        englishToChineseMap[raw]?.let { return it }

        // 2. Normalized without underscores, hyphens, extra whitespace
        val cleaned = raw.replace('_', ' ').replace('-', ' ').replace(Regex("\\s+"), " ")
        englishToChineseMap[cleaned]?.let { return it }

        // 3. Morphological analysis (plural -> singular)
        smartMorphologyTranslate(cleaned)?.let {
            cacheTranslation(raw, it, persistToDb = false)
            return it
        }

        // 4. Compound token decomposition
        smartCompoundTranslate(cleaned)?.let {
            cacheTranslation(raw, it, persistToDb = false)
            return it
        }

        return null
    }

    /**
     * Handles plural suffixes: -ies -> -y, -es, -s
     */
    private fun smartMorphologyTranslate(tag: String): String? {
        if (tag.endsWith("ies") && tag.length > 4) {
            val singular = tag.removeSuffix("ies") + "y"
            englishToChineseMap[singular]?.let { return it }
        }
        if (tag.endsWith("es") && tag.length > 3) {
            val singular1 = tag.removeSuffix("es")
            englishToChineseMap[singular1]?.let { return it }
            val singular2 = tag.removeSuffix("s")
            englishToChineseMap[singular2]?.let { return it }
        }
        if (tag.endsWith("s") && tag.length > 2) {
            val singular = tag.removeSuffix("s")
            englishToChineseMap[singular]?.let { return it }
        }
        return null
    }

    /**
     * Decomposes compound modifier + noun tags (e.g. "dark blue hair", "sunset sky", "pixel art")
     */
    private fun smartCompoundTranslate(tag: String): String? {
        val tokens = tag.split(" ").filter { it.isNotBlank() }
        if (tokens.size in 2..4) {
            val modifiers = mapOf(
                "dark" to "深色/暗", "light" to "浅色/明亮", "blue" to "蓝", "red" to "红",
                "black" to "黑", "white" to "白", "green" to "绿", "yellow" to "黄",
                "purple" to "紫", "pink" to "粉", "gold" to "金", "golden" to "金色",
                "silver" to "银", "short" to "短", "long" to "长", "night" to "夜景/夜晚",
                "sunset" to "日落", "sunrise" to "日出", "starry" to "星空/星辰",
                "rainy" to "雨景/雨天", "snowy" to "雪景/下雪", "autumn" to "金秋",
                "spring" to "初春", "summer" to "盛夏", "winter" to "冬日",
                "cyberpunk" to "赛博朋克", "futuristic" to "未来主义", "fantasy" to "奇幻",
                "sci-fi" to "科幻", "scifi" to "科幻", "anime" to "动漫", "cute" to "萌系可爱",
                "digital" to "数码", "pixel" to "像素", "vintage" to "复古", "retro" to "怀旧"
            )

            val nouns = mapOf(
                "girl" to "少女", "girls" to "少女", "boy" to "少年", "boys" to "少年",
                "hair" to "发", "eye" to "眼瞳", "eyes" to "眼眸", "dress" to "礼服连衣裙",
                "skirt" to "短裙", "sky" to "天空", "city" to "城市", "cities" to "城市",
                "landscape" to "风景", "scenery" to "景致", "forest" to "森林", "sea" to "大海",
                "ocean" to "海洋", "mountain" to "山峰", "mountains" to "群山",
                "tree" to "树木", "trees" to "林木", "flower" to "花卉", "flowers" to "鲜花",
                "cat" to "猫咪", "cats" to "猫咪", "dog" to "狗狗", "car" to "汽车", "cars" to "跑车名车",
                "art" to "艺术", "background" to "背景", "cloud" to "云彩", "clouds" to "白云",
                "star" to "星辰", "stars" to "繁星", "sword" to "剑", "swords" to "双剑/利剑"
            )

            val translatedTokens = tokens.map { token ->
                englishToChineseMap[token] ?: modifiers[token] ?: nouns[token]
            }

            if (translatedTokens.all { it != null }) {
                return translatedTokens.joinToString("")
            }
        }
        return null
    }

    /**
     * Prefetches translations asynchronously for all tags belonging to a wallpaper.
     * Hits in-memory first; then rule normalizer; then Room FTS;
     * and for any remaining tags, queries Google Translate GTX service on-the-fly and saves to Room.
     */
    suspend fun prefetchTranslations(tags: List<String>) = withContext(Dispatchers.IO) {
        val missing = tags.map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .filter { raw ->
                val norm = raw.lowercase()
                !englishToChineseMap.containsKey(norm) && !pendingTranslations.contains(norm)
            }

        if (missing.isEmpty()) return@withContext

        // Mark as pending to avoid duplicate requests
        missing.forEach { pendingTranslations.add(it.lowercase()) }

        coroutineScope {
            missing.map { tag ->
                async {
                    val normalized = tag.lowercase()
                    try {
                        // 1. Try local rule / morphology
                        val localHit = translateToChinese(tag)
                        if (localHit != null) {
                            cacheTranslation(normalized, localHit, persistToDb = true)
                            return@async
                        }

                        // 2. Try Room DB
                        val dbHit = database.tagFtsDao().findByEnglishTag(normalized)
                        if (dbHit != null) {
                            cacheTranslation(normalized, dbHit.chineseName, persistToDb = false)
                            return@async
                        }

                        // 3. Dynamic online translation query
                        val onlineZh = fetchOnlineTranslation(tag)
                        if (!onlineZh.isNullOrBlank()) {
                            cacheTranslation(normalized, onlineZh, persistToDb = true)
                        }
                    } catch (_: Exception) {
                        // Fail silently for network errors
                    } finally {
                        pendingTranslations.remove(normalized)
                    }
                }
            }.awaitAll()
        }
    }

    /**
     * Queries Google Translate lightweight endpoint for rapid on-the-fly tag translation.
     */
    private fun fetchOnlineTranslation(englishTag: String): String? {
        return try {
            val encoded = URLEncoder.encode(englishTag, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=zh-CN&dt=t&q=$encoded"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            ApiClient.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null

                // Parse standard JSON array: [[["中文","english",...],...]]
                val jsonArray = org.json.JSONArray(body)
                val sentencesArray = jsonArray.optJSONArray(0) ?: return null
                val sb = StringBuilder()
                for (i in 0 until sentencesArray.length()) {
                    val sentence = sentencesArray.optJSONArray(i) ?: continue
                    val text = sentence.optString(0)
                    if (text.isNotBlank() && text != "null") {
                        sb.append(text)
                    }
                }
                var translated = sb.toString().trim()
                // Strict safeguard: Strip any concatenated hexadecimal hash strings (e.g. 6ffafab0da7e640be86ac09d0d5e539c)
                translated = translated.replace(Regex("""[0-9a-fA-F]{16,}"""), "").trim()

                if (translated.isNotBlank() && !translated.equals(englishTag, ignoreCase = true)) {
                    return translated
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Stores translation in memory, updates reactive StateFlow, and optionally persists to Room FTS.
     */
    private fun cacheTranslation(englishTag: String, chineseName: String, persistToDb: Boolean) {
        val norm = englishTag.lowercase().trim()
        val zh = chineseName.replace(Regex("""[0-9a-fA-F]{16,}"""), "").trim()
        if (zh.isBlank()) return
        englishToChineseMap[norm] = zh
        chineseToEnglishMap[zh] = norm

        _tagTranslations.update { current ->
            current + (norm to zh)
        }

        if (persistToDb) {
            try {
                val entity = TagFtsEntity(
                    rowid = 0,
                    englishTag = norm,
                    chineseName = zh,
                    category = ""
                )
                repositoryScope.launch {
                    try {
                        database.tagFtsDao().insertAll(listOf(entity))
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Real-time fuzzy auto-suggestions for search input.
     * Searches both Room FTS4 table and in-memory cache.
     * Returns matching TagSuggestion items in (Chinese - English Tag) format.
     */
    suspend fun searchSuggestions(query: String, limit: Int = 12): List<TagSuggestion> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList()

        try {
            // First query Room FTS4 database for fast full-text match
            val dbResults = database.tagFtsDao().searchTags(q, limit)
            if (dbResults.isNotEmpty()) {
                return@withContext dbResults.map { TagSuggestion(it.englishTag, it.chineseName, it.category) }
            }
        } catch (_: Exception) {
            // Fallback to in-memory dictionary matching
        }

        // In-memory fallback fuzzy search
        val qLower = q.lowercase()
        val inMemoryMatches = DefaultTagsDictionary.TAG_PAIRS
            .filter { (en, zh) ->
                zh.contains(q, ignoreCase = true) || en.contains(qLower)
            }
            .sortedBy { (en, zh) ->
                when {
                    zh.equals(q, ignoreCase = true) -> 1
                    en.equals(qLower, ignoreCase = true) -> 2
                    zh.startsWith(q, ignoreCase = true) -> 3
                    en.startsWith(qLower, ignoreCase = true) -> 4
                    else -> 5
                }
            }
            .take(limit)
            .map { (en, zh) -> TagSuggestion(en, zh) }

        return@withContext inMemoryMatches
    }

    /**
     * Resolves user input: If user typed Chinese, translates to English keyword for Wallhaven API.
     * e.g. "天使" -> "angel", "经典艺术" -> "classic art"
     */
    fun resolveSearchQuery(query: String): String {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return ""

        // Check if query contains Chinese characters
        val hasChinese = trimmed.any { it in '\u4e00'..'\u9fa5' }
        if (!hasChinese) {
            return trimmed
        }

        // Direct match in Chinese -> English dictionary
        val directMatch = chineseToEnglishMap[trimmed]
        if (directMatch != null) {
            return directMatch
        }

        // Partial match: find best matching tag
        val matchedPair = DefaultTagsDictionary.TAG_PAIRS.firstOrNull { (_, zh) ->
            zh.contains(trimmed) || trimmed.contains(zh)
        }
        if (matchedPair != null) {
            return matchedPair.first
        }

        return trimmed
    }
}
