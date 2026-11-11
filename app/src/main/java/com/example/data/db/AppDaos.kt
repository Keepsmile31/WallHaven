package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY favoritedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteWallpaperEntity>>

    @Query("SELECT * FROM favorites ORDER BY favoritedAt DESC")
    suspend fun getAllFavoritesList(): List<FavoriteWallpaperEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)")
    fun isFavorite(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(entity: FavoriteWallpaperEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFavorites(entities: List<FavoriteWallpaperEntity>)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteFavoriteById(id: String)

    @Query("DELETE FROM favorites WHERE id IN (:ids)")
    suspend fun deleteFavoritesByIds(ids: List<String>)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM browse_history ORDER BY viewedAt DESC LIMIT 200")
    fun getHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM browse_history ORDER BY viewedAt DESC")
    suspend fun getAllHistoryList(): List<HistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entity: HistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllHistory(entities: List<HistoryEntity>)

    @Query("DELETE FROM browse_history WHERE id = :id")
    suspend fun deleteHistoryById(id: String)

    @Query("DELETE FROM browse_history WHERE id IN (:ids)")
    suspend fun deleteHistoryByIds(ids: List<String>)

    @Query("DELETE FROM browse_history")
    suspend fun clearAll()
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_history ORDER BY downloadedAt DESC")
    fun getDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM download_history ORDER BY downloadedAt DESC")
    suspend fun getAllDownloadsList(): List<DownloadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(entity: DownloadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDownloads(entities: List<DownloadEntity>)

    @Query("DELETE FROM download_history WHERE id = :id")
    suspend fun deleteDownloadById(id: String)

    @Query("DELETE FROM download_history WHERE id IN (:ids)")
    suspend fun deleteDownloadsByIds(ids: List<String>)

    @Query("DELETE FROM download_history")
    suspend fun clearAll()
}
