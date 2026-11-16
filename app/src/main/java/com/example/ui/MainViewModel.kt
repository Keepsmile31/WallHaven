package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.db.AppDatabase
import com.example.data.db.DownloadEntity
import com.example.data.db.FavoriteWallpaperEntity
import com.example.data.db.HistoryEntity
import com.example.data.db.SearchHistoryEntity
import com.example.data.model.WallpaperDto
import com.example.data.preferences.FilterMode
import com.example.data.preferences.AppSettings
import com.example.data.preferences.UpdatePrefs
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.AppUpdateInfo
import com.example.data.repository.AppUpdateRepository
import com.example.data.repository.TagSuggestion
import com.example.data.repository.TagTranslationRepository
import com.example.data.repository.UpdateCheckResult
import com.example.data.repository.WallpaperRepository
import com.example.ui.components.FilterState
import com.example.util.BatchDownloadResult
import com.example.util.DownloadStatus
import com.example.util.HapticHelper
import com.example.util.WallpaperDownloadManager
import com.example.util.WallpaperSetHelper
import com.example.util.logError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class TabType(val id: Int) {
    LATEST(0),
    HOT(1),
    RANDOM(2),
    TOP(3),
    SEARCH(4);

    companion object {
        fun fromExploreSubTab(subTab: Int): TabType = when (subTab) {
            0 -> LATEST
            1 -> HOT
            2 -> RANDOM
            else -> TOP
        }
    }
}

