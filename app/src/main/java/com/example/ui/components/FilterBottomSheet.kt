package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Immutable
data class FilterState(
    val categories: String = "111",
    val purity: String = "110",
    val scenes: Set<String> = emptySet(),
    val ratios: Set<String> = emptySet(),
    val resolutions: Set<String> = emptySet(),
    val atleast: String = "",
    val topRange: String = "1M",
    val sorting: String = "toplist",
    val order: String = "desc",
    val color: String = ""
) {
    fun hasActiveFilters(isTopTab: Boolean = false, isSearch: Boolean = false): Boolean {
        val defaultPurity = if (purity.length >= 3 && purity[2] == '1') "111" else "110"
        return scenes.isNotEmpty() ||
                ratios.isNotEmpty() ||
                resolutions.isNotEmpty() ||
                atleast.isNotBlank() ||
                color.isNotBlank() ||
                order != "desc" ||
                categories != "111" ||
                (purity != defaultPurity && purity != "110" && purity != "111") ||
                (isTopTab && topRange != "1M") ||
                (isSearch && sorting != "relevance")
    }
}

@Immutable
data class PrecomputedColor(val hex: String, val color: Color, val isLight: Boolean)

private val WallhavenColors = listOf(
    "660000", "990000", "cc0000", "cc3333", "ea4c88",
    "993399", "663399", "333399", "0066cc", "0099ff",
    "66cccc", "77cc33", "669900", "336600", "666600",
    "999900", "cccc33", "ffff00", "ffcc33", "ff9900",
    "ff6600", "cc6633", "996633", "663300", "000000",
    "999999", "cccccc", "ffffff", "424153"
)

val PrecomputedWallhavenColors: List<PrecomputedColor> = WallhavenColors.map { hex ->
    val colorInt = try {
        android.graphics.Color.parseColor("#$hex")
    } catch (_: Exception) {
        0xFF000000.toInt()
    }
    val isLight = hex.equals("ffffff", true) || hex.equals("cccccc", true) || hex.equals("ffff00", true)
    PrecomputedColor(hex, Color(colorInt), isLight)
}

val StyleOptions = listOf(
    "anime" to "二次元",
    "landscape" to "风景自然",
    "cyberpunk" to "赛博朋克",
    "minimalism" to "极简美学",
    "scifi" to "科幻未来",
    "dark" to "暗黑/OLED",
    "pixel" to "像素艺术",
    "cars" to "酷炫名车",
    "fantasy" to "奇幻玄幻",
    "architecture" to "建筑风光"
)

val AspectRatioOptions = listOf(
    "16x9" to "16:9 横屏",
    "16x10" to "16:10 宽屏",
    "21x9" to "21:9 带鱼屏",
    "32x9" to "32:9 超宽",
    "48x9" to "48:9 三屏超宽",
    "9x16" to "9:16 竖屏",
    "10x16" to "10:16 竖屏",
    "9x18" to "9:18 全面屏",
    "3x2" to "3:2 平板/PC",
    "4x3" to "4:3 传统屏",
    "5x4" to "5:4 传统屏",
    "1x1" to "1:1 正方"
)

val AtleastResolutionOptions = listOf(
    "" to "不限制",
    "1920x1080" to "1080P+ (≥1080P)",
    "2560x1440" to "2K+ (≥2K)",
    "3840x2160" to "4K+ (≥4K)",
    "7680x4320" to "8K+ (≥8K)"
)

val SpecificResolutionGroups = listOf(
    "带鱼超宽屏" to listOf(
        "2560x1080" to "2560×1080",
        "3440x1440" to "3440×1440",
        "5120x1440" to "5120×1440"
    ),
    "竖屏手机" to listOf(
        "1080x1920" to "1080×1920",
        "1440x2560" to "1440×2560",
        "2160x3840" to "2160×3840"
    ),
    "常用宽屏" to listOf(
        "1280x720" to "1280×720",
        "1600x900" to "1600×900",
        "1920x1080" to "1920×1080",
        "1920x1200" to "1920×1200",
        "2560x1440" to "2560×1440",
        "2560x1600" to "2560×1600",
        "3840x2160" to "3840×2160",
        "5120x2880" to "5120×2880",
        "7680x4320" to "7680×4320"
    )
)

val TopRangeOptions = listOf(
    "1M" to "月榜 (默认)",
    "1d" to "日榜",
    "3d" to "3天",
    "1w" to "周榜",
    "3M" to "季榜",
    "6M" to "半年",
    "1y" to "年榜"
)

val SearchSortOptions = listOf(
    "relevance" to "相关度",
    "hot" to "热度",
    "favorites" to "最多收藏",
    "date_added" to "最新发布",
    "toplist" to "热门趋势",
    "views" to "最多浏览"
)

