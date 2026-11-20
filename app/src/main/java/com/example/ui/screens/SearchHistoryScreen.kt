package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.db.SearchHistoryEntity
import com.example.util.HapticHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchHistoryScreen(
    searchHistory: List<SearchHistoryEntity>,
    hapticHelper: HapticHelper,
    onBack: () -> Unit,
    onSelectQuery: (String) -> Unit,
    onDeleteQuery: (String) -> Unit,
    onBatchDeleteQueries: (List<String>) -> Unit,
    onClearAll: () -> Unit
) {
    var isBatchMode by remember { mutableStateOf(false) }
    var selectedQueries by remember { mutableStateOf(setOf<String>()) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val cutoutPadding = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val topInset = maxOf(statusBarPadding, cutoutPadding)
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val currentQueries = remember(searchHistory) { searchHistory.map { it.query } }

    LaunchedEffect(currentQueries) {
        if (currentQueries.isEmpty()) {
            isBatchMode = false
            selectedQueries = emptySet()
        } else {
            val validSet = currentQueries.toSet()
            val pruned = selectedQueries.intersect(validSet)
            if (pruned.size != selectedQueries.size) {
                selectedQueries = pruned
            }
        }
    }

    BackHandler(enabled = isBatchMode) {
        isBatchMode = false
        selectedQueries = emptySet()
    }

    BackHandler(enabled = !isBatchMode) {
        onBack()
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("清空搜索历史") },
            text = { Text("确定清空全部 ${searchHistory.size} 条搜索历史吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAll()
                        selectedQueries = emptySet()
                        isBatchMode = false
                        showClearAllDialog = false
                    }
                ) {
                    Text(stringResource(R.string.action_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showBatchDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            title = { Text("批量删除搜索历史") },
            text = { Text("确定删除选中的 ${selectedQueries.size} 条搜索记录吗？此操作无法撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        onBatchDeleteQueries(selectedQueries.toList())
                        selectedQueries = emptySet()
                        isBatchMode = false
                        showBatchDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    val toggleSelection: (String) -> Unit = { query ->
        hapticHelper.vibrateTick()
        selectedQueries = if (selectedQueries.contains(query)) {
            selectedQueries - query
        } else {
            selectedQueries + query
        }
    }

    val enterBatchWithItem: (String) -> Unit = { query ->
        hapticHelper.vibrateTick()
        isBatchMode = true
        selectedQueries = selectedQueries + query
    }

    val listBottomPadding = navBottomInset + if (isBatchMode) 168.dp else 88.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_history_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = topInset + 6.dp, bottom = 8.dp, start = 6.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("search_history_back_btn")
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_history),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.search_history_manage_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (searchHistory.isNotEmpty()) {
                            Text(
                                text = "共 ${searchHistory.size} 条搜索记录",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (searchHistory.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = {
                                hapticHelper.vibrateTick()
                                isBatchMode = !isBatchMode
                                if (!isBatchMode) {
                                    selectedQueries = emptySet()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("search_history_batch_btn")
                        ) {
                            Text(
                                text = if (isBatchMode) stringResource(R.string.batch_exit) else stringResource(R.string.batch_select),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = {
                                hapticHelper.vibrateTick()
                                showClearAllDialog = true
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("search_history_clear_all_btn")
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.history_clear_all),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (searchHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = navBottomInset + 88.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_history),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = stringResource(R.string.search_history_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = listBottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("all_search_history_list")
                ) {
                    items(
                        items = searchHistory,
                        key = { it.query },
                        contentType = { "search_history_item" }
                    ) { item ->
                        val isSelected = selectedQueries.contains(item.query)
                        val baseCardColor = MaterialTheme.colorScheme.surfaceVariant
                            .copy(alpha = 0.45f)
                            .compositeOver(MaterialTheme.colorScheme.background)
                        val selectedCardColor = MaterialTheme.colorScheme.primaryContainer
                            .copy(alpha = 0.45f)
                            .compositeOver(MaterialTheme.colorScheme.background)
                        val cardContainerColor = if (isBatchMode && isSelected) selectedCardColor else baseCardColor
                        val borderStroke = if (isBatchMode && isSelected) {
                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            BorderStroke(1.dp, cardContainerColor)
                        }
                        val dateText = remember(item.searchedAt) {
                            dateFormat.format(Date(item.searchedAt))
                        }

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = cardContainerColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = borderStroke,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .combinedClickable(
                                    onClick = {
                                        if (isBatchMode) {
                                            toggleSelection(item.query)
                                        } else {
                                            hapticHelper.vibrateTick()
                                            onSelectQuery(item.query)
                                        }
                                    },
                                    onLongClick = {
                                        if (!isBatchMode) {
                                            enterBatchWithItem(item.query)
                                        } else {
                                            toggleSelection(item.query)
                                        }
                                    }
                                )
                                .testTag("all_search_history_card_${item.query}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_history),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.query,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = dateText,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                    )
                                }

                                if (isBatchMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                } else {
                                    IconButton(
                                        onClick = {
                                            hapticHelper.vibrateTick()
                                            onDeleteQuery(item.query)
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("delete_all_search_history_${item.query}")
                                    ) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.ic_delete_outline),
                                            contentDescription = stringResource(R.string.search_history_delete),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Batch Action Bar
        AnimatedVisibility(
            visible = isBatchMode && searchHistory.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = navBottomInset + 84.dp, start = 12.dp, end = 12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_history_batch_bar")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.batch_selected_count, selectedQueries.size),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val allSelected = selectedQueries.size == currentQueries.size && currentQueries.isNotEmpty()
                        TextButton(
                            onClick = {
                                hapticHelper.vibrateTick()
                                selectedQueries = if (allSelected) {
                                    emptySet()
                                } else {
                                    currentQueries.toSet()
                                }
                            },
                            modifier = Modifier.testTag("search_history_select_all_btn")
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(
                                    if (allSelected) R.drawable.ic_close else R.drawable.ic_select_all
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (allSelected) stringResource(R.string.batch_deselect_all) else stringResource(R.string.batch_select_all),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showClearAllDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("search_history_batch_clear_btn")
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_delete),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.history_clear_all), fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (selectedQueries.isNotEmpty()) {
                                    showBatchDeleteDialog = true
                                }
                            },
                            enabled = selectedQueries.isNotEmpty(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("search_history_batch_delete_btn")
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_delete_outline),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (selectedQueries.isNotEmpty()) {
                                    "${stringResource(R.string.history_delete_selected)} (${selectedQueries.size})"
                                } else {
                                    stringResource(R.string.history_delete_selected)
                                },
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