data class DetailReturnEntry(
    val wallpaper: WallpaperDto,
    val returnTab: Int,
    val scrollOffset: Int
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val database = AppDatabase.getInstance(application)
    val preferencesManager = UserPreferencesManager(application)
    val repository = WallpaperRepository(database)
    val tagTranslationRepository = TagTranslationRepository(database)
    val hapticHelper = HapticHelper(application)
    val downloadManager = WallpaperDownloadManager(application, database, hapticHelper)
    val wallpaperSetHelper = WallpaperSetHelper(application)
    val backupManager = BackupManager(application, preferencesManager, database)
    val updatePrefs = UpdatePrefs(application)
    val appUpdateRepository = AppUpdateRepository(updatePrefs)

    // Current Bottom Tab: 0=Explore, 1=Search, 2=Favorites, 3=History, 4=Settings
    private val _currentTab = MutableStateFlow(0)
    val currentTab = _currentTab.asStateFlow()

    // 4 Explore Sub-Tabs: 0=Latest, 1=Hot, 2=Random, 3=Top
    private val _exploreSubTab = MutableStateFlow(3)
    val exploreSubTab = _exploreSubTab.asStateFlow()

    // Approach 1: Global Shared FilterState
    private val _globalFilter = MutableStateFlow(
        FilterState(sorting = "toplist", topRange = "1M", categories = "111", purity = "110")
    )

    // Approach 2: Independent FilterState cache for each Tab
    private val _tabFilters = mutableMapOf(
        TabType.LATEST to FilterState(sorting = "date_added", categories = "111", purity = "110", topRange = "1M"),
        TabType.HOT to FilterState(sorting = "hot", categories = "111", purity = "110", topRange = "1M"),
        TabType.RANDOM to FilterState(sorting = "random", categories = "111", purity = "110", topRange = "1M"),
        TabType.TOP to FilterState(sorting = "toplist", topRange = "1M", categories = "111", purity = "110"),
        TabType.SEARCH to FilterState(categories = "111", purity = "110", sorting = "relevance", topRange = "1M")
    )

    private val _currentExploreFilter = MutableStateFlow(_tabFilters[TabType.TOP] ?: FilterState())
    val currentExploreFilter = _currentExploreFilter.asStateFlow()

    // Explore Wallpapers State
    private val _exploreWallpapers = MutableStateFlow<List<WallpaperDto>>(emptyList())
    val exploreWallpapers = _exploreWallpapers.asStateFlow()

    private val _isExploreLoading = MutableStateFlow(false)
    val isExploreLoading = _isExploreLoading.asStateFlow()

    private val _exploreError = MutableStateFlow<String?>(null)
    val exploreError = _exploreError.asStateFlow()

    private var explorePage = 1
    private val _isExploreEndReached = MutableStateFlow(false)
    val isExploreEndReached = _isExploreEndReached.asStateFlow()

    // Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Real-time tag auto-suggestions and translations
    private val _tagSuggestions = MutableStateFlow<List<TagSuggestion>>(emptyList())
    val tagSuggestions = _tagSuggestions.asStateFlow()
    val tagTranslations: StateFlow<Map<String, String>> = tagTranslationRepository.tagTranslations
    private var suggestionsJob: Job? = null

    private val _searchFilterState = MutableStateFlow(_tabFilters[TabType.SEARCH] ?: FilterState(categories = "111", purity = "100", sorting = "relevance"))
    val searchFilterState = _searchFilterState.asStateFlow()

    private val _selectedSearchColor = MutableStateFlow("")
    val selectedSearchColor = _selectedSearchColor.asStateFlow()

    private val _searchResults = MutableStateFlow<List<WallpaperDto>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isSearchLoading = MutableStateFlow(false)
    val isSearchLoading = _isSearchLoading.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError = _searchError.asStateFlow()

    private var searchPage = 1
    private val _isSearchEndReached = MutableStateFlow(false)
    val isSearchEndReached = _isSearchEndReached.asStateFlow()

    // Detail Sheet State
    private val _selectedWallpaper = MutableStateFlow<WallpaperDto?>(null)
    val selectedWallpaper = _selectedWallpaper.asStateFlow()

    private val _detailReturnStack = MutableStateFlow<List<DetailReturnEntry>>(emptyList())
    val detailReturnStack = _detailReturnStack.asStateFlow()

    private val _pendingDetailScrollOffset = MutableStateFlow(0)
    val pendingDetailScrollOffset = _pendingDetailScrollOffset.asStateFlow()

    private val _pendingUpdateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val pendingUpdateInfo = _pendingUpdateInfo.asStateFlow()
    private var hasCheckedColdStartUpdate = false

    val settings: StateFlow<AppSettings> = preferencesManager.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val isSearchActive: StateFlow<Boolean> = combine(
        _searchQuery,
        _searchResults,
        _isSearchLoading,
        _searchError,
        _selectedSearchColor
    ) { query, results, loading, error, color ->
        query.isNotEmpty() || results.isNotEmpty() || loading || error != null || color.isNotBlank()
    }.combine(_searchFilterState) { baseActive, filter ->
        baseActive to filter
    }.combine(settings) { (baseActive, filter), currentSettings ->
        baseActive || (currentSettings.filterMode != FilterMode.GLOBAL_SHARED && filter.hasActiveFilters(isSearch = true))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Local DB Flows
    val favorites: StateFlow<List<FavoriteWallpaperEntity>> = repository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favoriteIds: StateFlow<Set<String>> = favorites
        .map { list -> list.mapTo(HashSet(list.size)) { it.id } }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    val browseHistory: StateFlow<List<HistoryEntity>> = repository.getHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val downloadHistory: StateFlow<List<DownloadEntity>> = repository.getDownloads()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val searchHistory: StateFlow<List<SearchHistoryEntity>> = repository.getSearchHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val downloadStatusMap: StateFlow<Map<String, DownloadStatus>> = downloadManager.downloadStatusMap
    val isBatchDownloading: StateFlow<Boolean> = downloadManager.isBatchDownloading

    private val _isSettingsLoaded = MutableStateFlow(false)
    val isSettingsLoaded = _isSettingsLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            tagTranslationRepository.initializeDatabaseIfNeeded()
        }

        viewModelScope.launch {
            val currentSettings = preferencesManager.getSettings()
            val hasApiKey = currentSettings.apiKey.isNotBlank()
            val defaultPurity = if (hasApiKey) "111" else "110"
            val effectivePurity = when {
                currentSettings.purity == "100" -> defaultPurity
                !hasApiKey && currentSettings.purity.length >= 3 && currentSettings.purity[2] == '1' -> currentSettings.purity.substring(0, 2) + "0"
                else -> currentSettings.purity
            }
            // Initialize default categories and purity on filters
            _tabFilters.keys.forEach { k ->
                val f = _tabFilters[k]
                if (f != null) {
                    _tabFilters[k] = f.copy(categories = "111", purity = effectivePurity)
                }
            }
            _globalFilter.value = _globalFilter.value.copy(categories = "111", purity = effectivePurity)

            val currentTabType = TabType.fromExploreSubTab(_exploreSubTab.value)
            _currentExploreFilter.value = getFilterStateForTab(currentTabType)
            _searchFilterState.value = getFilterStateForTab(TabType.SEARCH)

            _isSettingsLoaded.value = true
            loadExploreWallpapers(reset = true)
            backupManager.checkAndRunDailyAutoBackup()

            // Automatic cache clearing every three days
            if (currentSettings.autoClearCacheMode == "every_3_days") {
                val now = System.currentTimeMillis()
                val threeDaysMs = 3L * 24 * 60 * 60 * 1000L
                if (currentSettings.lastCacheClearTime == 0L || (now - currentSettings.lastCacheClearTime) >= threeDaysMs) {
                    com.example.util.CacheManager.clearCache(getApplication())
                    preferencesManager.setLastCacheClearTime(now)
                }
            }
        }

        // Observe filterMode changes dynamically (skip initial emission to avoid cold-start duplicate fetch)
        viewModelScope.launch {
            preferencesManager.settingsFlow.drop(1).collect { newSettings ->
                val currentTabType = TabType.fromExploreSubTab(_exploreSubTab.value)
                val targetExploreFilter = if (newSettings.filterMode == FilterMode.GLOBAL_SHARED) {
                    _globalFilter.value
                } else {
                    _tabFilters[currentTabType] ?: defaultFilterForTab(currentTabType)
                }
                if (_currentExploreFilter.value != targetExploreFilter) {
                    _currentExploreFilter.value = targetExploreFilter
                    loadExploreWallpapers(reset = true)
                }

                val targetSearchFilter = if (newSettings.filterMode == FilterMode.GLOBAL_SHARED) {
                    _globalFilter.value
                } else {
                    _tabFilters[TabType.SEARCH] ?: defaultFilterForTab(TabType.SEARCH)
                }
                _searchFilterState.value = targetSearchFilter
            }
        }
    }

    fun selectTab(tab: Int, clearDetailReturn: Boolean = true) {
        if (clearDetailReturn && _detailReturnStack.value.isNotEmpty()) {
            _detailReturnStack.value = emptyList()
        }
        _currentTab.value = tab
    }

    fun defaultFilterForTab(tab: TabType): FilterState {
        val hasApiKey = settings.value.apiKey.isNotBlank()
        val defaultPurity = if (hasApiKey) "111" else "110"
        val purity = when {
            settings.value.purity == "100" -> defaultPurity
            !hasApiKey && settings.value.purity.length >= 3 && settings.value.purity[2] == '1' -> settings.value.purity.substring(0, 2) + "0"
            else -> settings.value.purity
        }
        return when (tab) {
            TabType.LATEST -> FilterState(sorting = "date_added", categories = "111", purity = purity, topRange = "1M")
            TabType.HOT -> FilterState(sorting = "hot", categories = "111", purity = purity, topRange = "1M")
            TabType.RANDOM -> FilterState(sorting = "random", categories = "111", purity = purity, topRange = "1M")
            TabType.TOP -> FilterState(sorting = "toplist", categories = "111", purity = purity, topRange = "1M")
            TabType.SEARCH -> FilterState(categories = "111", purity = purity, sorting = "relevance", topRange = "1M")
        }
    }

    /**
     * Unified Filter Query Method:
     * Returns the global filter if GLOBAL_SHARED is active, or the tab-specific filter if TAB_INDEPENDENT.
     */
    fun getFilterStateForTab(tab: TabType): FilterState {
        return if (settings.value.filterMode == FilterMode.GLOBAL_SHARED) {
            _globalFilter.value
        } else {
            _tabFilters[tab] ?: defaultFilterForTab(tab)
        }
    }

    /**
     * Unified Filter Update Method:
     * Depending on the active mode, automatically writes the new filter criteria to either
     * the global slot or the tab slot, then triggers reactive updates.
     */
    fun updateFilter(tab: TabType, newState: FilterState) {
        if (settings.value.filterMode == FilterMode.GLOBAL_SHARED) {
            _globalFilter.value = newState
            _tabFilters[tab] = newState
            _currentExploreFilter.value = newState
            _searchFilterState.value = newState
        } else {
            _tabFilters[tab] = newState
            if (tab == TabType.SEARCH) {
                _searchFilterState.value = newState
            } else if (tab == TabType.fromExploreSubTab(_exploreSubTab.value)) {
                _currentExploreFilter.value = newState
            }
        }

        if (tab == TabType.SEARCH) {
            performSearch(reset = true)
        } else {
            loadExploreWallpapers(reset = true)
        }
    }

    fun selectExploreSubTab(subTab: Int) {
        if (_exploreSubTab.value == subTab) return
        _exploreSubTab.value = subTab
        val tabType = TabType.fromExploreSubTab(subTab)
        _currentExploreFilter.value = getFilterStateForTab(tabType)
        loadExploreWallpapers(reset = true)
    }

    fun applyExploreFilter(newFilter: FilterState) {
        val tabType = TabType.fromExploreSubTab(_exploreSubTab.value)
        updateFilter(tabType, newFilter)
    }

    fun resetExploreFilter() {
        val tabType = TabType.fromExploreSubTab(_exploreSubTab.value)
        updateFilter(tabType, defaultFilterForTab(tabType))
    }

    fun removeSingleExploreFilter(type: String) {
        val tabType = TabType.fromExploreSubTab(_exploreSubTab.value)
        val current = getFilterStateForTab(tabType)
        val updated = when (type) {
            "scenes", "scene" -> current.copy(scenes = emptySet())
            "ratios", "ratio" -> current.copy(ratios = emptySet())
            "resolutions", "resolution" -> current.copy(resolutions = emptySet())
            "atleast" -> current.copy(atleast = "")
            "color" -> current.copy(color = "")
            "order" -> current.copy(order = "desc")
            "categories" -> current.copy(categories = "110")
            "purity" -> current.copy(purity = "100")
            "topRange" -> current.copy(topRange = "1M")
            else -> current
        }
        updateFilter(tabType, updated)
    }

    private var exploreJob: Job? = null
    private var searchJob: Job? = null

    fun loadExploreWallpapers(reset: Boolean = false) {
        if (reset) {
            exploreJob?.cancel()
            explorePage = 1
            _isExploreEndReached.value = false
            _exploreError.value = null
            // Do not clear _exploreWallpapers immediately if it already has items
            // Keeping previous items while isExploreLoading is true prevents screen blanking
        } else {
            if (_isExploreLoading.value || _isExploreEndReached.value) return
        }

        exploreJob = viewModelScope.launch {
            _isExploreLoading.value = true
            _exploreError.value = null
            try {
                val currentSettings = preferencesManager.getSettings()
                val filter = _currentExploreFilter.value
                val isRandom = _exploreSubTab.value == 2
                val isTop = _exploreSubTab.value == 3

                val sorting = when (_exploreSubTab.value) {
                    0 -> "date_added"
                    1 -> "hot"
                    2 -> "random"
                    3 -> "toplist"
                    else -> filter.sorting
                }

                // On Random tab, Wallhaven doesn't support scene query or toplist range
                val query = if (!isRandom && filter.scenes.isNotEmpty()) filter.scenes.joinToString(" ") else null
                val topRange = if (isTop) filter.topRange else null
                val ratiosParam = if (filter.ratios.isNotEmpty()) filter.ratios.joinToString(",") else null
                val resolutionsParam = if (filter.resolutions.isNotEmpty()) filter.resolutions.joinToString(",") else null
                val atleastParam = filter.atleast.ifBlank { null }
                val colorsParam = filter.color.ifBlank { null }

                val response = repository.searchWallpapers(
                    query = query,
                    categories = filter.categories,
                    purity = filter.purity,
                    sorting = sorting,
                    order = filter.order,
                    topRange = topRange,
                    atleast = atleastParam,
                    resolutions = resolutionsParam,
                    ratios = ratiosParam,
                    colors = colorsParam,
                    page = explorePage,
                    apiKey = currentSettings.apiKey
                )

                val newItems = response.data
                if (reset) {
                    _exploreWallpapers.value = newItems
                } else {
                    _exploreWallpapers.value = _exploreWallpapers.value + newItems
                }

                val lastPage = response.meta?.lastPage ?: 1
                if (explorePage >= lastPage || newItems.isEmpty()) {
                    _isExploreEndReached.value = true
                } else {
                    explorePage++
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logError(e, "MainViewModel.loadExploreWallpapers(page=$explorePage, subTab=${_exploreSubTab.value})")
                _exploreError.value = "加载壁纸失败，请检查网络连接 (${e.message ?: "未知错误"})"
            } finally {
                _isExploreLoading.value = false
            }
        }
    }

    // Search operations
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        suggestionsJob?.cancel()
        if (query.isEmpty()) {
            searchJob?.cancel()
            _tagSuggestions.value = emptyList()
            _isSearchLoading.value = false
            _searchError.value = null
            _searchResults.value = emptyList()
            _isSearchEndReached.value = false
            searchPage = 1
        } else if (query.isBlank() || !settings.value.tagLocalization) {
            _tagSuggestions.value = emptyList()
        } else {
            suggestionsJob = viewModelScope.launch {
                _tagSuggestions.value = tagTranslationRepository.searchSuggestions(query)
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        suggestionsJob?.cancel()
        _searchQuery.value = ""
        _tagSuggestions.value = emptyList()
        _isSearchLoading.value = false
        _searchError.value = null
        _searchResults.value = emptyList()
        _isSearchEndReached.value = false
        searchPage = 1
    }

    fun exitSearchToSearchHome() {
        clearSearch()
        _selectedSearchColor.value = ""
        if (settings.value.filterMode != FilterMode.GLOBAL_SHARED) {
            val defaultSearch = defaultFilterForTab(TabType.SEARCH)
            _tabFilters[TabType.SEARCH] = defaultSearch
            _searchFilterState.value = defaultSearch
        }
    }

    fun selectTagSuggestion(suggestion: TagSuggestion) {
        _searchQuery.value = suggestion.englishTag
        _tagSuggestions.value = emptyList()
        performSearch(reset = true)
    }

    fun selectSearchHistory(query: String) {
        _searchQuery.value = query
        _tagSuggestions.value = emptyList()
        performSearch(reset = true)
    }

    fun deleteSearchHistoryItem(query: String) {
        viewModelScope.launch {
            repository.deleteSearchHistoryItem(query)
            hapticHelper.vibrateClick(settings.value.hapticFeedback)
        }
    }

    fun batchDeleteSearchHistory(queries: List<String>) {
        viewModelScope.launch {
            repository.batchDeleteSearchHistory(queries)
            hapticHelper.vibrateSuccess(settings.value.hapticFeedback)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
            hapticHelper.vibrateSuccess(settings.value.hapticFeedback)
        }
    }

    fun clearTagSuggestions() {
        _tagSuggestions.value = emptyList()
    }

    fun translateTag(englishTag: String): String? {
        return tagTranslationRepository.translateToChinese(englishTag)
    }

    fun applySearchFilter(filter: FilterState) {
        updateFilter(TabType.SEARCH, filter)
    }

    fun resetSearchFilter() {
        _selectedSearchColor.value = ""
        updateFilter(TabType.SEARCH, defaultFilterForTab(TabType.SEARCH))
    }

    fun removeSingleSearchFilter(type: String) {
        val current = getFilterStateForTab(TabType.SEARCH)
        val updated = when (type) {
            "scenes", "scene" -> current.copy(scenes = emptySet())
            "ratios", "ratio" -> current.copy(ratios = emptySet())
            "resolutions", "resolution" -> current.copy(resolutions = emptySet())
            "atleast" -> current.copy(atleast = "")
            "sorting" -> current.copy(sorting = "relevance")
            "order" -> current.copy(order = "desc")
            "categories" -> current.copy(categories = "111")
            "purity" -> current.copy(purity = "100")
            "color" -> {
                _selectedSearchColor.value = ""
                current.copy(color = "")
            }
            "topRange" -> current.copy(topRange = "1M")
            else -> current
        }
        updateFilter(TabType.SEARCH, updated)
    }

    fun setSearchColor(color: String) {
        _selectedSearchColor.value = if (_selectedSearchColor.value == color) "" else color
        performSearch(reset = true)
    }

    fun searchFromDetailTag(
        englishTag: String,
        localizedTag: String?,
        detailScrollOffset: Int
    ) {
        val currentWallpaper = _selectedWallpaper.value
        if (currentWallpaper != null) {
            _detailReturnStack.value = _detailReturnStack.value + DetailReturnEntry(
                wallpaper = currentWallpaper,
                returnTab = _currentTab.value,
                scrollOffset = detailScrollOffset.coerceAtLeast(0)
            )
        }
        _pendingDetailScrollOffset.value = 0
        _selectedWallpaper.value = null
        _currentTab.value = 1

        val isLocalized = settings.value.tagLocalization
        val resolvedChinese = if (isLocalized) {
            localizedTag?.trim()?.ifBlank { null }
                ?: tagTranslationRepository.translateToChinese(englishTag)
        } else {
            null
        }

        if (resolvedChinese != null) {
            tagTranslationRepository.registerTranslationMapping(
                englishTag = englishTag,
                chineseName = resolvedChinese
            )
        }

        suggestionsJob?.cancel()
        _searchQuery.value = resolvedChinese ?: englishTag
        _tagSuggestions.value = emptyList()
        performSearch(reset = true, resolvedQueryOverride = englishTag)
    }

    fun searchFromDetailColor(colorHex: String, detailScrollOffset: Int) {
        val currentWallpaper = _selectedWallpaper.value
        if (currentWallpaper != null) {
            _detailReturnStack.value = _detailReturnStack.value + DetailReturnEntry(
                wallpaper = currentWallpaper,
                returnTab = _currentTab.value,
                scrollOffset = detailScrollOffset.coerceAtLeast(0)
            )
        }
        _pendingDetailScrollOffset.value = 0
        _selectedWallpaper.value = null
        _currentTab.value = 1
        _selectedSearchColor.value = colorHex
        performSearch(reset = true)
    }

    fun navigateBackFromDetailReturn() {
        val stack = _detailReturnStack.value
        val entry = stack.lastOrNull() ?: return
        _detailReturnStack.value = stack.dropLast(1)
        _currentTab.value = entry.returnTab
        _pendingDetailScrollOffset.value = entry.scrollOffset.coerceAtLeast(0)
        _selectedWallpaper.value = entry.wallpaper
        if (settings.value.tagLocalization && entry.wallpaper.tags.isNotEmpty()) {
            viewModelScope.launch {
                tagTranslationRepository.prefetchTranslations(entry.wallpaper.tags.map { it.name })
            }
        }
    }

    fun consumePendingDetailScroll() {
        _pendingDetailScrollOffset.value = 0
    }

    fun performSearch(reset: Boolean = true, resolvedQueryOverride: String? = null) {
        _tagSuggestions.value = emptyList()
        val rawQuery = _searchQuery.value.trim()
        val initialResolvedQuery = when {
            !resolvedQueryOverride.isNullOrBlank() -> resolvedQueryOverride.trim()
            settings.value.tagLocalization -> tagTranslationRepository.resolveSearchQuery(rawQuery)
            else -> rawQuery
        }
        val filter = _searchFilterState.value
        val hasCriteria = initialResolvedQuery.isNotBlank() ||
                rawQuery.isNotBlank() ||
                _selectedSearchColor.value.isNotBlank() ||
                filter.hasActiveFilters(isSearch = true)

        if (!hasCriteria) {
            searchJob?.cancel()
            _isSearchLoading.value = false
            _searchError.value = null
            _searchResults.value = emptyList()
            _isSearchEndReached.value = false
            searchPage = 1
            return
        }

        if (reset) {
            searchJob?.cancel()
            searchPage = 1
            _isSearchEndReached.value = false
            _searchError.value = null
            if (rawQuery.isNotBlank()) {
                viewModelScope.launch {
                    repository.recordSearchQuery(rawQuery)
                }
            }
            // Do not clear _searchResults immediately if it already has items
        } else {
            if (_isSearchLoading.value || _isSearchEndReached.value) return
        }

        searchJob = viewModelScope.launch {
            _isSearchLoading.value = true
            _searchError.value = null
            val resolvedQuery = if (!resolvedQueryOverride.isNullOrBlank()) {
                resolvedQueryOverride.trim()
            } else if (settings.value.tagLocalization) {
                tagTranslationRepository.resolveSearchQueryWithDb(rawQuery)
            } else {
                rawQuery
            }
            try {
                val currentSettings = preferencesManager.getSettings()
                val scenesQuery = if (filter.scenes.isNotEmpty()) filter.scenes.joinToString(" ") else ""
                val fullQuery = if (resolvedQuery.isNotBlank() && scenesQuery.isNotBlank()) {
                    "$resolvedQuery $scenesQuery"
                } else resolvedQuery.ifBlank { scenesQuery.ifBlank { null } }

                val colorsParam = filter.color.ifBlank {
                    _selectedSearchColor.value.ifBlank { null }
                }
                val ratiosParam = if (filter.ratios.isNotEmpty()) filter.ratios.joinToString(",") else null
                val resolutionsParam = if (filter.resolutions.isNotEmpty()) filter.resolutions.joinToString(",") else null
                val atleastParam = filter.atleast.ifBlank { null }
                val topRangeParam = if (filter.sorting == "toplist") filter.topRange else null

                val response = repository.searchWallpapers(
                    query = fullQuery,
                    colors = colorsParam,
                    sorting = filter.sorting,
                    order = filter.order,
                    topRange = topRangeParam,
                    categories = filter.categories,
                    purity = filter.purity,
                    atleast = atleastParam,
                    resolutions = resolutionsParam,
                    ratios = ratiosParam,
                    page = searchPage,
                    apiKey = currentSettings.apiKey
                )

                val newItems = response.data
                if (reset) {
                    _searchResults.value = newItems
                } else {
                    _searchResults.value = _searchResults.value + newItems
                }

                val lastPage = response.meta?.lastPage ?: 1
                if (searchPage >= lastPage || newItems.isEmpty()) {
                    _isSearchEndReached.value = true
                } else {
                    searchPage++
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logError(e, "MainViewModel.performSearch(query=$initialResolvedQuery, page=$searchPage)")
                _searchError.value = "搜索失败: ${e.message ?: "网络异常"}"
            } finally {
                _isSearchLoading.value = false
            }
        }
    }

    // Batch operations for Favorites Screen
    fun batchDownloadWallpapers(
        wallpapers: List<WallpaperDto>,
        onComplete: (BatchDownloadResult) -> Unit
    ) {
        viewModelScope.launch {
            val appSettings = preferencesManager.getSettings()
            val result = downloadManager.batchDownloadWallpapers(
                wallpapers = wallpapers,
                concurrentLimit = appSettings.concurrentDownloads,
                downloadDir = appSettings.downloadDirectory
            )
            onComplete(result)
        }
    }

    fun batchUnfavorite(wallpaperIds: List<String>) {
        viewModelScope.launch {
            repository.batchRemoveFavorites(wallpaperIds)
            hapticHelper.vibrateSuccess(settings.value.hapticFeedback)
        }
    }

    // Detail & Single item operations
    fun selectWallpaper(wallpaper: WallpaperDto) {
        _pendingDetailScrollOffset.value = 0
        _selectedWallpaper.value = wallpaper
        if (settings.value.tagLocalization && wallpaper.tags.isNotEmpty()) {
            viewModelScope.launch {
                tagTranslationRepository.prefetchTranslations(wallpaper.tags.map { it.name })
            }
        }
        viewModelScope.launch {
            repository.recordHistory(wallpaper)
            try {
                val currentSettings = preferencesManager.getSettings()
                val detailed = repository.getWallpaperDetails(wallpaper.id, currentSettings.apiKey)
                if (_selectedWallpaper.value?.id == wallpaper.id) {
                    _selectedWallpaper.value = detailed
                    if (detailed.tags.isNotEmpty()) {
                        tagTranslationRepository.prefetchTranslations(detailed.tags.map { it.name })
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logError(e, "MainViewModel.selectWallpaper(id=${wallpaper.id})")
            }
        }
    }

    fun dismissDetailSheet() {
        _pendingDetailScrollOffset.value = 0
        _selectedWallpaper.value = null
    }

    fun toggleFavorite(wallpaper: WallpaperDto) {
        viewModelScope.launch {
            val enabled = settings.value.hapticFeedback
            val favorited = repository.toggleFavorite(wallpaper)
            if (favorited) {
                hapticHelper.vibrateSuccess(enabled)
            } else {
                hapticHelper.vibrateClick(enabled)
            }
        }
    }

    fun downloadWallpaper(wallpaper: WallpaperDto) {
        viewModelScope.launch {
            val appSettings = preferencesManager.getSettings()
            downloadManager.downloadWallpaper(
                wallpaper = wallpaper,
                enableHaptic = settings.value.hapticFeedback,
                downloadDir = appSettings.downloadDirectory
            )
        }
    }

    fun deleteHistoryItem(id: String) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
            hapticHelper.vibrateClick(settings.value.hapticFeedback)
        }
    }

    fun batchDeleteHistory(ids: List<String>) {
        viewModelScope.launch {
            repository.batchDeleteHistory(ids)
            hapticHelper.vibrateSuccess(settings.value.hapticFeedback)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            hapticHelper.vibrateSuccess(settings.value.hapticFeedback)
        }
    }

    fun deleteDownloadItem(id: String) {
        viewModelScope.launch {
            repository.deleteDownloadItem(id)
            hapticHelper.vibrateClick(settings.value.hapticFeedback)
        }
    }

    fun batchDeleteDownloads(ids: List<String>) {
        viewModelScope.launch {
            repository.batchDeleteDownloads(ids)
            hapticHelper.vibrateSuccess(settings.value.hapticFeedback)
        }
    }

    fun clearDownloads() {
        viewModelScope.launch {
            repository.clearDownloads()
            hapticHelper.vibrateSuccess(settings.value.hapticFeedback)
        }
    }

    fun acceptDisclaimer(confirmedAt: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            preferencesManager.setDisclaimerAccepted(true, acceptedAt = confirmedAt)
        }
    }

    fun checkForUpdatesOnColdStart() {
        if (hasCheckedColdStartUpdate) return
        hasCheckedColdStartUpdate = true
        viewModelScope.launch {
            val result = appUpdateRepository.checkForUpdate(isColdStart = true)
            if (result is UpdateCheckResult.UpdateAvailable) {
                _pendingUpdateInfo.value = result.info
            }
        }
    }

    fun dismissUpdateDialog() {
        _pendingUpdateInfo.value = null
    }

    fun dismissUpdateLater() {
        appUpdateRepository.remindLaterToday()
        _pendingUpdateInfo.value = null
    }

    fun ignoreUpdateVersion(tagName: String) {
        appUpdateRepository.ignoreVersion(tagName)
        _pendingUpdateInfo.value = null
    }

    fun clearCacheNow(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            com.example.util.CacheManager.clearCache(getApplication())
            preferencesManager.setLastCacheClearTime(System.currentTimeMillis())
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}
