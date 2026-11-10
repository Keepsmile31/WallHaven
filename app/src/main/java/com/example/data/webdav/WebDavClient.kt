package com.example.data.webdav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class WebDavClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun normalizeUrl(baseUrl: String, path: String): String {
        val cleanBase = baseUrl.trim().trimEnd('/')
        val cleanPath = path.trim().trim('/')
        return if (cleanPath.isEmpty()) cleanBase else "$cleanBase/$cleanPath"
    }

    suspend fun testConnection(serverUrl: String, user: String, pass: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                if (serverUrl.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("服务器地址不能为空"))
                }
                val credential = Credentials.basic(user.trim(), pass, Charsets.UTF_8)
                val cleanUrl = if (serverUrl.trim().endsWith("/")) serverUrl.trim() else "${serverUrl.trim()}/"

                // Try PROPFIND first
                try {
                    val request = Request.Builder()
                        .url(cleanUrl)
                        .method("PROPFIND", null)
                        .header("Authorization", credential)
                        .header("Depth", "0")
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful || response.code == 207 || response.code == 200) {
                            return@withContext Result.success(true)
                        } else if (response.code == 401) {
                            return@withContext Result.failure(IOException("认证失败：用户名或密码错误 (HTTP 401)"))
                        }
                    }
                } catch (_: Exception) {}

                // Fallback attempt: GET on server URL
                val getReq = Request.Builder()
                    .url(cleanUrl)
                    .header("Authorization", credential)
                    .build()

                client.newCall(getReq).execute().use { getResp ->
                    if (getResp.isSuccessful || getResp.code in 200..399) {
                        Result.success(true)
                    } else if (getResp.code == 401) {
                        Result.failure(IOException("认证失败：用户名或密码错误 (HTTP 401)"))
                    } else {
                        Result.failure(IOException("服务器响应异常 (HTTP ${getResp.code}): ${getResp.message}"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun ensureDirectoryExists(
        serverUrl: String,
        user: String,
        pass: String,
        folderPath: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanFolder = folderPath.trim().trim('/')
            if (cleanFolder.isEmpty()) {
                return@withContext Result.success(true)
            }
            val folderUrl = normalizeUrl(serverUrl, cleanFolder) + "/"
            val credential = Credentials.basic(user.trim(), pass, Charsets.UTF_8)

            // Try PROPFIND on directory
            try {
                val checkReq = Request.Builder()
                    .url(folderUrl)
                    .method("PROPFIND", null)
                    .header("Authorization", credential)
                    .header("Depth", "0")
                    .build()

                client.newCall(checkReq).execute().use { resp ->
                    if (resp.isSuccessful || resp.code == 207 || resp.code == 200) {
                        return@withContext Result.success(true)
                    }
                }
            } catch (_: Exception) {}

            // Create collection using MKCOL
            val mkcolReq = Request.Builder()
                .url(folderUrl)
                .method("MKCOL", null)
                .header("Authorization", credential)
                .build()

            client.newCall(mkcolReq).execute().use { resp ->
                if (resp.isSuccessful || resp.code == 201 || resp.code == 200 || resp.code == 204 || resp.code == 405) {
                    Result.success(true)
                } else {
                    Result.failure(IOException("创建远程目录失败 (${resp.code}): ${resp.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadFile(
        serverUrl: String,
        user: String,
        pass: String,
        folderPath: String,
        fileName: String,
        content: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (serverUrl.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("服务器地址不能为空"))
            }
            ensureDirectoryExists(serverUrl, user, pass, folderPath)

            val cleanFolder = folderPath.trim().trim('/')
            val folderUrl = if (cleanFolder.isEmpty()) serverUrl.trim().trimEnd('/') else normalizeUrl(serverUrl, cleanFolder)
            val fullUrl = "$folderUrl/$fileName"
            val credential = Credentials.basic(user.trim(), pass, Charsets.UTF_8)
            val body: RequestBody = content.toRequestBody("application/json; charset=utf-8".toMediaType())

            val putReq = Request.Builder()
                .url(fullUrl)
                .put(body)
                .header("Authorization", credential)
                .build()

            client.newCall(putReq).execute().use { response ->
                if (response.isSuccessful || response.code in 200..299) {
                    Result.success(true)
                } else {
                    Result.failure(IOException("WebDAV 上传失败 (HTTP ${response.code}): ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadFile(
        serverUrl: String,
        user: String,
        pass: String,
        folderPath: String,
        fileName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (serverUrl.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("服务器地址不能为空"))
            }
            val cleanFolder = folderPath.trim().trim('/')
            val folderUrl = if (cleanFolder.isEmpty()) serverUrl.trim().trimEnd('/') else normalizeUrl(serverUrl, cleanFolder)
            val fullUrl = "$folderUrl/$fileName"
            val credential = Credentials.basic(user.trim(), pass, Charsets.UTF_8)

            val getReq = Request.Builder()
                .url(fullUrl)
                .get()
                .header("Authorization", credential)
                .build()

            client.newCall(getReq).execute().use { response ->
                if (response.isSuccessful || response.code in 200..299) {
                    val bodyString = response.body?.string() ?: ""
                    if (bodyString.isBlank()) {
                        Result.failure(IOException("云端备份文件内容为空"))
                    } else {
                        Result.success(bodyString)
                    }
                } else if (response.code == 404) {
                    Result.failure(IOException("未在云端找到备份文件 ($fileName)，请先执行上传备份"))
                } else {
                    Result.failure(IOException("下载云端备份失败 (HTTP ${response.code}): ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
