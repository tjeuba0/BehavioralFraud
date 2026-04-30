package com.poc.behavioralfraud.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.poc.behavioralfraud.ui.theme.IPayTheme

/**
 * Status pill badge — FR-CL-08 REQ-21.
 *
 * Pill shape (`shapes.full`) chứa dot 6dp bên trái + label. 5 variant:
 *  - Success / Warning / Error / Info / Neutral
 *
 * POC tint: dùng `iconX.copy(alpha = 0.12f)` cho background light tint thay vì
 * tạo riêng token bg (Figma chưa có). Nếu cần đậm hơn — bump alpha.
 */
@Composable
fun IPayStatusBadge(
    text: String,
    variant: IPayStatusBadgeVariant,
    modifier: Modifier = Modifier,
) {
    val resolved = variant.resolve()

    Row(
        modifier = modifier
            .clip(IPayTheme.shapes.full)
            .background(resolved.bg)
            .padding(
                horizontal = IPayTheme.spacing.s8,
                vertical = IPayTheme.spacing.s4,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s6),
    ) {
        Box(
            modifier = Modifier
                .size(IPayTheme.spacing.s6)
                .clip(IPayTheme.shapes.full)
                .background(resolved.dot),
        )
        Text(
            text = text,
            style = IPayTheme.typography.bodyEmphasizedSmall,
            color = resolved.label,
        )
    }
}

enum class IPayStatusBadgeVariant { Success, Warning, Error, Info, Neutral }

private data class IPayStatusBadgeColors(
    val bg: Color,
    val label: Color,
    val dot: Color,
)

@Composable
private fun IPayStatusBadgeVariant.resolve(): IPayStatusBadgeColors {
    val colors = IPayTheme.colors
    return when (this) {
        IPayStatusBadgeVariant.Success -> IPayStatusBadgeColors(
            bg = colors.iconSuccess.copy(alpha = IPayBadgeConstants.STATUS_TINT_ALPHA),
            label = colors.textSuccess,
            dot = colors.iconSuccess,
        )
        IPayStatusBadgeVariant.Warning -> IPayStatusBadgeColors(
            bg = colors.iconWarning.copy(alpha = IPayBadgeConstants.STATUS_TINT_ALPHA),
            label = colors.iconWarning,
            dot = colors.iconWarning,
        )
        IPayStatusBadgeVariant.Error -> IPayStatusBadgeColors(
            bg = colors.tabIndicatorActive.copy(alpha = IPayBadgeConstants.STATUS_TINT_ALPHA),
            label = colors.tabIndicatorActive,
            dot = colors.tabIndicatorActive,
        )
        IPayStatusBadgeVariant.Info -> IPayStatusBadgeColors(
            bg = colors.bgBrandSecondary,
            label = colors.iconInfo,
            dot = colors.iconInfo,
        )
        IPayStatusBadgeVariant.Neutral -> IPayStatusBadgeColors(
            bg = colors.bgNeutralSecondary,
            label = colors.textNeutralSecondary,
            dot = colors.iconNeutralSecondary,
        )
    }
}

/**
 * Notification badge — FR-CL-08 REQ-22.
 *
 * Hai mode:
 *  - Dot mode: [count] = null → hiển thị 1 chấm tròn 8dp.
 *  - Count mode: [count] != null → pill chứa số (`9+` nếu > 9).
 *
 * Background: linear gradient `notificationBgStart → notificationBgEnd` (red).
 * White outside border (`stroke.lg`) để tăng visibility trên container brand.
 */
@Composable
fun IPayNotificationBadge(
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    val colors = IPayTheme.colors
    val gradient: Brush = Brush.horizontalGradient(
        listOf(colors.notificationBgStart, colors.notificationBgEnd),
    )

    if (count == null) {
        Box(
            modifier = modifier
                .size(IPayTheme.spacing.s8)
                .clip(IPayTheme.shapes.full)
                .background(gradient)
                .border(
                    width = IPayTheme.stroke.lg,
                    brush = SolidColor(colors.notificationOutsideBorder),
                    shape = IPayTheme.shapes.full,
                ),
        )
    } else {
        val display = if (count > IPayBadgeConstants.COUNT_MAX_DISPLAY) {
            "${IPayBadgeConstants.COUNT_MAX_DISPLAY}+"
        } else {
            count.toString()
        }
        Box(
            modifier = modifier
                .defaultMinSize(minWidth = IPayTheme.spacing.s16, minHeight = IPayTheme.spacing.s16)
                .height(IPayTheme.spacing.s16)
                .clip(IPayTheme.shapes.full)
                .background(gradient)
                .border(
                    width = IPayTheme.stroke.lg,
                    brush = SolidColor(colors.notificationOutsideBorder),
                    shape = IPayTheme.shapes.full,
                )
                .padding(horizontal = IPayTheme.spacing.s4),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = display,
                style = IPayTheme.typography.labelXS,
                color = colors.notificationLabel,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private object IPayBadgeConstants {
    const val STATUS_TINT_ALPHA: Float = 0.12f
    const val COUNT_MAX_DISPLAY: Int = 9
}

@Preview(name = "StatusBadge — All variants", showBackground = true)
@Composable
private fun IPayStatusBadgePreviewAll() {
    IPayTheme {
        Row(
            modifier = Modifier.padding(IPayTheme.spacing.s16),
            horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8),
        ) {
            IPayStatusBadge(text = "Thành công", variant = IPayStatusBadgeVariant.Success)
            IPayStatusBadge(text = "Cảnh báo", variant = IPayStatusBadgeVariant.Warning)
            IPayStatusBadge(text = "Lỗi", variant = IPayStatusBadgeVariant.Error)
        }
    }
}

@Preview(name = "StatusBadge — Info + Neutral", showBackground = true)
@Composable
private fun IPayStatusBadgePreviewInfoNeutral() {
    IPayTheme {
        Row(
            modifier = Modifier.padding(IPayTheme.spacing.s16),
            horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8),
        ) {
            IPayStatusBadge(text = "Đang xử lý", variant = IPayStatusBadgeVariant.Info)
            IPayStatusBadge(text = "Mới", variant = IPayStatusBadgeVariant.Neutral)
        }
    }
}

@Preview(name = "NotificationBadge — Dot + counts", showBackground = true)
@Composable
private fun IPayNotificationBadgePreview() {
    IPayTheme {
        Row(
            modifier = Modifier.padding(IPayTheme.spacing.s16),
            horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IPayNotificationBadge()
            Spacer(Modifier.width(IPayTheme.spacing.s4))
            IPayNotificationBadge(count = 3)
            IPayNotificationBadge(count = 12)
        }
    }
}
