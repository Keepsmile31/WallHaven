package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.preferences.AppSettings
import com.example.data.preferences.FilterMode
import com.example.data.preferences.UserPreferencesManager
import com.example.ui.theme.AvailableThemeColors
import com.example.util.CacheManager
import com.example.util.HapticHelper
import kotlinx.coroutines.launch

@Composable
internal fun SettingsAppearanceScreen(
    settings: AppSettings,
    preferencesManager: UserPreferencesManager,
    hapticHelper: HapticHelper,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showDarkModeDialog by remember { mutableStateOf(false) }

    SubPageScaffold(
        title = stringResource(R.string.settings_appearance),
        onBack = onBack
    ) {
        M3SettingsCard {
            val toggleDynamic = {
                val next = !settings.dynamicColor
                hapticHelper.vibrateTick(settings.hapticFeedback)
                scope.launch { preferencesManager.setDynamicColor(next) }
            }
            M3SettingsItemRow(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.settings_dynamic_color),
                subtitle = stringResource(R.string.settings_dynamic_color_desc),
                onClick = { toggleDynamic() },
                trailingContent = {
                    Switch(
                        checked = settings.dynamicColor,
                        onCheckedChange = {
                            hapticHelper.vibrateTick(settings.hapticFeedback)
                            scope.launch { preferencesManager.setDynamicColor(it) }
                        },
                        modifier = Modifier.testTag("dynamic_color_switch")
                    )
                }
            )

            if (!settings.dynamicColor) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.settings_theme_color),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvailableThemeColors.forEach { colorDef ->
                                val isSelected = settings.themeColor == colorDef.key
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            hapticHelper.vibrateTick(settings.hapticFeedback)
                                            scope.launch { preferencesManager.setThemeColor(colorDef.key) }
                                        }
                                        .padding(4.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(colorDef.primary)
                                            .then(
                                                if (isSelected) Modifier.border(
                                                    2.5.dp,
                                                    MaterialTheme.colorScheme.onSurface,
                                                    CircleShape
                                                ) else Modifier
                                            )
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(colorDef.nameRes),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.settings_color_style),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "vibrant" to R.string.style_vibrant,
                                "expressive" to R.string.style_expressive,
                                "neutral" to R.string.style_neutral,
                                "high_contrast" to R.string.style_high_contrast
                            ).forEach { (styleKey, nameRes) ->
                                FilterChip(
                                    selected = settings.colorStyle == styleKey,
                                    onClick = {
                                        hapticHelper.vibrateTick(settings.hapticFeedback)
                                        scope.launch { preferencesManager.setColorStyle(styleKey) }
                                    },
                                    label = { Text(stringResource(nameRes), fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            M3SettingsDivider()

            val currentDarkModeLabel = when (settings.darkMode) {
                "light" -> stringResource(R.string.dark_mode_light)
                "dark" -> stringResource(R.string.dark_mode_dark)
                else -> stringResource(R.string.dark_mode_follow_system)
            }
            M3SettingsItemRow(
                icon = Icons.Default.DarkMode,
                title = stringResource(R.string.settings_dark_mode),
                subtitle = currentDarkModeLabel,
                testTag = "settings_dark_mode_row",
                onClick = { showDarkModeDialog = true },
                trailingContent = {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = currentDarkModeLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            )

            M3SettingsDivider()

            val toggleAmoled = {
                val next = !settings.amoledBlack
                hapticHelper.vibrateTick(settings.hapticFeedback)
                scope.launch { preferencesManager.setAmoledBlack(next) }
            }
            M3SettingsItemRow(
                icon = Icons.Default.Contrast,
                title = stringResource(R.string.settings_amoled),
                subtitle = stringResource(R.string.settings_amoled_desc),
                onClick = { toggleAmoled() },
                trailingContent = {
                    Switch(
                        checked = settings.amoledBlack,
                        onCheckedChange = {
                            hapticHelper.vibrateTick(settings.hapticFeedback)
                            scope.launch { preferencesManager.setAmoledBlack(it) }
                        },
                        modifier = Modifier.testTag("amoled_black_switch")
                    )
                }
            )

            M3SettingsDivider()

            val toggleHaptic = {
                val next = !settings.hapticFeedback
                hapticHelper.vibrateTick(next)
                scope.launch { preferencesManager.setHapticFeedback(next) }
            }
            M3SettingsItemRow(
                icon = Icons.Default.Vibration,
                title = stringResource(R.string.settings_haptic),
                subtitle = stringResource(R.string.settings_haptic_desc),
                onClick = { toggleHaptic() },
                trailingContent = {
                    Switch(
                        checked = settings.hapticFeedback,
                        onCheckedChange = {
                            hapticHelper.vibrateTick(it)
                            scope.launch { preferencesManager.setHapticFeedback(it) }
                        },
                        modifier = Modifier.testTag("haptic_feedback_switch")
                    )
                }
            )

            M3SettingsDivider()

            val toggleHideNav = {
                val next = !settings.hideNavOnScroll
                hapticHelper.vibrateTick(settings.hapticFeedback)
                scope.launch { preferencesManager.setHideNavOnScroll(next) }
            }
            M3SettingsItemRow(
                icon = Icons.Default.VisibilityOff,
                title = stringResource(R.string.settings_hide_nav_on_scroll),
                subtitle = stringResource(R.string.settings_hide_nav_on_scroll_desc),
                onClick = { toggleHideNav() },
                trailingContent = {
                    Switch(
                        checked = settings.hideNavOnScroll,
                        onCheckedChange = {
                            hapticHelper.vibrateTick(settings.hapticFeedback)
                            scope.launch { preferencesManager.setHideNavOnScroll(it) }
                        },
                        modifier = Modifier.testTag("hide_nav_on_scroll_switch")
                    )
                }
            )

            M3SettingsDivider()

            val toggleFavCount = {
                val next = !settings.showFavoritesCount
                hapticHelper.vibrateTick(settings.hapticFeedback)
                scope.launch { preferencesManager.setShowFavoritesCount(next) }
            }
            M3SettingsItemRow(
                icon = Icons.Default.Favorite,
                title = stringResource(R.string.settings_show_favorites_count),
                subtitle = stringResource(R.string.settings_show_favorites_count_desc),
                onClick = { toggleFavCount() },
                trailingContent = {
                    Switch(
                        checked = settings.showFavoritesCount,
                        onCheckedChange = {
                            hapticHelper.vibrateTick(settings.hapticFeedback)
                            scope.launch { preferencesManager.setShowFavoritesCount(it) }
                        },
                        modifier = Modifier.testTag("show_favorites_count_switch")
                    )
                }
            )
        }
    }

    if (showDarkModeDialog) {
        DarkModeDialog(
            currentMode = settings.darkMode,
            onSelectMode = { mode ->
                hapticHelper.vibrateTick(settings.hapticFeedback)
                scope.launch { preferencesManager.setDarkMode(mode) }
                showDarkModeDialog = false
            },
            onDismiss = { showDarkModeDialog = false }
        )
    }
}

@Composable
private fun DarkModeDialog(
    currentMode: String,
    onSelectMode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val modes = listOf(
        Triple("system", stringResource(R.string.dark_mode_follow_system), "根据系统夜间模式自动切换"),
        Triple("light", stringResource(R.string.dark_mode_light), "始终使用浅色外观"),
        Triple("dark", stringResource(R.string.dark_mode_dark), "始终使用深色外观")
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_dark_mode)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                modes.forEach { (modeKey, modeTitle, modeDesc) ->
                    val isSelected = currentMode == modeKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectMode(modeKey) }
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                            .testTag("dark_mode_option_$modeKey"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = modeTitle,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = modeDesc,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
internal fun SettingsContentScreen(
    settings: AppSettings,
    preferencesManager: UserPreferencesManager,
    hapticHelper: HapticHelper,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showFilterModeDialog by remember { mutableStateOf(false) }

    SubPageScaffold(
        title = "内容与浏览",
        onBack = onBack
    ) {
        M3SettingsSectionTitle(title = "身份与密钥", icon = Icons.Default.Key)
        M3SettingsCard {
            M3SettingsItemRow(
                icon = Icons.Default.Key,
                title = "WallHaven API 密钥",
                subtitle = if (settings.apiKey.isNotBlank()) {
                    "已配置 (••••••••${settings.apiKey.takeLast(4)})"
                } else {
                    "未配置 (点击配置个人 API Key 以检索限制与私有资源)"
                },
                testTag = "settings_api_key_row",
                onClick = { showApiKeyDialog = true },
                trailingContent = {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = if (settings.apiKey.isNotBlank()) "已配置" else "配置",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            )
        }

        M3SettingsSectionTitle(title = "过滤与展示偏好", icon = Icons.Default.FilterList)
        M3SettingsCard {
            M3SettingsItemRow(
                icon = Icons.Default.FilterList,
                title = "筛选条件模式",
                subtitle = when (settings.filterMode) {
                    FilterMode.GLOBAL_SHARED -> "全局共享 (所有标签页共用同一组筛选规则)"
                    FilterMode.TAB_INDEPENDENT -> "标签独立 (各标签页保持专属的筛选状态)"
                },
                testTag = "settings_filter_mode_row",
                onClick = { showFilterModeDialog = true },
                trailingContent = {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = if (settings.filterMode == FilterMode.GLOBAL_SHARED) "全局共享" else "标签独立",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            )

            M3SettingsDivider()

            val toggleLocalization = {
                val next = !settings.tagLocalization
                hapticHelper.vibrateTick(settings.hapticFeedback)
                scope.launch { preferencesManager.setTagLocalization(next) }
            }
            M3SettingsItemRow(
                icon = Icons.Default.Translate,
                title = "标签本地化与搜索 (Beta)",
                subtitle = "开启壁纸标签中文显示与中文搜索。*当前处于测试阶段，部分词汇可能未翻译或存在异常。",
                onClick = { toggleLocalization() },
                trailingContent = {
                    Switch(
                        checked = settings.tagLocalization,
                        onCheckedChange = {
                            hapticHelper.vibrateTick(settings.hapticFeedback)
                            scope.launch { preferencesManager.setTagLocalization(it) }
                        },
                        modifier = Modifier.testTag("tag_localization_switch")
                    )
                }
            )
        }
    }

    if (showApiKeyDialog) {
        ApiKeyDialog(
            currentApiKey = settings.apiKey,
            onSave = { newKey ->
                scope.launch { preferencesManager.setApiKey(newKey.trim()) }
                hapticHelper.vibrateSuccess(settings.hapticFeedback)
                showApiKeyDialog = false
            },
            onDismiss = { showApiKeyDialog = false }
        )
    }

    if (showFilterModeDialog) {
        FilterModeDialog(
            currentMode = settings.filterMode,
            onSelectMode = { mode ->
                hapticHelper.vibrateTick(settings.hapticFeedback)
                scope.launch { preferencesManager.setFilterMode(mode) }
                showFilterModeDialog = false
            },
            onDismiss = { showFilterModeDialog = false }
        )
    }
}

@Composable
private fun ApiKeyDialog(
    currentApiKey: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var keyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("WallHaven API 密钥") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.settings_api_desc),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("API Key") },
                    placeholder = { Text("填写您的 API Key") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input")
                )
                OutlinedButton(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://1815318986.share.123pan.cn/123pan/ngLtVv-N3Fm3")
                        )
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("获取教程 (${stringResource(R.string.channel_123pan)})", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    Toast.makeText(context, "API Key 保存成功", Toast.LENGTH_SHORT).show()
                    onSave(keyInput)
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
private fun FilterModeDialog(
    currentMode: FilterMode,
    onSelectMode: (FilterMode) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        Triple(
            FilterMode.TAB_INDEPENDENT,
            "标签独立 (默认)",
            "最新、热门、随机、榜单等各标签页维护独立的筛选条件"
        ),
        Triple(
            FilterMode.GLOBAL_SHARED,
            "全局共享",
            "所有标签页共享同一套筛选条件，更改后同步应用到全局"
        )
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("筛选条件模式") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { (mode, title, desc) ->
                    val isSelected = currentMode == mode
                    val tag = if (mode == FilterMode.TAB_INDEPENDENT) {
                        "filter_mode_tab_independent"
                    } else {
                        "filter_mode_global_shared"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectMode(mode) }
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                            .testTag(tag),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = title,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = desc,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
internal fun SettingsStorageScreen(
    settings: AppSettings,
    preferencesManager: UserPreferencesManager,
    hapticHelper: HapticHelper,
    onClearCacheNow: ((() -> Unit) -> Unit)?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var cacheDisplaySize by remember { mutableStateOf(CacheManager.getFormattedCacheSize(context)) }
    var isClearingCache by remember { mutableStateOf(false) }

    val dirLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                preferencesManager.setDownloadDirectory(uri.toString())
                Toast.makeText(context, "已更新下载保存目录", Toast.LENGTH_SHORT).show()
            }
        }
    }

    SubPageScaffold(
        title = stringResource(R.string.settings_storage),
        onBack = onBack
    ) {
        M3SettingsSectionTitle(title = "缓存管理", icon = Icons.Default.CleanHands)
        M3SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "当前应用缓存大小",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "包括壁纸原图预载、缩略图磁盘缓存与网络响应",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = cacheDisplaySize,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        isClearingCache = true
                        val performCleanup = {
                            cacheDisplaySize = CacheManager.getFormattedCacheSize(context)
                            isClearingCache = false
                            hapticHelper.vibrateSuccess(settings.hapticFeedback)
                            Toast.makeText(context, "缓存已成功清理！", Toast.LENGTH_SHORT).show()
                        }

                        if (onClearCacheNow != null) {
                            onClearCacheNow { performCleanup() }
                        } else {
                            scope.launch {
                                CacheManager.clearCache(context)
                                preferencesManager.setLastCacheClearTime(System.currentTimeMillis())
                                performCleanup()
                            }
                        }
                    },
                    enabled = !isClearingCache,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clear_cache_now_button")
                ) {
                    if (isClearingCache) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("正在清理缓存...")
                    } else {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("立即清理缓存")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "自动清理缓存策略",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                val strategies = listOf(
                    Triple("none", "不自动清理", "保留本地缓存，仅在手动点击或系统空间紧张时清理"),
                    Triple("every_3_days", "每 3 天自动清理", "应用启动时若距上次清理已超 3 天，则自动重置缓存"),
                    Triple("on_exit", "退出应用时自动清理", "应用完全关闭/退出时自动清空磁盘与内存缓存")
                )

                strategies.forEach { (modeKey, title, description) ->
                    val isSelected = settings.autoClearCacheMode == modeKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                hapticHelper.vibrateTick(settings.hapticFeedback)
                                scope.launch { preferencesManager.setAutoClearCacheMode(modeKey) }
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                            .testTag("auto_clear_mode_$modeKey"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        M3SettingsSectionTitle(title = "下载保存目录", icon = Icons.Default.FolderOpen)
        M3SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "当前保存路径",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = settings.downloadDirectory,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { dirLauncher.launch(null) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("自定义目录", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                preferencesManager.setDownloadDirectory("/storage/emulated/0/Download/WallHaven")
                            }
                            Toast.makeText(context, "已恢复为默认公共下载目录", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("恢复默认路径", fontSize = 12.sp)
                    }
                }
            }
        }

        M3SettingsSectionTitle(title = "下载任务并发", icon = Icons.Default.Download)
        M3SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_concurrent_downloads),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.settings_concurrent_downloads_desc, settings.concurrentDownloads),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${settings.concurrentDownloads} 线程",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                var sliderVal by remember(settings.concurrentDownloads) {
                    mutableFloatStateOf(settings.concurrentDownloads.toFloat())
                }

                Slider(
                    value = sliderVal,
                    onValueChange = { sliderVal = it },
                    onValueChangeFinished = {
                        val count = sliderVal.toInt().coerceIn(1, 6)
                        hapticHelper.vibrateTick(settings.hapticFeedback)
                        scope.launch { preferencesManager.setConcurrentDownloads(count) }
                    },
                    valueRange = 1f..6f,
                    steps = 4,
                    modifier = Modifier.fillMaxWidth().testTag("concurrent_downloads_slider")
                )
            }
        }
    }
}
