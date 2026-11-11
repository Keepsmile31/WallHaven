package com.example.ui.components

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R

/**
 * 首次启动免责声明与服务条款强制弹窗
 * - 不可外部点击取消 (dismissOnClickOutside = false)
 * - 不可返回键取消 (dismissOnBackPress = false)
 * - 强制滑动到底部阅读完整条款后方可激活“同意并继续”按钮
 * - 点击“退出应用”直接终止退出应用
 */
@Composable
fun DisclaimerDialog(
    onAgree: () -> Unit,
    onExit: () -> Unit
) {
    val scrollState = rememberScrollState()
    var hasScrolledToBottom by remember { mutableStateOf(false) }

    // 判断用户是否已滑动浏览至条款最底部
    val isCurrentlyAtBottom by remember {
        derivedStateOf {
            !scrollState.canScrollForward || (scrollState.value > 0 && scrollState.value >= scrollState.maxValue - 24)
        }
    }

    LaunchedEffect(isCurrentlyAtBottom) {
        if (isCurrentlyAtBottom) {
            hasScrolledToBottom = true
        }
    }

    Dialog(
        onDismissRequest = { /* 强制弹窗：禁止点击外部区域关闭 */ },
        properties = DialogProperties(
            dismissOnBackPress = false, // 禁止物理返回键关闭
            dismissOnClickOutside = false, // 禁止点击外部关闭
            usePlatformDefaultWidth = false
        )
    ) {
        // 拦截物理返回按键，防止返回跳过弹窗
        BackHandler(enabled = true) {
            /* 必须显式选择同意或退出 */
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 头部盾牌图标
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_security),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "免责声明与服务条款",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "首次使用须知 · 请完整滑动阅读全部条款",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 可滚动条款文本框
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(min = 200.dp, max = 390.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(14.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "欢迎使用本软件。在您使用本软件前，请务必仔细阅读以下免责声明与服务条款。点击“同意”即代表您已充分理解并自愿遵守本声明的全部内容：",
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            DisclaimerSection(
                                index = "1",
                                title = "服务与网络免责声明",
                                items = listOf(
                                    "本软件基于 Wallhaven.cc 官方公开 API 及网络公开数据接口构建，仅作为网络内容的辅助浏览与管理工具。",
                                    "本软件不针对中国大陆地区用户提供服务。在使用本软件前，请确保您的网络环境符合合规访问 Wallhaven.cc 的要求。"
                                )
                            )

                            DisclaimerSection(
                                index = "2",
                                title = "使用说明与商业化免责",
                                items = listOf(
                                    "任何主体使用本软件开展的任何商业活动（包括但不限于付费分发、捆绑销售、作为增值服务一部分、二次打包盈利等），均与本软件开发者无关。",
                                    "开发者不认可、不提供技术支持，亦不对任何第三方的商业行为承担任何形式的保证或连带责任。"
                                )
                            )

                            DisclaimerSection(
                                index = "3",
                                title = "法律合规与用户自担责任",
                                items = listOf(
                                    "您应自行确保使用本软件及访问相关数据的行为符合您所在国家或地区的法律法规。",
                                    "您对使用本软件的一切行为及由此产生的直接或间接后果承担全部法律责任。因违法、违规或不当使用本软件而导致的任何争议、损失或法律责任，均由您自行承担。"
                                )
                            )

                            DisclaimerSection(
                                index = "4",
                                title = "内容版权与第三方服务说明",
                                items = listOf(
                                    "本软件所展示的壁纸图片、元数据等内容的知识产权均归原作者或 Wallhaven.cc 平台所有，开发者不对上述内容主张任何权利。",
                                    "对于因第三方平台（Wallhaven.cc）服务中断、接口变更或内容下架等原因导致的功能受损，开发者不承担任何责任。"
                                )
                            )

                            DisclaimerSection(
                                index = "5",
                                title = "免责及“现状”提供条款",
                                items = listOf(
                                    "本软件按“现状”提供，不包含任何明示或暗示的保证。在法律允许的最大范围内，开发者免于承担因使用或无法使用本软件所导致的任何直接、间接、附带或从属损害赔偿责任。"
                                )
                            )

                            // 底部滑动已完成指示标记
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_check_circle),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "已滑至条款最底部",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // 未滑到底部时的悬浮提示徽章
                        if (!hasScrolledToBottom && scrollState.canScrollForward) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                                shape = RoundedCornerShape(20.dp),
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_downward),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "请向下滑动阅读完整条款",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 阅读进度状态反馈文案
                    if (!hasScrolledToBottom) {
                        Text(
                            text = "请先滑动浏览至条款最底部，以解锁“同意”按钮",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = "您已完整阅读上述条款，请确认是否同意以开始使用",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 操作按钮区
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 退出按钮
                        OutlinedButton(
                            onClick = onExit,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("disclaimer_exit_btn")
                        ) {
                            Text(
                                text = "退出应用",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 同意按钮（未滑动到底部时默认置灰禁用）
                        Button(
                            onClick = onAgree,
                            enabled = hasScrolledToBottom,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                                .testTag("disclaimer_agree_btn")
                        ) {
                            Text(
                                text = "同意并继续",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisclaimerSection(
    index: String,
    title: String,
    items: List<String>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = CircleShape,
                modifier = Modifier.size(20.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = index,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$index. $title",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items.forEach { text ->
            Text(
                text = "• $text",
                fontSize = 12.sp,
                lineHeight = 17.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
