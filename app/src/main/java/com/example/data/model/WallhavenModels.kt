package com.example.data.model

import androidx.compose.runtime.Immutable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Immutable
@JsonClass(generateAdapter = true)
data class SearchResponse(
    @Json(name = "data") val data: List<WallpaperDto> = emptyList(),
    @Json(name = "meta") val meta: MetaDto? = null
)

@JsonClass(generateAdapter = true)
data class WallpaperDetailResponse(
    @Json(name = "data") val data: WallpaperDto
)

@Immutable
@JsonClass(generateAdapter = true)
data class WallpaperDto(
    @Json(name = "id") val id: String,
    @Json(name = "url") val url: String = "",
    @Json(name = "views") val views: Int = 0,
    @Json(name = "favorites") val favorites: Int = 0,
    @Json(name = "purity") val purity: String = "sfw",
    @Json(name = "category") val category: String = "general",
    @Json(name = "dimension_x") val dimensionX: Int = 1920,
    @Json(name = "dimension_y") val dimensionY: Int = 1080,
    @Json(name = "resolution") val resolution: String = "1920x1080",
    @Json(name = "ratio") val ratio: String = "1.78",
    @Json(name = "file_size") val fileSize: Long = 0L,
    @Json(name = "file_type") val fileType: String = "image/jpeg",
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "colors") val colors: List<String> = emptyList(),
    @Json(name = "path") val path: String = "",
    @Json(name = "thumbs") val thumbs: ThumbsDto = ThumbsDto(),
    @Json(name = "tags") val tags: List<TagDto> = emptyList()
)

@Immutable
@JsonClass(generateAdapter = true)
data class ThumbsDto(
    @Json(name = "large") val large: String? = null,
    @Json(name = "original") val original: String? = null,
    @Json(name = "small") val small: String? = null
) {
    val bestThumb: String
        get() = large?.takeIf { it.isNotBlank() }
            ?: small?.takeIf { it.isNotBlank() }
            ?: original?.takeIf { it.isNotBlank() }
            ?: ""

    fun getThumbForRatio(isNonLandscape: Boolean): String {
        return if (isNonLandscape) {
            original?.takeIf { it.isNotBlank() }
                ?: large?.takeIf { it.isNotBlank() }
                ?: small?.takeIf { it.isNotBlank() }
                ?: ""
        } else {
            large?.takeIf { it.isNotBlank() }
                ?: small?.takeIf { it.isNotBlank() }
                ?: original?.takeIf { it.isNotBlank() }
                ?: ""
        }
    }
}

@Immutable
@JsonClass(generateAdapter = true)
data class TagDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "alias") val alias: String? = null,
    @Json(name = "category_id") val categoryId: Int? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "purity") val purity: String? = null
)

@Immutable
@JsonClass(generateAdapter = true)
data class MetaDto(
    @Json(name = "current_page") val currentPage: Int = 1,
    @Json(name = "last_page") val lastPage: Int = 1
)
