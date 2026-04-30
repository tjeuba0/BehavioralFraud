package com.poc.behavioralfraud.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.poc.behavioralfraud.ui.theme.IPayTheme

/**
 * Chip (pill) — FR-CL-08 REQ-14.
 *
 * Variants:
 *  - [IPayChipVariant.Default]  = bg `chipBg`, border `chipBorder`, label `chipLabel`
 *  - [IPayChipVariant.Selected] = bg `bgBrandSecondary`, border `borderBrandPrimary`, label `textBrandPrimary`
 *
 * Shape `IPayShapes.full` (50% radius — pill). Click goes through [safeClickable]
 * (350ms debounce). Optional [leadingIcon] painted in label color.
 */
@Composable
fun IPayChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: IPayChipVariant = IPayChipVariant.Default,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = IPayTheme.colors
    val spacing = IPayTheme.spacing

    val bg = when (variant) {
        IPayChipVariant.Default -> colors.chipBg
        IPayChipVariant.Selected -> colors.bgBrandSecondary
    }
    val border = when (variant) {
        IPayChipVariant.Default -> colors.chipBorder
        IPayChipVariant.Selected -> colors.borderBrandPrimary
    }
    val label = when (variant) {
        IPayChipVariant.Default -> colors.chipLabel
        IPayChipVariant.Selected -> colors.textBrandPrimary
    }

    Box(
        modifier = modifier
            .clip(IPayTheme.shapes.full)
            .background(bg)
            .border(width = IPayTheme.stroke.s, brush = SolidColor(border), shape = IPayTheme.shapes.full)
            .safeClickable(enabled = enabled, role = Role.Button, onSafeClick = onClick)
            .padding(horizontal = spacing.s12, vertical = spacing.s6),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            leadingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = label,
                    modifier = Modifier.size(spacing.s16),
                )
                Spacer(Modifier.width(spacing.s6))
            }
            Text(
                text = text,
                style = IPayTheme.typography.labelSmall,
                color = label,
            )
        }
    }
}

/**
 * AI chip — FR-CL-08 REQ-15.
 *
 * Decorative pill with **gradient border** (5 stops) + **gradient label** (3 stops).
 * Reserved for AI features only — NOT used for promotions / generic highlights.
 *
 * Static (no click). Background `aiChipBg` (default white), optional leading sparkle
 * icon tinted with mid-gradient color (icon uses single tint — POC compromise; full
 * gradient on icon would require a custom DrawScope painter). Border via
 * [androidx.compose.foundation.border] with a [Brush.linearGradient]; label gradient
 * applied via [androidx.compose.ui.text.TextStyle.brush].
 */
@Composable
fun IPayAIChip(
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = Icons.Default.AutoAwesome,
) {
    val colors = IPayTheme.colors
    val spacing = IPayTheme.spacing

    val borderBrush = Brush.linearGradient(
        listOf(
            colors.aiChipBorderStart,
            colors.aiChipBorderMiddle1,
            colors.aiChipBorderMiddle2,
            colors.aiChipBorderMiddle3,
            colors.aiChipBorderEnd,
        ),
    )
    val labelBrush = Brush.horizontalGradient(
        listOf(
            colors.aiChipLabelStart,
            colors.aiChipLabelMiddle,
            colors.aiChipLabelEnd,
        ),
    )

    Box(
        modifier = modifier
            .clip(IPayTheme.shapes.full)
            .background(colors.aiChipBg)
            .border(width = IPayTheme.stroke.s, brush = borderBrush, shape = IPayTheme.shapes.full)
            .padding(horizontal = spacing.s12, vertical = spacing.s6),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            leadingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = colors.aiChipLabelMiddle,
                    modifier = Modifier.size(spacing.s16),
                )
                Spacer(Modifier.width(spacing.s6))
            }
            Text(
                text = text,
                style = IPayTheme.typography.labelSmall.copy(brush = labelBrush),
                color = Color.Unspecified,
            )
        }
    }
}

enum class IPayChipVariant { Default, Selected }

object IPayChipDefaults {
    /** Pill content padding — semantic h=s12 / v=s6. Resolved at composition via [IPayTheme]. */
    @Composable
    fun contentPadding(): PaddingValues = PaddingValues(
        horizontal = IPayTheme.spacing.s12,
        vertical = IPayTheme.spacing.s6,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun IPayChipDefaultPreview() {
    IPayTheme {
        IPayChip(text = "All", onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun IPayChipSelectedPreview() {
    IPayTheme {
        IPayChip(
            text = "Selected",
            onClick = {},
            variant = IPayChipVariant.Selected,
            leadingIcon = Icons.Default.Star,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IPayAIChipPreview() {
    IPayTheme {
        IPayAIChip(text = "AI Suggest")
    }
}
