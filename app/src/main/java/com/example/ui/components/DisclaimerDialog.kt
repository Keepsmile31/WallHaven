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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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

internal const val DISCLAIMER_CONFIRM_STATEMENT_ZH =
    "我承诺身处中国大陆以外地区且已年满18周岁，自愿承担使用本第三方客户端的一切法律责任。"

internal const val DISCLAIMER_CONFIRM_STATEMENT_EN =
    "I confirm that I am located outside of mainland China, am at least 18 years of age, and voluntarily assume all legal responsibility for my use of this third-party client."

private val ZH_CHAR_AND_DIGIT_REGEX = Regex("[^\\u4e00-\\u9fa50-9]")
private val EN_CHAR_AND_DIGIT_REGEX = Regex("[^a-z0-9]")

private val EXPECTED_ZH_CANONICAL =
    DISCLAIMER_CONFIRM_STATEMENT_ZH.replace(ZH_CHAR_AND_DIGIT_REGEX, "")
private val EXPECTED_EN_CANONICAL =
    DISCLAIMER_CONFIRM_STATEMENT_EN.lowercase().replace(EN_CHAR_AND_DIGIT_REGEX, "")

internal fun isDisclaimerConfirmationMatched(input: String): Boolean {
    if (input.isBlank()) return false
    val normalizedZh = input.replace(ZH_CHAR_AND_DIGIT_REGEX, "")
    if (normalizedZh == EXPECTED_ZH_CANONICAL) return true

    val normalizedEn = input.lowercase().replace(EN_CHAR_AND_DIGIT_REGEX, "")
    return normalizedEn == EXPECTED_EN_CANONICAL
}

/**
 * 首次启动及条款更新后的免责声明与服务条款强制弹窗（两步确认）：
 * 1. 强制阅读条款并滑动到底部，激活“同意并继续”按钮；点击“拒绝并退出”直接退出应用。
 * 2. 点击“同意并继续”后进入二次确认弹窗，需手动输入确认声明后点击“确认”并记录确认时间戳方可进入软件。
 */
