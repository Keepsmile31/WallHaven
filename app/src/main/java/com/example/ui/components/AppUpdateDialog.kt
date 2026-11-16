package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.R
import com.example.data.repository.AppUpdateInfo

@Composable
internal fun AppUpdateDialog(
    updateInfo: AppUpdateInfo,
    onUpdateNow: () -> Unit,
    onRemindLater: () -> Unit,
    onIgnoreVersion: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onRemindLater,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .widthIn(max = 560.dp),
        icon = {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_cloud_download),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "发现新版本 ${updateInfo.tagName}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("app_update_dialog_body"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "当前版本: v${BuildConfig.VERSION_NAME} → 最新版本: ${updateInfo.tagName}",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (updateInfo.apkFileName.isNotBlank()) {
                            val sizeLabel = if (updateInfo.apkSizeBytes > 0L) {
                                val mb = updateInfo.apkSizeBytes / (1024.0 * 1024.0)
                                String.format(java.util.Locale.US, " (%.1f MB)", mb)
                            } else ""
                            Text(
                                text = "安装包: ${updateInfo.apkFileName}$sizeLabel",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                Text(
                    text = "更新说明",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = updateInfo.releaseNotes,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("app_update_release_notes")
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.apkDownloadUrl))
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "无法打开浏览器下载安装包", Toast.LENGTH_SHORT).show()
                        }
                        onUpdateNow()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("update_now_btn")
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_open_in_new),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("立即更新", fontWeight = FontWeight.SemiBold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onIgnoreVersion,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("update_ignore_btn")
                    ) {
                        Text("不再提示", fontSize = 13.sp)
                    }

                    TextButton(
                        onClick = onRemindLater,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("update_later_btn")
                    ) {
                        Text("以后再说", fontSize = 13.sp)
                    }
                }
            }
        }
    )
}
