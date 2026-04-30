package com.poc.behavioralfraud.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.poc.behavioralfraud.ui.theme.IPayTheme

/**
 * Selection card — FR-CL-08 REQ-19.
 *
 * Card chứa label + (optional) description bên trái, radio/checkbox indicator bên phải.
 *  - Default: bg `bgNeutralPrimary`, border `borderNeutralPrimary`, width `stroke.xs`.
 *  - Selected: bg `bgBrandSecondary`, border `borderBrandPrimary`, width `stroke.s`.
 *
 * Click toàn card sẽ trigger [onClick] (qua [safeClickable]).
 */
@Composable
fun IPaySelection(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: IPaySelectionVariant = IPaySelectionVariant.Radio,
    description: String? = null,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val colors = IPayTheme.colors
    val spacing = IPayTheme.spacing
    val shape = IPayTheme.shapes.small

    val bgColor = if (selected) colors.bgBrandSecondary else colors.bgNeutralPrimary
    val borderColor = if (selected) colors.borderBrandPrimary else colors.borderNeutralPrimary
    val borderWidth = if (selected) IPayTheme.stroke.s else IPayTheme.stroke.xs

    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else IPaySelectionConstants.DISABLED_ALPHA)
            .clip(shape)
            .background(bgColor)
            .border(width = borderWidth, brush = SolidColor(borderColor), shape = shape)
            .safeClickable(
                enabled = enabled,
                role = if (variant == IPaySelectionVariant.Radio) Role.RadioButton else Role.Checkbox,
                onSafeClick = onClick,
            )
            .padding(spacing.s16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (selected) colors.iconBrandPrimary else colors.iconNeutralPrimary,
                modifier = Modifier.size(spacing.s24),
            )
            Spacer(Modifier.width(spacing.s12))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = IPayTheme.typography.bodyEmphasizedMedium,
                color = colors.textNeutralPrimary,
            )
            if (description != null) {
                Spacer(Modifier.size(spacing.s2))
                Text(
                    text = description,
                    style = IPayTheme.typography.bodySmall,
                    color = colors.textNeutralSecondary,
                )
            }
        }

        Spacer(Modifier.width(spacing.s12))

        when (variant) {
            IPaySelectionVariant.Radio -> RadioIndicator(selected = selected)
            IPaySelectionVariant.Checkbox -> CheckboxIndicator(selected = selected)
        }
    }
}

@Composable
private fun RadioIndicator(selected: Boolean) {
    val colors = IPayTheme.colors
    val ringColor = if (selected) colors.borderBrandPrimary else colors.borderNeutralSecondary

    Box(
        modifier = Modifier
            .size(IPayTheme.spacing.s20)
            .clip(IPayTheme.shapes.full)
            .border(
                width = IPayTheme.stroke.lg,
                brush = SolidColor(ringColor),
                shape = IPayTheme.shapes.full,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(IPayTheme.spacing.s10)
                    .clip(IPayTheme.shapes.full)
                    .background(colors.borderBrandPrimary),
            )
        }
    }
}

@Composable
private fun CheckboxIndicator(selected: Boolean) {
    val colors = IPayTheme.colors
    val shape = IPayTheme.shapes.r4
    val borderColor = if (selected) colors.borderBrandPrimary else colors.borderNeutralSecondary
    val bgColor = if (selected) colors.borderBrandPrimary else colors.bgNeutralPrimary

    Box(
        modifier = Modifier
            .size(IPayTheme.spacing.s20)
            .clip(shape)
            .background(bgColor)
            .border(width = IPayTheme.stroke.lg, brush = SolidColor(borderColor), shape = shape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = colors.iconOnColorPrimary,
                modifier = Modifier.size(IPayTheme.spacing.s16),
            )
        }
    }
}

enum class IPaySelectionVariant { Radio, Checkbox }

private object IPaySelectionConstants {
    const val DISABLED_ALPHA: Float = 0.4f
}

@Preview(name = "Selection — Radio", showBackground = true)
@Composable
private fun IPaySelectionPreviewRadio() {
    IPayTheme {
        Column(
            modifier = Modifier.padding(IPayTheme.spacing.s16),
            verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s12),
        ) {
            IPaySelection(label = "Chuyển nhanh 24/7", selected = true, onClick = {})
            IPaySelection(label = "Chuyển thường", selected = false, onClick = {})
        }
    }
}

@Preview(name = "Selection — Checkbox + description", showBackground = true)
@Composable
private fun IPaySelectionPreviewCheckbox() {
    IPayTheme {
        Column(
            modifier = Modifier.padding(IPayTheme.spacing.s16),
            verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s12),
        ) {
            IPaySelection(
                label = "Lưu người nhận",
                description = "Thêm vào danh sách yêu thích",
                selected = true,
                onClick = {},
                variant = IPaySelectionVariant.Checkbox,
                leadingIcon = Icons.Default.Star,
            )
            IPaySelection(
                label = "Nhận thông báo",
                description = "Push notification khi giao dịch thành công",
                selected = false,
                onClick = {},
                variant = IPaySelectionVariant.Checkbox,
            )
        }
    }
}
