package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.data.backup.BackupManager
import com.example.data.preferences.AppSettings
import com.example.data.preferences.UserPreferencesManager
import com.example.data.webdav.WebDavClient
import com.example.ui.components.LiquidGlassSwitch
import com.example.util.BugLogManager
import com.example.util.HapticHelper
import kotlinx.coroutines.launch

@Composable
internal fun SettingsBackupScreen(
    settings: AppSettings,
    preferencesManager: UserPreferencesManager,
    backupManager: BackupManager,
    hapticHelper: HapticHelper,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isBackingUpToWebDav by remember { mutableStateOf(false) }
    var isRestoringFromWebDav by remember { mutableStateOf(false) }
    var showWebDavDialog by remember { mutableStateOf(false) }
    var showWebDavRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showBackupStrategyDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = backupManager.createExportJson()
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(json.toByteArray(Charsets.UTF_8))
                    }
                    hapticHelper.vibrateSuccess(settings.hapticFeedback)
                    Toast.makeText(context, "数据已成功导出至文件！", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().use { it.readText() }
                    }
                    if (json.isNullOrBlank()) {
                        Toast.makeText(context, "文件读取为空，导入取消", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    val res = backupManager.restoreFromJson(json)
                    if (res.isSuccess) {
                        val summary = res.getOrNull()
                        hapticHelper.vibrateSuccess(settings.hapticFeedback)
                        val msg = if (summary != null) {
                            "恢复成功！已还原 ${summary.favoritesCount} 个收藏，${summary.historyCount} 条历史记录"
                        } else {
                            "本地备份数据已成功导入！"
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "解析失败: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "导入异常: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    SubPageScaffold(
        title = "备份与恢复",
        onBack = onBack
    ) {
        M3SettingsSectionTitle(title = "WebDAV 云端同步", icon = ImageVector.vectorResource(R.drawable.ic_cloud_sync))
        M3SettingsCard {
            M3SettingsItemRow(
                icon = ImageVector.vectorResource(R.drawable.ic_cloud_sync),
                title = "WebDAV 云端服务器",
                subtitle = if (settings.webDavUrl.isNotBlank()) settings.webDavUrl else "未配置 (支持坚果云、Nextcloud、群晖等服务)",
                testTag = "settings_webdav_row",
                onClick = { showWebDavDialog = true },
                trailingContent = {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = if (settings.webDavUrl.isNotBlank()) "编辑" else "配置",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            )

            M3SettingsDivider()

            M3SettingsItemRow(
                icon = ImageVector.vectorResource(R.drawable.ic_folder),
                title = "云端备份路径",
                subtitle = "${settings.webDavPath.ifBlank { "/wallhaven_backup/" }}wallhaven_backup.json",
                onClick = { showWebDavDialog = true }
            )

            M3SettingsDivider()

            M3SettingsItemRow(
                icon = ImageVector.vectorResource(R.drawable.ic_cloud_upload),
                title = "立即备份至云端",
                subtitle = if (isBackingUpToWebDav) "正在上传备份至 WebDAV 云端..." else "将收藏、历史记录与偏好设置即时上传至云端",
                enabled = !isBackingUpToWebDav,
                onClick = {
                    if (settings.webDavUrl.isBlank()) {
                        Toast.makeText(context, "请先配置 WebDAV 服务器地址", Toast.LENGTH_SHORT).show()
                        showWebDavDialog = true
                    } else {
                        isBackingUpToWebDav = true
                        scope.launch {
                            val res = backupManager.backupToWebDav()
                            isBackingUpToWebDav = false
                            if (res.isSuccess) {
                                hapticHelper.vibrateSuccess(settings.hapticFeedback)
                                Toast.makeText(context, res.getOrNull() ?: "云端备份成功！", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "备份失败: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                trailingContent = {
                    if (isBackingUpToWebDav) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            )

            M3SettingsDivider()

            M3SettingsItemRow(
                icon = ImageVector.vectorResource(R.drawable.ic_cloud_download),
                title = "从云端恢复数据",
                subtitle = if (isRestoringFromWebDav) "正在从云端下载并恢复记录..." else "从云端同步下载备份并合并本地记录",
                enabled = !isRestoringFromWebDav,
                onClick = {
                    if (settings.webDavUrl.isBlank()) {
                        Toast.makeText(context, "请先配置 WebDAV 服务器地址", Toast.LENGTH_SHORT).show()
                        showWebDavDialog = true
                    } else {
                        showWebDavRestoreConfirmDialog = true
                    }
                },
                trailingContent = {
                    if (isRestoringFromWebDav) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            )

            M3SettingsDivider()

            val toggleAutoBackup = {
                val next = !settings.webDavAutoBackup
                if (next && settings.webDavUrl.isBlank()) {
                    Toast.makeText(context, "请先配置 WebDAV 服务器地址", Toast.LENGTH_SHORT).show()
                    showWebDavDialog = true
                } else {
                    hapticHelper.vibrateTick(settings.hapticFeedback)
                    scope.launch { preferencesManager.setWebDavAutoBackup(next) }
                }
            }
            M3SettingsItemRow(
                icon = ImageVector.vectorResource(R.drawable.ic_sync),
                title = "WebDAV 每日自动备份",
                subtitle = if (settings.webDavUrl.isNotBlank()) "每日冷启动时自动在后台上传备份至云端" else "需要先配置 WebDAV 服务器",
                onClick = { toggleAutoBackup() },
                trailingContent = {
                    LiquidGlassSwitch(
                        checked = settings.webDavAutoBackup,
                        onCheckedChange = { checked ->
                            if (checked && settings.webDavUrl.isBlank()) {
                                Toast.makeText(context, "请先配置 WebDAV 服务器地址", Toast.LENGTH_SHORT).show()
                                showWebDavDialog = true
                            } else {
                                hapticHelper.vibrateTick(settings.hapticFeedback)
                                scope.launch { preferencesManager.setWebDavAutoBackup(checked) }
                            }
                        },
                        modifier = Modifier.testTag("webdav_auto_backup_switch")
                    )
                }
            )

            M3SettingsDivider()

            M3SettingsItemRow(
                icon = ImageVector.vectorResource(R.drawable.ic_rule),
                title = "恢复合并策略",
                subtitle = "恢复时智能合并本地与云端数据，保留较新记录",
                onClick = { showBackupStrategyDialog = true }
            )
        }

        M3SettingsSectionTitle(title = "本地文件备份与导出", icon = ImageVector.vectorResource(R.drawable.ic_storage))
        M3SettingsCard {
            M3SettingsItemRow(
                icon = ImageVector.vectorResource(R.drawable.ic_file_upload),
                title = stringResource(R.string.export_data),
                subtitle = stringResource(R.string.export_data_desc),
                onClick = { exportLauncher.launch("wallhaven_backup_${System.currentTimeMillis()}.json") }
            )

            M3SettingsDivider()

            M3SettingsItemRow(
                icon = ImageVector.vectorResource(R.drawable.ic_file_download),
                title = stringResource(R.string.import_data),
                subtitle = stringResource(R.string.import_data_desc),
                onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) }
            )
        }
    }

    if (showWebDavDialog) {
        WebDavConfigDialog(
            settings = settings,
            preferencesManager = preferencesManager,
            hapticHelper = hapticHelper,
            onDismiss = { showWebDavDialog = false }
        )
    }

    if (showBackupStrategyDialog) {
        BackupStrategyDialog(onDismiss = { showBackupStrategyDialog = false })
    }

    if (showWebDavRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showWebDavRestoreConfirmDialog = false },
            title = { Text("从 WebDAV 恢复备份") },
            text = { Text("将从 WebDAV 云端下载并恢复备份数据（包含收藏、历史、下载记录与偏好设置）。本地已有记录将予以保留合并。是否确认执行？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWebDavRestoreConfirmDialog = false
                        isRestoringFromWebDav = true
                        scope.launch {
                            val res = backupManager.restoreFromWebDav()
                            isRestoringFromWebDav = false
                            if (res.isSuccess) {
                                val summary = res.getOrNull()
                                hapticHelper.vibrateSuccess(settings.hapticFeedback)
                                val msg = if (summary != null) {
                                    "恢复成功！已还原 ${summary.favoritesCount} 个收藏，${summary.historyCount} 条历史记录"
                                } else {
                                    "恢复成功！已还原设置和记录"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "恢复失败: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("确认恢复")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWebDavRestoreConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun WebDavConfigDialog(
    settings: AppSettings,
    preferencesManager: UserPreferencesManager,
    hapticHelper: HapticHelper,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var urlInput by remember(settings.webDavUrl) { mutableStateOf(settings.webDavUrl) }
    var userInput by remember(settings.webDavUsername) { mutableStateOf(settings.webDavUsername) }
    var passInput by remember(settings.webDavPassword) { mutableStateOf(settings.webDavPassword) }
    var pathInput by remember(settings.webDavPath) { mutableStateOf(settings.webDavPath) }
    var autoBackupInput by remember(settings.webDavAutoBackup) { mutableStateOf(settings.webDavAutoBackup) }
    var isTesting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("WebDAV 服务器设置") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("服务器地址 (URL)") },
                    placeholder = { Text("https://dav.jianguoyun.com/dav/") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("webdav_url_input")
                )
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    label = { Text("用户名 / 账号") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("webdav_user_input")
                )
                OutlinedTextField(
                    value = passInput,
                    onValueChange = { passInput = it },
                    label = { Text("密码 / 应用授权码") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("webdav_pass_input")
                )
                OutlinedTextField(
                    value = pathInput,
                    onValueChange = { pathInput = it },
                    label = { Text("远程备份路径") },
                    placeholder = { Text("/wallhaven_backup/") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("开启每日自动备份", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("每日首次冷启动应用时自动同步", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LiquidGlassSwitch(
                        checked = autoBackupInput,
                        onCheckedChange = {
                            hapticHelper.vibrateTick(settings.hapticFeedback)
                            autoBackupInput = it
                        },
                        backdropColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                }
                OutlinedButton(
                    onClick = {
                        isTesting = true
                        scope.launch {
                            val client = WebDavClient()
                            val res = client.testConnection(urlInput, userInput, passInput)
                            isTesting = false
                            if (res.isSuccess) {
                                hapticHelper.vibrateSuccess(settings.hapticFeedback)
                                Toast.makeText(context, "WebDAV 连接测试成功！", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "测试失败: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isTesting && urlInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("测试中...", fontSize = 13.sp)
                    } else {
                        Text("测试连接", fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        preferencesManager.setWebDavConfig(
                            url = urlInput.trim(),
                            user = userInput.trim(),
                            pass = passInput,
                            path = pathInput.trim(),
                            autoBackup = autoBackupInput
                        )
                    }
                    hapticHelper.vibrateSuccess(settings.hapticFeedback)
                    Toast.makeText(context, "WebDAV 配置已保存", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun BackupStrategyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("恢复与同步合并策略") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("智能增量合并", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Text("从云端或本地恢复时，本地已有的收藏与历史将完整保留，并自动将备份中的记录增量合并入库。", fontSize = 13.sp, lineHeight = 18.sp)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                Text("时间戳对齐", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Text("对于重复的收藏项，系统会自动对比记录时间戳并保留最新标记。", fontSize = 13.sp, lineHeight = 18.sp)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                Text("偏好设置同步", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Text("外观与过滤偏好自动对齐云端配置，确保多端体验一致。", fontSize = 13.sp, lineHeight = 18.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了")
            }
        }
    )
}

@Composable
internal fun SettingsAboutScreen(
    settings: AppSettings,
    hapticHelper: HapticHelper,
    onClearHistory: (() -> Unit)?,
    onClearDownloads: (() -> Unit)?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val debugLogCount by BugLogManager.logCount.collectAsStateWithLifecycle()
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showUserAgreementDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearDownloadsDialog by remember { mutableStateOf(false) }

    SubPageScaffold(
        title = stringResource(R.string.settings_about),
        onBack = onBack
    ) {
        M3SettingsCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                val appIconBg = if (isDarkTheme) Color(0xFF2E2F34) else Color.White
                val appIconFg = if (isDarkTheme) Color.White else Color(0xFF2E2F34)

                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(appIconBg)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_brand_logo),
                        contentDescription = stringResource(R.string.app_name),
                        colorFilter = ColorFilter.tint(appIconFg),
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "WallHaven",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Compile SDK 37 · arm64-v8a",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { showUpdateDialog = true },
                    modifier = Modifier.fillMaxWidth().testTag("check_update_button")
                ) {
                    Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_cloud_download), contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("检查更新 (123云盘发布源)")
                }
            }
        }

        M3SettingsSectionTitle(title = "条款与支持", icon = ImageVector.vectorResource(R.drawable.ic_description))
        M3SettingsCard {
            M3SettingsItemRow(
                icon = Icons.Default.Info,
                title = stringResource(R.string.about_wallhaven_entry),
                subtitle = "应用介绍与开源说明",
                onClick = { showAboutDialog = true }
            )

            M3SettingsDivider()

            M3SettingsItemRow(
                icon = ImageVector.vectorResource(R.drawable.ic_description),
                title = stringResource(R.string.user_agreement_title),
                subtitle = "服务条款与使用准则",
                onClick = { showUserAgreementDialog = true }
            )

            M3SettingsDivider()

            M3SettingsItemRow(
                icon = ImageVector.vectorResource(R.drawable.ic_policy),
                title = stringResource(R.string.privacy_policy_title),
                subtitle = "隐私保护、权限与数据安全政策",
                onClick = { showPrivacyPolicyDialog = true }
            )

            M3SettingsDivider()

            M3SettingsItemRow(
                icon = ImageVector.vectorResource(R.drawable.ic_open_in_new),
                title = "WallHaven 官方主站",
                subtitle = "https://wallhaven.cc",
                onClick = {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wallhaven.cc"))
                    try {
                        context.startActivity(browserIntent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            M3SettingsDivider()

            M3SettingsItemRow(
                icon = ImageVector.vectorResource(R.drawable.ic_open_in_new),
                title = "开发者主页 (酷安)",
                subtitle = "前往酷安查看最新动态与交流讨论",
                onClick = {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.coolapk.com/u/3207664"))
                    try {
                        context.startActivity(browserIntent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            M3SettingsDivider()

            M3SettingsItemRow(
                icon = ImageVector.vectorResource(R.drawable.ic_open_in_new),
                title = stringResource(R.string.about_project_url),
                subtitle = "https://github.com/Keepsmile31/WallHaven",
                testTag = "about_project_url_row",
                onClick = {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Keepsmile31/WallHaven"))
                    try {
                        context.startActivity(browserIntent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        M3SettingsSectionTitle(
            title = stringResource(R.string.debug_logs_section_title),
            icon = ImageVector.vectorResource(R.drawable.ic_bug_report)
        )
        M3SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.export_debug_logs_desc, debugLogCount),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            hapticHelper.vibrateClick(settings.hapticFeedback)
                            val shareRes = BugLogManager.shareDebugLogs(context)
                            if (shareRes.isFailure) {
                                val errMsg = shareRes.exceptionOrNull()?.message ?: "未知错误"
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.export_debug_logs_failed, errMsg),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_debug_logs_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.export_debug_logs), fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            BugLogManager.clearLogs()
                            hapticHelper.vibrateSuccess(settings.hapticFeedback)
                            Toast.makeText(
                                context,
                                context.getString(R.string.debug_logs_cleared),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        enabled = debugLogCount > 0,
                        modifier = Modifier.testTag("clear_debug_logs_btn")
                    ) {
                        Text(stringResource(R.string.clear_debug_logs), fontSize = 13.sp)
                    }
                }
            }
        }

        M3SettingsSectionTitle(title = "记录重置", icon = ImageVector.vectorResource(R.drawable.ic_delete_outline))
        M3SettingsCard {
            M3SettingsItemRow(
                icon = ImageVector.vectorResource(R.drawable.ic_delete_outline),
                title = "清空浏览历史",
                subtitle = "清除本地所有已浏览壁纸记录",
                onClick = { showClearHistoryDialog = true }
            )

            M3SettingsDivider()

            M3SettingsItemRow(
                icon = ImageVector.vectorResource(R.drawable.ic_delete_outline),
                title = "清空下载记录",
                subtitle = "清除本地下载管理列表（不会删除已下载的本地壁纸文件）",
                onClick = { showClearDownloadsDialog = true }
            )
        }
    }

    if (showUpdateDialog) {
        UpdateCheckDialog(onDismiss = { showUpdateDialog = false })
    }

    if (showAboutDialog) {
        AboutWallHavenDialog(onDismiss = { showAboutDialog = false })
    }

    if (showUserAgreementDialog) {
        UserAgreementDialog(onDismiss = { showUserAgreementDialog = false })
    }

    if (showPrivacyPolicyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyPolicyDialog = false })
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("清空浏览历史") },
            text = { Text("确定要清除所有的本地壁纸浏览历史吗？该操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory?.invoke()
                        hapticHelper.vibrateSuccess(settings.hapticFeedback)
                        Toast.makeText(context, "浏览历史已清空", Toast.LENGTH_SHORT).show()
                        showClearHistoryDialog = false
                    }
                ) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showClearDownloadsDialog) {
        AlertDialog(
            onDismissRequest = { showClearDownloadsDialog = false },
            title = { Text("清空下载记录") },
            text = { Text("确定要清除所有的本地壁纸下载任务记录吗？（已保存至相册的文件不受影响）") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearDownloads?.invoke()
                        hapticHelper.vibrateSuccess(settings.hapticFeedback)
                        Toast.makeText(context, "下载记录已清空", Toast.LENGTH_SHORT).show()
                        showClearDownloadsDialog = false
                    }
                ) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDownloadsDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun UpdateCheckDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.check_updates)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "当前安装版本: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "123 云盘已收录最新发布版本以及历史版本归档，支持在此查看更新详情、直接下载最新版本或随时回退至历史版本。",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "架构: arm64-v8a · Compile SDK 37",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://1815318986.share.123pan.cn/123pan/ngLtVv-8xfm3"))
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "无法打开浏览器，请检查网络", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_open_in_new), contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("前往 123 云盘")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun AboutWallHavenDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_wallhaven_entry)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("WallHaven Android 客户端", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("版本: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.about_target_platform), fontSize = 12.sp)
                Text(
                    text = stringResource(R.string.about_app_desc),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wallhaven.cc")))
                        } catch (_: Exception) {
                            Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.visit_wallhaven), fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.coolapk.com/u/3207664")))
                        } catch (_: Exception) {
                            Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.about_developer_link), fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Keepsmile31/WallHaven")))
                        } catch (_: Exception) {
                            Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("${stringResource(R.string.about_project_url)} (GitHub)", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun UserAgreementDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.user_agreement_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("1. 服务范围与宗旨", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Text("本应用为 WallHaven (wallhaven.cc) 官方开放 API 的第三方开源客户端，旨在为用户提供优质的高清壁纸浏览、检索与本地缓存管理服务。", fontSize = 13.sp, lineHeight = 18.sp)
                Text("2. 用户权利与义务", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Text("用户应在遵守法律法规的前提下使用壁纸资源，壁纸知识产权归原作者所有，仅供个人非商用学习与鉴赏。", fontSize = 13.sp, lineHeight = 18.sp)
                Text("3. 免责声明", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Text("因第三方服务器网络波动或不可抗力导致的服务中断，本客户端将协助用户重试，但不承担相关衍生责任。", fontSize = 13.sp, lineHeight = 18.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("我知道了")
            }
        }
    )
}

@Composable
private fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.privacy_policy_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("1. 数据收集原则", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Text("本应用秉持零侵入、无追踪隐私原则，不采集用户的任何个人身份、设备标识符 (IMEI/OAID) 或位置信息。", fontSize = 13.sp, lineHeight = 18.sp)
                Text("2. 本地与云端数据", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Text("用户的收藏与浏览历史完全保存在本地 SQLite 数据库中。若启用 WebDAV 备份，数据仅加密上传至用户自行设定的私有云端服务器。", fontSize = 13.sp, lineHeight = 18.sp)
                Text("3. 权限使用说明", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Text("仅在用户主动下载时使用 MediaStore 媒体库存储权限；通知权限用于前台任务下载进度展示，用户可随时在系统设置中开关。", fontSize = 13.sp, lineHeight = 18.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("我知道了")
            }
        }
    )
}
