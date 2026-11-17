package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

@JsonClass(generateAdapter = true)
data class GitHubReleaseDto(
    @param:Json(name = "tag_name") val tagName: String = "",
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "body") val body: String? = null,
    @param:Json(name = "html_url") val htmlUrl: String = "",
    @param:Json(name = "published_at") val publishedAt: String? = null,
    @param:Json(name = "prerelease") val prerelease: Boolean = false,
    @param:Json(name = "draft") val draft: Boolean = false,
    @param:Json(name = "assets") val assets: List<GitHubReleaseAssetDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitHubReleaseAssetDto(
    @param:Json(name = "name") val name: String = "",
    @param:Json(name = "browser_download_url") val browserDownloadUrl: String = "",
    @param:Json(name = "size") val size: Long = 0L,
    @param:Json(name = "content_type") val contentType: String? = null
)

interface GitHubApiService {
    @GET("repos/Keepsmile31/WallHaven/releases/latest")
    suspend fun getLatestRelease(): GitHubReleaseDto
}

object GitHubApiClient {
    private const val GITHUB_API_BASE_URL = "https://api.github.com/"

    private val okHttpClient by lazy {
        ApiClient.baseOkHttpClient.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    val service: GitHubApiService by lazy {
        Retrofit.Builder()
            .baseUrl(GITHUB_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GitHubApiService::class.java)
    }
}
