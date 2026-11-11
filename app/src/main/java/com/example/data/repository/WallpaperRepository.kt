package com.example.data.repository

import com.example.data.api.ApiClient
import com.example.data.api.WallhavenApiService
import com.example.data.db.AppDatabase
import com.example.data.db.DownloadEntity
import com.example.data.db.FavoriteWallpaperEntity
import com.example.data.db.HistoryEntity
import com.example.data.model.SearchResponse
import com.example.data.model.WallpaperDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class WallpaperRepository(
    private val database: AppDatabase,
    private val apiService: WallhavenApiService = ApiClient.apiService
) {
    suspend fun searchWallpapers(
        query: String? = null,
        categories: String? = null,
        purity: String? = null,
        sorting: String? = null,
        order: String? = "desc",
        topRange: String? = null,
        atleast: String? = null,
        ratios: String? = null,
        resolutions: String? = null,
        colors: String? = null,
        page: Int = 1,
        seed: String? = null,
        apiKey: String? = null
    ): SearchResponse {
        return apiService.searchWallpapers(
            query = if (query.isNullOrBlank()) null else query,
            categories = categories,
            purity = purity,
            sorting = sorting,
            order = order,
            topRange = topRange,
            atleast = if (atleast.isNullOrBlank()) null else atleast,
            ratios = if (ratios.isNullOrBlank()) null else ratios,
            resolutions = if (resolutions.isNullOrBlank()) null else resolutions,
            colors = if (colors.isNullOrBlank()) null else colors,
            page = page,
            seed = seed,
            apiKey = if (apiKey.isNullOrBlank()) null else apiKey
        )
    }

    suspend fun getWallpaperDetails(id: String, apiKey: String? = null): WallpaperDto {
        return apiService.getWallpaper(id, if (apiKey.isNullOrBlank()) null else apiKey).data
    }

    fun getFavorites(): Flow<List<FavoriteWallpaperEntity>> =
        database.favoriteDao().getAllFavorites()

    suspend fun toggleFavorite(wallpaper: WallpaperDto): Boolean {
        val currentlyFav = database.favoriteDao().isFavorite(wallpaper.id).first()
        if (currentlyFav) {
            database.favoriteDao().deleteFavoriteById(wallpaper.id)
            return false
        } else {
            database.favoriteDao().insertFavorite(FavoriteWallpaperEntity.fromDto(wallpaper))
            return true
        }
    }

    suspend fun batchRemoveFavorites(ids: List<String>) {
        database.favoriteDao().deleteFavoritesByIds(ids)
    }

    suspend fun recordHistory(wallpaper: WallpaperDto) {
        database.historyDao().insertHistory(
            HistoryEntity(
                id = wallpaper.id,
                url = wallpaper.url,
                thumbUrl = wallpaper.thumbs.bestThumb,
                fullPath = wallpaper.path,
                resolution = wallpaper.resolution,
                category = wallpaper.category,
                ratio = wallpaper.ratio,
                viewedAt = System.currentTimeMillis()
            )
        )
    }

    fun getHistory(): Flow<List<HistoryEntity>> =
        database.historyDao().getHistory()

    suspend fun deleteHistoryItem(id: String) {
        database.historyDao().deleteHistoryById(id)
    }

    suspend fun batchDeleteHistory(ids: List<String>) {
        if (ids.isEmpty()) return
        ids.chunked(500).forEach { chunk ->
            database.historyDao().deleteHistoryByIds(chunk)
        }
    }

    suspend fun clearHistory() {
        database.historyDao().clearAll()
    }

    fun getDownloads(): Flow<List<DownloadEntity>> =
        database.downloadDao().getDownloads()

    suspend fun deleteDownloadItem(id: String) {
        database.downloadDao().deleteDownloadById(id)
    }

    suspend fun batchDeleteDownloads(ids: List<String>) {
        if (ids.isEmpty()) return
        ids.chunked(500).forEach { chunk ->
            database.downloadDao().deleteDownloadsByIds(chunk)
        }
    }

    suspend fun clearDownloads() {
        database.downloadDao().clearAll()
    }
}