@Composable
fun DisclaimerDialog(
    onAgree: (Long) -> Unit,
    onExit: () -> Unit
) {
    val scrollState = rememberScrollState()
    var hasScrolledToBottom by remember { mutableStateOf(false) }
    var showConfirmationPrompt by remember { mutableStateOf(false) }
    var confirmInput by remember { mutableStateOf("") }

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
        SafeTextToolbarProvider {
            val textToolbar = androidx.compose.ui.platform.LocalTextToolbar.current
            LaunchedEffect(showConfirmationPrompt) {
                textToolbar.hide()
            }

            // 拦截物理返回按键：第二步可退回第一步，第一步禁止退出弹窗
            BackHandler(enabled = true) {
                if (showConfirmationPrompt) {
                    textToolbar.hide()
                    showConfirmationPrompt = false
                }
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
                        .widthIn(max = 520.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 16.dp
                ) {
                    if (!showConfirmationPrompt) {
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
                                .heightIn(min = 120.dp, max = 400.dp)
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
                                    .padding(16.dp)
                                    .testTag("disclaimer_scroll_content"),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                WallhavenAgreementBody(showFooterIndicator = true)
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
                                text = "请先滑动浏览至条款最底部，以解锁“同意并继续”按钮",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "您已完整阅读上述条款，点击“同意并继续”后将进入最终确认",
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
                            OutlinedButton(
                                onClick = onExit,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("disclaimer_exit_btn")
                            ) {
                                Text(
                                    text = "拒绝并退出",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = { showConfirmationPrompt = true },
                                enabled = hasScrolledToBottom,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                ),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
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
                } else {
                    val isConfirmationValid by remember(confirmInput) {
                        derivedStateOf { isDisclaimerConfirmationMatched(confirmInput) }
                    }
                    val showMismatchHint = confirmInput.isNotBlank() && !isConfirmationValid

                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_rule),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "最终确认提示",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "为确认您已充分知悉上述条款，请在下方输入框中输入以下声明：",
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // 可选中并复制的声明展示卡片
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    SelectionContainer {
                                        Text(
                                            text = DISCLAIMER_CONFIRM_STATEMENT_ZH,
                                            fontSize = 13.5.sp,
                                            lineHeight = 20.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.testTag("disclaimer_confirm_statement")
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = confirmInput,
                                onValueChange = { confirmInput = it },
                                label = { Text("确认声明输入框") },
                                placeholder = { Text("请输入上述确认声明") },
                                isError = showMismatchHint,
                                minLines = 2,
                                maxLines = 4,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("disclaimer_confirm_input")
                            )

                            if (showMismatchHint) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "输入内容与声明不匹配，请核对后重新输入或粘贴。",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else if (isConfirmationValid) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "声明校验通过，点击“确认”即可进入软件。",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showConfirmationPrompt = false },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("disclaimer_confirm_back_btn")
                            ) {
                                Text(
                                    text = "返回条款",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = { onAgree(System.currentTimeMillis()) },
                                enabled = isConfirmationValid,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                ),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .testTag("disclaimer_confirm_submit_btn")
                            ) {
                                Text(
                                    text = "确认",
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
    }
}

/**
 * 可复用的《WallHaven 用户协议与免责声明》完整正文内容
 * 同时用于首次启动弹窗和“设置 > 关于与帮助 > 免责声明与服务条款”
 */
@Composable
internal fun WallhavenAgreementBody(
    showFooterIndicator: Boolean = false
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "WallHaven 用户协议与免责声明",
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "更新日期： 2026年11月15日",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "生效日期： 2026年11月15日",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "【重要地域与服务限制声明】",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "本应用不面向中华人民共和国大陆地区（以下简称“中国大陆”）用户提供任何形式的服务。如果您身处中国大陆境内，或您的网络接入地位于中国大陆，请立即停止下载、安装、访问或使用本应用，并立即将其从您的设备中卸载。",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "欢迎使用 WallHaven 第三方客户端（以下简称“本应用”）。在您开始使用本应用之前，请务必审慎阅读、充分理解本《用户协议与免责声明》（以下简称“本协议”）的全部内容。",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "一旦您点击“同意并继续”或实际开始使用本应用，即视为您已作出具有法律约束力的承诺：您目前不处于中国大陆境内，您已年满 18 周岁，且完全阅读、理解并同意接受本协议所有条款的约束。如果您不符合上述任何一项条件，请立即点击“拒绝并退出”并卸载本应用。",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AgreementArticleSection(
            badge = "一",
            title = "第一条 服务对象与严格地域限制（核心条款）",
            items = listOf(
                "明确排除中国大陆服务：本应用严格不向中国大陆地区的自然人、法人或其他组织提供服务，亦不针对中国大陆市场进行任何形式的运营、推广或技术支持。",
                "违规跨区使用责任自负：任何位于中国大陆境内的用户，不得通过任何技术手段（包括但不限于代理服务器、VPN、修改系统区域或网络环境等）规避本协议的地域限制强行使用本应用。因用户违反本条规定在中国大陆境内强行下载、安装、使用本应用所产生的一切行政处罚、刑事责任、民事纠纷及其他法律后果，均由该用户本人独立承担，与本应用开发者无关。",
                "年龄限制：本应用仅面向所在国家或地区法律法规允许合法访问相关第三方服务的成年用户（年满 18 周岁）提供。 未满 18 周岁的未成年人严禁使用本应用。"
            )
        )

        AgreementArticleSection(
            badge = "二",
            title = "第二条 独立客户端声明与技术中立性",
            items = listOf(
                "非官方客户端声明：本应用是由独立开发者基于公开的 WallHaven API 开发的第三方数据解析与壁纸浏览工具，与 WallHaven 官方网站及其运营方无任何隶属、关联、合作或官方授权关系。",
                "纯本地工具属性：本应用完全在用户本地设备上运行，不提供任何服务器存储、内容托管、数据库缓存、网络代理或跨境加速中转服务。您通过本应用检索、浏览或下载的所有图像、标签及元数据，均由您的设备在本地直接向第三方平台发起网络请求并实时获取，本应用不对上述数据流进行任何形式的拦截、存储或二次分发。"
            )
        )

        AgreementArticleSection(
            badge = "三",
            title = "第三条 第三方内容与知识产权免责",
            items = listOf(
                "版权归属：本应用展示的所有壁纸作品、缩略图、标签及相关元数据，以及“WallHaven”相关名称与标识，其知识产权及所有权均归属于原版权方及第三方平台（wallhaven.cc）所有。本应用仅提供技术性的视图呈现，不享有任何内容版权，亦不对第三方内容的真实性、合法性、准确性或安全性作任何明示或暗示的担保。",
                "版权投诉与处理：由于本应用不存储任何图像数据，若权利人认为相关内容侵犯了合法权益，请直接联系原始资源托管平台进行删除或下架处理；一旦源站移除相关内容，本应用将自动同步不再显示。"
            )
        )

        AgreementArticleSection(
            badge = "四",
            title = "第四条 自定义 API Key 与内容分级说明",
            items = listOf(
                "默认安全过滤机制：本应用在默认状态下仅请求并展示常规全年龄向（SFW）内容。",
                "用户自主配置责任：本应用提供的“自定义 API Key”及高级筛选参数配置功能，仅作为标准接口协议的通用技术实现，仅供所在司法管辖区法律允许的成年用户用于管理个人账号偏好。",
                "风险自担原则：您自行获取、填入第三方平台 API Key 以及手动调整内容过滤级别的行为，完全属于您的个人自主操作。因您自行配置 API Key 或修改筛选参数所获取的任何第三方内容，其浏览、存储及传播所产生的一切法律后果与道德责任，均由您本人独立承担，开发者不承担任何连带责任。"
            )
        )

        AgreementArticleSection(
            badge = "五",
            title = "第五条 开源、非营利与服务中断免责",
            items = listOf(
                "非营利性质：本应用为个人技术学习与界面交互设计研究产物，完全免费提供，不包含任何付费会员、内购项目或商业广告。任何第三方对本应用进行的二次打包、植入恶意代码、汉化分发或商业售卖行为均未获得开发者授权，由此引发的一切纠纷与法律责任由该第三方自行承担。",
                "服务可用性免责：因第三方接口（WallHaven API）策略变更、服务器宕机、地区网络限制、不可抗力或您自身网络环境原因导致本应用部分或全部功能无法正常使用的，开发者不承担任何违约或赔偿责任，且开发者有权随时暂停、修改或终止本应用的维护与更新。"
            )
        )

        AgreementArticleSection(
            badge = "六",
            title = "第六条 协议修改与法律适用",
            items = listOf(
                "协议更新：开发者有权根据法律环境变化或软件功能调整随时修订本协议，修订后的协议一经在应用内公布即自动生效。若您继续使用本应用，即视为接受修订后的协议。",
                "可分割性：若本协议中的任何条款因与您所在司法管辖区的强制性法律相抵触而被认定为无效或不可执行，该条款应在法律允许的最大范围内予以解释，且不影响本协议其余条款的法律效力。"
            )
        )

        if (showFooterIndicator) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 4.dp)
                    .testTag("disclaimer_bottom_marker"),
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
    }
}

@Composable
private fun AgreementArticleSection(
    badge: String,
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
                        text = badge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items.forEach { text ->
            Text(
                text = "• $text",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    }
}