val OrderOptions = listOf(
    "desc" to "倒序 / 降序 (默认)",
    "asc" to "正序 / 升序"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    sheetState: SheetState,
    initialFilterState: FilterState,
    isTopTab: Boolean = false,
    isRandomTab: Boolean = false,
    isSearchMode: Boolean = false,
    hasApiKey: Boolean = false,
    onDismissRequest: () -> Unit,
    onReset: () -> Unit,
    onConfirm: (FilterState) -> Unit,
    onRequireApiKey: () -> Unit
) {
    var transientState by remember(initialFilterState, hasApiKey) {
        val defaultPurity = if (hasApiKey) "111" else "110"
        val clampedPurity = if (!hasApiKey && initialFilterState.purity.length >= 3 && initialFilterState.purity[2] == '1') {
            initialFilterState.purity.substring(0, 2) + "0"
        } else if (initialFilterState.purity == "100") {
            defaultPurity
        } else {
            initialFilterState.purity
        }
        val categories = if (initialFilterState.categories == "110") "111" else initialFilterState.categories
        mutableStateOf(initialFilterState.copy(categories = categories, purity = clampedPurity))
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
        modifier = Modifier.testTag("filter_bottom_sheet")
    ) {
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(
                    top = 10.dp,
                    bottom = navBottom + 8.dp
                )
        ) {
            // Subtle drag indicator affordance
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }

            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "筛选壁纸",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = {
                        val defaultPurity = if (hasApiKey) "111" else "110"
                        val defaultState = if (isSearchMode) {
                            FilterState(categories = "111", purity = defaultPurity, sorting = "relevance", topRange = "1M")
                        } else {
                            FilterState(categories = "111", purity = defaultPurity, sorting = "toplist", topRange = "1M")
                        }
                        transientState = defaultState
                        onReset()
                        onDismissRequest()
                    },
                    modifier = Modifier.testTag("filter_sheet_reset_btn")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.filter_reset), fontSize = 12.sp)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            val scrollState = rememberScrollState()

            // Isolate content scrolling from ModalBottomSheet gestures:
            // 1. Leftover scroll delta and fling velocity are absorbed so they NEVER bleed
            //    into ModalBottomSheet to accidentally dismiss the sheet.
            // 2. The inner container has a completely stable height constraint (.weight(1f)),
            //    eliminating dynamic resize cycles that cancelled touch gestures and caused unresponsiveness.
            val contentNestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        return Offset.Zero
                    }

                    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                        // Absorb remaining scroll delta to prevent ModalBottomSheet from collapsing
                        return available
                    }

                    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                        // Absorb remaining fling velocity so upward/downward flings never dismiss the sheet
                        return available
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .nestedScroll(contentNestedScrollConnection)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sorting options (Search Mode Only)
                if (isSearchMode) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterSectionTitle("排序方式")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SearchSortOptions.forEach { (key, label) ->
                                FilterChip(
                                    selected = transientState.sorting == key,
                                    onClick = { transientState = transientState.copy(sorting = key) },
                                    label = { Text(label, fontSize = 12.sp) },
                                    modifier = Modifier.testTag("sort_chip_$key")
                                )
                            }
                        }
                    }
                }

                // Sort Direction (Descending / Ascending)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterSectionTitle("排序方向")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OrderOptions.forEach { (key, label) ->
                            FilterChip(
                                selected = transientState.order == key,
                                onClick = { transientState = transientState.copy(order = key) },
                                label = { Text(label, fontSize = 12.sp) },
                                modifier = Modifier.testTag("order_chip_$key")
                            )
                        }
                    }
                }

                // Top List Period (Two-row layout)
                if ((isTopTab && !isRandomTab && !isSearchMode) || (isSearchMode && transientState.sorting == "toplist")) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterSectionTitle("榜单周期 (默认月榜)")
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TopRangeOptions.take(4).forEach { (key, label) ->
                                    FilterChip(
                                        selected = transientState.topRange == key,
                                        onClick = { transientState = transientState.copy(topRange = key) },
                                        label = { Text(label, fontSize = 12.sp) },
                                        modifier = Modifier.testTag("top_range_chip_$key")
                                    )
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TopRangeOptions.drop(4).forEach { (key, label) ->
                                    FilterChip(
                                        selected = transientState.topRange == key,
                                        onClick = { transientState = transientState.copy(topRange = key) },
                                        label = { Text(label, fontSize = 12.sp) },
                                        modifier = Modifier.testTag("top_range_chip_$key")
                                    )
                                }
                            }
                        }
                    }
                }

                // Categories (General, Anime, People)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterSectionTitle("壁纸类别 (多选)")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val cats = listOf(
                            0 to stringResource(R.string.cat_general),
                            1 to stringResource(R.string.cat_anime),
                            2 to stringResource(R.string.cat_people)
                        )
                        cats.forEach { (index, title) ->
                            val isChecked = transientState.categories.getOrNull(index) == '1'
                            FilterChip(
                                selected = isChecked,
                                onClick = {
                                    val arr = transientState.categories.toCharArray()
                                    if (index in arr.indices) {
                                        arr[index] = if (arr[index] == '1') '0' else '1'
                                        if (arr.none { it == '1' }) arr[index] = '1'
                                        transientState = transientState.copy(categories = String(arr))
                                    }
                                },
                                label = { Text(title, fontSize = 12.sp) },
                                leadingIcon = if (isChecked) {
                                    { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier.testTag("cat_chip_$index")
                            )
                        }
                    }
                }

                // Purity (Normal, Rough, Restricted)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterSectionTitle("内容分级 (多选)")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val purities = listOf(
                            0 to stringResource(R.string.purity_normal),
                            1 to stringResource(R.string.purity_rough),
                            2 to stringResource(R.string.purity_restricted)
                        )
                        purities.forEach { (index, title) ->
                            val isChecked = transientState.purity.getOrNull(index) == '1'
                            val isRestricted = index == 2
                            FilterChip(
                                selected = isChecked,
                                onClick = {
                                    if (isRestricted && !isChecked && !hasApiKey) {
                                        onRequireApiKey()
                                        return@FilterChip
                                    }
                                    val arr = transientState.purity.toCharArray()
                                    if (index in arr.indices) {
                                        arr[index] = if (arr[index] == '1') '0' else '1'
                                        if (arr.none { it == '1' }) arr[index] = '1'
                                        transientState = transientState.copy(purity = String(arr))
                                    }
                                },
                                label = { Text(title, fontSize = 12.sp) },
                                leadingIcon = if (isChecked) {
                                    { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else if (isRestricted && !hasApiKey) {
                                    { Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                colors = if (isRestricted && isChecked) {
                                    FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                } else FilterChipDefaults.filterChipColors(),
                                modifier = Modifier.testTag("purity_chip_$index")
                            )
                        }
                    }
                }

                // Styles (Multi-select)
                val isStyleDisabled = isRandomTab
                Column(
                    modifier = if (isStyleDisabled) Modifier.alpha(0.38f) else Modifier,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterSectionTitle("风格题材 (多选)${if (isStyleDisabled) " - 随机模式不支持" else ""}")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StyleOptions.forEach { (key, label) ->
                            val isSelected = key in transientState.scenes
                            FilterChip(
                                selected = isSelected,
                                enabled = !isStyleDisabled,
                                onClick = {
                                    val newSet = if (isSelected) {
                                        transientState.scenes - key
                                    } else {
                                        transientState.scenes + key
                                    }
                                    transientState = transientState.copy(scenes = newSet)
                                },
                                label = { Text(label, fontSize = 12.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                modifier = Modifier.testTag("style_chip_$key")
                            )
                        }
                    }
                }

                // Aspect Ratios (Multi-select)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterSectionTitle("屏幕比例 (多选)")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AspectRatioOptions.forEach { (key, label) ->
                            val isSelected = key in transientState.ratios
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newSet = if (isSelected) {
                                        transientState.ratios - key
                                    } else {
                                        transientState.ratios + key
                                    }
                                    transientState = transientState.copy(ratios = newSet)
                                },
                                label = { Text(label, fontSize = 12.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                modifier = Modifier.testTag("ratio_chip_$key")
                            )
                        }
                    }
                }

                // Minimum Resolution Threshold (Atleast)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterSectionTitle("最低分辨率门槛 (≥)")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AtleastResolutionOptions.forEach { (key, label) ->
                            val isSelected = transientState.atleast == key
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    transientState = transientState.copy(
                                        atleast = if (isSelected && key.isNotBlank()) "" else key
                                    )
                                },
                                label = { Text(label, fontSize = 12.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                modifier = Modifier.testTag("atleast_chip_${key.ifBlank { "none" }}")
                            )
                        }
                    }
                }

                // Specific Resolutions (Multi-select)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterSectionTitle("精确指定分辨率 (多选)")
                    SpecificResolutionGroups.forEach { (groupTitle, resolutionsList) ->
                        Text(
                            text = groupTitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        ) {
                            resolutionsList.forEach { (key, label) ->
                                val isSelected = key in transientState.resolutions
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val newSet = if (isSelected) {
                                            transientState.resolutions - key
                                        } else {
                                            transientState.resolutions + key
                                        }
                                        transientState = transientState.copy(resolutions = newSet)
                                    },
                                    label = { Text(label, fontSize = 12.sp) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null,
                                    modifier = Modifier.testTag("res_chip_$key")
                                )
                            }
                        }
                    }
                }

                // Color Filter (Moved to the very bottom: 29 Standard Wallhaven Color Swatches)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterSectionTitle("色彩筛选 (官方 29 色标准调色盘)")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PrecomputedWallhavenColors.forEach { item ->
                            val isSelected = transientState.color.equals(item.hex, ignoreCase = true)

                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(item.color)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        transientState = transientState.copy(
                                            color = if (isSelected) "" else item.hex
                                        )
                                    }
                                    .testTag("color_swatch_${item.hex}"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected color #${item.hex}",
                                        tint = if (item.isLight) Color.Black else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (transientState.color.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { transientState = transientState.copy(color = "") }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear color",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "清除已选色彩 (#${transientState.color})",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(10.dp))

            // Confirm button
            Button(
                onClick = {
                    onConfirm(transientState)
                    onDismissRequest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(48.dp)
                    .testTag("filter_sheet_confirm_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.filter_confirm), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun FilterSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 13.sp
    )
}
