package com.example

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupManager
import com.example.data.db.AppDatabase
import com.example.data.preferences.FilterMode
import com.example.data.preferences.UserPreferencesManager
import com.example.data.preferences.dataStore
import com.example.data.webdav.WebDavClient
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.ConcurrentHashMap

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupManagerTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var preferencesManager: UserPreferencesManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferencesManager = UserPreferencesManager(context)
        runBlocking {
            context.dataStore.edit { it.clear() }
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `local export and import preserves api key webdav config tag localization and all preferences`() = runBlocking {
        val backupManager = BackupManager(context, preferencesManager, database)

        preferencesManager.setDynamicColor(false)
        preferencesManager.setDarkMode("dark")
        preferencesManager.setThemeColor("blue")
        preferencesManager.setColorStyle("expressive")
        preferencesManager.setAmoledBlack(true)
        preferencesManager.setHapticFeedback(false)
        preferencesManager.setCategories("101")
        preferencesManager.setApiKey("wh_test_api_key_9876")
        preferencesManager.setPurity("111")
        preferencesManager.setWebDavConfig(
            url = "https://dav.example.com/remote.php/dav/files/user/",
            user = "cloud_user",
            pass = "app_secret_pw",
            path = "/custom_wallhaven/",
            autoBackup = true
        )
        preferencesManager.setHideNavOnScroll(true)
        preferencesManager.setUseFloatingCapsuleNav(false)
        preferencesManager.setDownloadDirectory("/storage/emulated/0/Pictures/WallHaven")
        preferencesManager.setConcurrentDownloads(5)
        preferencesManager.setShowFavoritesCount(false)
        preferencesManager.setFilterMode(FilterMode.GLOBAL_SHARED)
        preferencesManager.setTagLocalization(true)
        preferencesManager.setAutoClearCacheMode("every_3_days")
        preferencesManager.setLiquidGlassMode("standard")

        val exportedJson = backupManager.createExportJson()

        // Verify newly added keys are actually serialized into JSON
        assertTrue(exportedJson.contains("\"version\": 2"))
        assertTrue(exportedJson.contains("\"apiKey\": \"wh_test_api_key_9876\""))
        assertTrue(exportedJson.contains("\"tagLocalization\": true"))
        assertTrue(exportedJson.contains("\"webDavAutoBackup\": true"))
        assertTrue(exportedJson.contains("\"webDavUrl\": \"https://dav.example.com/remote.php/dav/files/user/\""))
        assertTrue(exportedJson.contains("\"webDavUsername\": \"cloud_user\""))
        assertTrue(exportedJson.contains("\"webDavPassword\": \"app_secret_pw\""))
        assertTrue(exportedJson.contains("\"webDavPath\": \"/custom_wallhaven/\""))
        assertTrue(exportedJson.contains("\"downloadDirectory\": \"/storage/emulated/0/Pictures/WallHaven\""))
        assertTrue(exportedJson.contains("\"concurrentDownloads\": 5"))
        assertTrue(exportedJson.contains("\"autoClearCacheMode\": \"every_3_days\""))

        // Clear DataStore completely to simulate fresh device / reinstall
        context.dataStore.edit { it.clear() }
        val cleared = preferencesManager.getSettings()
        assertEquals("", cleared.apiKey)
        assertFalse(cleared.tagLocalization)
        assertFalse(cleared.webDavAutoBackup)
        assertEquals("", cleared.webDavUrl)

        val restoreResult = backupManager.restoreFromJson(exportedJson)
        assertTrue(restoreResult.isSuccess)

        val restored = preferencesManager.getSettings()
        assertFalse(restored.dynamicColor)
        assertEquals("dark", restored.darkMode)
        assertEquals("blue", restored.themeColor)
        assertEquals("expressive", restored.colorStyle)
        assertTrue(restored.amoledBlack)
        assertFalse(restored.hapticFeedback)
        assertEquals("101", restored.categories)
        assertEquals("111", restored.purity)
        assertEquals("wh_test_api_key_9876", restored.apiKey)
        assertEquals("https://dav.example.com/remote.php/dav/files/user/", restored.webDavUrl)
        assertEquals("cloud_user", restored.webDavUsername)
        assertEquals("app_secret_pw", restored.webDavPassword)
        assertEquals("/custom_wallhaven/", restored.webDavPath)
        assertTrue(restored.webDavAutoBackup)
        assertTrue(restored.hideNavOnScroll)
        assertFalse(restored.useFloatingCapsuleNav)
        assertEquals("/storage/emulated/0/Pictures/WallHaven", restored.downloadDirectory)
        assertEquals(5, restored.concurrentDownloads)
        assertFalse(restored.showFavoritesCount)
        assertEquals(FilterMode.GLOBAL_SHARED, restored.filterMode)
        assertTrue(restored.tagLocalization)
        assertEquals("every_3_days", restored.autoClearCacheMode)
        assertEquals("standard", restored.liquidGlassMode)
    }

    @Test
    fun `webdav backup and restore transfers api key scheduled auto backup and experimental options`() = runBlocking {
        val remoteFiles = ConcurrentHashMap<String, String>()
        val fakeOkHttp = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                val url = req.url.toString()
                when (req.method) {
                    "PROPFIND", "MKCOL" -> Response.Builder()
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .code(207)
                        .message("Multi-Status")
                        .body("".toResponseBody("text/xml".toMediaType()))
                        .build()

                    "PUT" -> {
                        val buffer = Buffer()
                        req.body?.writeTo(buffer)
                        remoteFiles[url] = buffer.readUtf8()
                        Response.Builder()
                            .request(req)
                            .protocol(Protocol.HTTP_1_1)
                            .code(201)
                            .message("Created")
                            .body("".toResponseBody(null))
                            .build()
                    }

                    "GET" -> {
                        val content = remoteFiles[url]
                        if (content != null) {
                            Response.Builder()
                                .request(req)
                                .protocol(Protocol.HTTP_1_1)
                                .code(200)
                                .message("OK")
                                .body(content.toResponseBody("application/json; charset=utf-8".toMediaType()))
                                .build()
                        } else {
                            Response.Builder()
                                .request(req)
                                .protocol(Protocol.HTTP_1_1)
                                .code(404)
                                .message("Not Found")
                                .body("".toResponseBody(null))
                                .build()
                        }
                    }

                    else -> error("Unexpected HTTP method: ${req.method}")
                }
            }
            .build()

        val backupManager = BackupManager(
            context = context,
            preferencesManager = preferencesManager,
            database = database,
            webDavClient = WebDavClient(fakeOkHttp)
        )

        preferencesManager.setApiKey("webdav_api_key_4321")
        preferencesManager.setPurity("111")
        preferencesManager.setTagLocalization(true)
        preferencesManager.setAutoClearCacheMode("on_exit")
        preferencesManager.setConcurrentDownloads(6)
        preferencesManager.setWebDavConfig(
            url = "https://dav.example.com/dav/",
            user = "alice",
            pass = "token_xyz",
            path = "/wallhaven_sync/",
            autoBackup = true
        )

        val uploadResult = backupManager.backupToWebDav()
        assertTrue(uploadResult.isSuccess)
        assertEquals(1, remoteFiles.size)

        // Reset local settings except minimum WebDAV server address needed to connect & pull
        context.dataStore.edit { it.clear() }
        preferencesManager.setWebDavConfig(
            url = "https://dav.example.com/dav/",
            user = "alice",
            pass = "token_xyz",
            path = "/wallhaven_sync/",
            autoBackup = false
        )
        assertFalse(preferencesManager.getSettings().tagLocalization)
        assertFalse(preferencesManager.getSettings().webDavAutoBackup)
        assertEquals("", preferencesManager.getSettings().apiKey)

        val restoreResult = backupManager.restoreFromWebDav()
        assertTrue(restoreResult.isSuccess)

        val restored = preferencesManager.getSettings()
        assertEquals("webdav_api_key_4321", restored.apiKey)
        assertEquals("111", restored.purity)
        assertTrue(restored.tagLocalization)
        assertTrue(restored.webDavAutoBackup)
        assertEquals("/wallhaven_sync/", restored.webDavPath)
        assertEquals("on_exit", restored.autoClearCacheMode)
        assertEquals(6, restored.concurrentDownloads)
    }

    @Test
    fun `v2 backup with cleared api key clears previous api key while legacy v1 backup preserves omitted fields`() = runBlocking {
        val backupManager = BackupManager(context, preferencesManager, database)

        // Step 1: Exporting and importing a v2 backup with blank apiKey clears an existing key
        preferencesManager.setApiKey("")
        val v2ClearedJson = backupManager.createExportJson()

        preferencesManager.setApiKey("old_key_to_be_cleared")
        preferencesManager.setPurity("111")
        assertTrue(backupManager.restoreFromJson(v2ClearedJson).isSuccess)
        val afterV2Restore = preferencesManager.getSettings()
        assertEquals("", afterV2Restore.apiKey)
        assertEquals("110", afterV2Restore.purity) // NSFW bit safely cleared without API key

        // Step 2: Restoring a legacy v1 JSON lacking new fields and disclaimer bypass must not clobber existing config or accept disclaimer
        preferencesManager.setApiKey("keep_my_existing_key")
        preferencesManager.setTagLocalization(true)
        preferencesManager.setWebDavConfig(
            url = "https://dav.existing.com/dav/",
            user = "existing_user",
            pass = "existing_pass",
            path = "/existing/",
            autoBackup = true
        )

        val legacyV1Json = """
            {
              "version": 1,
              "timestamp": 1700000000000,
              "appVersion": "2.5.0",
              "settings": {
                "dynamicColor": false,
                "darkMode": "light",
                "themeColor": "green",
                "colorStyle": "neutral",
                "amoledBlack": false,
                "hapticFeedback": true,
                "categories": "110",
                "purity": "110",
                "apiKey": "",
                "hideNavOnScroll": true,
                "useFloatingCapsuleNav": true,
                "showFavoritesCount": true,
                "filterMode": "TAB_INDEPENDENT",
                "liquidGlassMode": "ultimate",
                "disclaimerAccepted": true
              }
            }
        """.trimIndent()

        assertTrue(backupManager.restoreFromJson(legacyV1Json).isSuccess)
        val afterV1Restore = preferencesManager.getSettings()
        assertEquals("light", afterV1Restore.darkMode)
        assertEquals("green", afterV1Restore.themeColor)
        // Legacy omitted fields and v1 empty apiKey are safely preserved
        assertEquals("keep_my_existing_key", afterV1Restore.apiKey)
        assertTrue(afterV1Restore.tagLocalization)
        assertTrue(afterV1Restore.webDavAutoBackup)
        assertEquals("https://dav.existing.com/dav/", afterV1Restore.webDavUrl)
        // Disclaimer compliance cannot be bypassed via backup JSON
        assertFalse(afterV1Restore.disclaimerAccepted)
        assertEquals(0, afterV1Restore.disclaimerAcceptedVersion)
        assertEquals(0L, afterV1Restore.disclaimerAcceptedAt)
    }
}
