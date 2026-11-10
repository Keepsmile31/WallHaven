package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Standardized header bar featuring the WallHaven foreground brand icon on the left,
 * followed by the title text and optional trailing content or subtitle.
 */
@Composable
fun AppHeaderBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconSize: Dp = 32.dp,
    titleFontSize: TextUnit = 22.sp,
    trailing: @Composable (RowScope.() -> Unit)? = null
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val iconTintColor = if (isDarkTheme) Color.White else Color(0xFF2E2F34)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_brand_logo),
                contentDescription = "WallHaven Icon",
                colorFilter = ColorFilter.tint(iconTintColor),
                modifier = Modifier.size(iconSize)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = titleFontSize,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (trailing != null) {
            trailing()
        }
    }
}
