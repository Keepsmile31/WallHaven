package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ThumbsDto
import com.example.data.model.WallpaperDto
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "favorites")
data class FavoriteWallpaperEntity(
    @PrimaryKey val id: String,
    val url: String,
    val resolution: String,
    val dimensionX: Int,
    val dimensionY: Int,
    val ratio: String,
    val fileSize: Long,
    val fileType: String,
    val category: String,
    val purity: String,
    val path: String,
    val thumbUrl: String,
    val views: Int,
    val favorites: Int,
    val colorsString: String, // comma separated hexes
    val favoritedAt: Long = System.currentTimeMillis()
) {
    fun toWallpaperDto(): WallpaperDto {
        val colorList = if (colorsString.isBlank()) emptyList() else colorsString.split(",")
        return WallpaperDto(
            id = id,
            url = url,
            dimensionX = dimensionX,
            dimensionY = dimensionY,
            resolution = resolution,
            ratio = ratio,
            fileSize = fileSize,
            fileType = fileType,
            category = category,
            purity = purity,
            path = path,
            thumbs = ThumbsDto(large = thumbUrl, original = thumbUrl, small = thumbUrl),
            views = views,
            favorites = favorites,
            colors = colorList
        )
    }

    companion object {
        fun fromDto(dto: WallpaperDto): FavoriteWallpaperEntity {
            val parsedRatio = dto.ratio.toFloatOrNull()
                ?: (dto.dimensionX.toFloat() / dto.dimensionY.coerceAtLeast(1).toFloat())
            val useOriginalThumb = parsedRatio < 1.26f || parsedRatio > 1.9f
            return FavoriteWallpaperEntity(
                id = dto.id,
                url = dto.url,
                resolution = dto.resolution,
                dimensionX = dto.dimensionX,
                dimensionY = dto.dimensionY,
                ratio = dto.ratio,
                fileSize = dto.fileSize,
                fileType = dto.fileType,
                category = dto.category,
                purity = dto.purity,
                path = dto.path,
                thumbUrl = dto.thumbs.getThumbForRatio(useOriginalThumb).ifBlank { dto.thumbs.bestThumb },
                views = dto.views,
                favorites = dto.favorites,
                colorsString = dto.colors.joinToString(","),
                favoritedAt = System.currentTimeMillis()
            )
        }
    }
}

@JsonClass(generateAdapter = true)
@Entity(tableName = "browse_history")
data class HistoryEntity(
    @PrimaryKey val id: String, // wallhaven id
    val url: String,
    val thumbUrl: String,
    val fullPath: String,
    val resolution: String,
    val category: String,
    val ratio: String,
    val viewedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "download_history")
data class DownloadEntity(
    @PrimaryKey val id: String, // wallhaven id
    val resolution: String,
    val thumbUrl: String,
    val fullPath: String,
    val localUri: String,
    val fileSize: Long,
    val downloadedAt: Long = System.currentTimeMillis()
)
