package com.example.data.api

import com.example.data.model.SearchResponse
import com.example.data.model.WallpaperDetailResponse
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface WallhavenApiService {

    @GET("api/v1/search")
    suspend fun searchWallpapers(
        @Query("q") query: String? = null,
        @Query("categories") categories: String? = null, // "111", "100", etc.
        @Query("purity") purity: String? = null,         // "100" (sfw), "110", etc.
        @Query("sorting") sorting: String? = null,       // "date_added", "toplist", "hot", "views", "favorites", "random"
        @Query("order") order: String? = "desc",
        @Query("topRange") topRange: String? = null,     // "1d", "3d", "1w", "1M", "3M", "6M", "1y"
        @Query("atleast") atleast: String? = null,
        @Query("resolutions") resolutions: String? = null,
        @Query("ratios") ratios: String? = null,         // "16x9", "9x16", etc.
        @Query("colors") colors: String? = null,         // hex without '#'
        @Query("page") page: Int = 1,
        @Query("seed") seed: String? = null,
        @Query("apikey") apiKey: String? = null
    ): SearchResponse

    @GET("api/v1/w/{id}")
    suspend fun getWallpaper(
        @Path("id") id: String,
        @Query("apikey") apiKey: String? = null
    ): WallpaperDetailResponse
}

object ApiClient {
    private const val BASE_URL = "https://wallhaven.cc/"

    private const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    val baseOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dispatcher(Dispatcher().apply {
                maxRequests = 32
                maxRequestsPerHost = 6
            })
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val original = chain.request()
                val request = if (original.header("User-Agent") == null) {
                    original.newBuilder()
                        .header("User-Agent", DEFAULT_USER_AGENT)
                        .build()
                } else {
                    original
                }
                chain.proceed(request)
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    val okHttpClient: OkHttpClient by lazy {
        baseOkHttpClient.newBuilder()
            .addInterceptor { chain ->
                val original = chain.request()
                val request = if (original.header("Accept") == null) {
                    original.newBuilder()
                        .header("Accept", "application/json")
                        .build()
                } else {
                    original
                }
                chain.proceed(request)
            }
            .build()
    }

    val apiService: WallhavenApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(WallhavenApiService::class.java)
    }
}
