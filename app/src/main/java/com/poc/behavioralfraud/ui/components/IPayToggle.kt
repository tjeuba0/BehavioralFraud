package com.poc.behavioralfraud.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.poc.behavioralfraud.ui.theme.IPayTheme

/**
 * iOS-style switch — FR-CL-08 REQ-18.
 *
 * Track 32×20dp (`spacing.s32 × spacing.s20`), handle 16×16dp (`spacing.s16`).
 *  - Off: track gradient `toggleBgDefaultStart → toggleBgDefaultEnd`
 *  - On:  track gradient `buttonPrimaryBgStart → buttonPrimaryBgEnd`
 *  - Handle: `toggleHandleDefault` (white)
 *
 * Animates handle position (200ms tween) when [checked] flips. When [enabled] is
 * false the entire control renders at 0.4 alpha and click handler is no-op.
 *
 * Travel distance = trackWidth(32) − handleSize(16) − 2×padding(2) = 12dp = `s12`.
 *
 * Always uses [safeClickable] (350ms debounce).
 */
@Composable
fun IPayToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = IPayTheme.colors
    val spacing = IPayTheme.spacing

    val trackGradient: Brush = if (checked) {
        Brush.horizontalGradient(
            listOf(colors.buttonPrimaryBgStart, colors.buttonPrimaryBgEnd),
        )
    } else {
        Brush.horizontalGradient(
            listOf(colors.toggleBgDefaultStart, colors.toggleBgDefaultEnd),
        )
    }

    val targetOffset: Dp = if (checked) spacing.s12 else spacing.s0
    val handleOffset: Dp by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = tween(durationMillis = IPayToggleConstants.ANIMATION_MS),
        label = "IPayToggleHandleOffset",
    )

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else IPayToggleConstants.DISABLED_ALPHA)
            .width(spacing.s32)
            .height(spacing.s20)
            .clip(IPayTheme.shapes.full)
            .background(trackGradient)
            .safeClickable(
                enabled = enabled,
                role = Role.Switch,
                onSafeClick = { onCheckedChange(!checked) },
            )
            .padding(spacing.s2),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = handleOffset)
                .size(spacing.s16)
                .clip(IPayTheme.shapes.full)
                .background(colors.toggleHandleDefault),
        )
    }
}

private object IPayToggleConstants {
    const val ANIMATION_MS: Int = 200
    const val DISABLED_ALPHA: Float = 0.4f
}

@Preview(name = "Toggle — Off", showBackground = true)
@Composable
private fun IPayTogglePreviewOff() {
    IPayTheme {
        Row(modifier = Modifier.padding(IPayTheme.spacing.s16)) {
            IPayToggle(checked = false, onCheckedChange = {})
        }
    }
}

@Preview(name = "Toggle — On", showBackground = true)
@Composable
private fun IPayTogglePreviewOn() {
    IPayTheme {
        Row(modifier = Modifier.padding(IPayTheme.spacing.s16)) {
            IPayToggle(checked = true, onCheckedChange = {})
        }
    }
}

@Preview(name = "Toggle — Disabled", showBackground = true)
@Composable
private fun IPayTogglePreviewDisabled() {
    IPayTheme {
        Row(
            modifier = Modifier.padding(IPayTheme.spacing.s16),
            horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IPayToggle(checked = false, onCheckedChange = {}, enabled = false)
            Spacer(Modifier.width(IPayTheme.spacing.s8))
            IPayToggle(checked = true, onCheckedChange = {}, enabled = false)
        }
    }
}
