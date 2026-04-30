package com.poc.behavioralfraud.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poc.behavioralfraud.ui.theme.IPayTheme

/**
 * PIN / OTP dots — FR-CL-09 REQ-02.
 *
 * Reusable for PIN entry (filled-circle mode) AND OTP entry (show-digit mode).
 *
 * Modes:
 *  - **Filled circle** (`digits == null`): each slot is a small dot; filled = brand,
 *    empty = neutral border. Subtle bounce on fill.
 *  - **Show digit**   (`digits != null`): each slot is an outlined box; filled
 *    slot shows its digit (used for OTP entry where user verifies typed number).
 *
 * @param length        total slots (6 for PIN, 6 for OTP)
 * @param enteredCount  how many slots are filled (0..length)
 * @param digits        null = filled-circle mode, non-null = show-digit mode
 *                      (substring used: `digits.take(enteredCount)`)
 */
@Composable
fun IPayPinDots(
    length: Int,
    enteredCount: Int,
    modifier: Modifier = Modifier,
    digits: String? = null,
) {
    val safeEntered = enteredCount.coerceIn(0, length)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(length) { index ->
            val filled = index < safeEntered
            if (digits == null) {
                FilledCircleSlot(filled = filled)
            } else {
                ShowDigitSlot(
                    filled = filled,
                    digit = digits.getOrNull(index)?.takeIf { filled },
                )
            }
        }
    }
}

@Composable
private fun FilledCircleSlot(filled: Boolean) {
    val baseSize: Dp = IPayPinDotsDefaults.DOT_SIZE
    val targetSize by animateDpAsState(
        targetValue = if (filled) baseSize else baseSize * IPayPinDotsDefaults.EMPTY_SCALE,
        animationSpec = spring(
            dampingRatio = IPayPinDotsDefaults.BOUNCE_DAMPING,
            stiffness = IPayPinDotsDefaults.BOUNCE_STIFFNESS,
        ),
        label = "pin_dot_size",
    )

    val fillColor = if (filled) {
        IPayTheme.colors.borderBrandPrimary
    } else {
        Color.Transparent
    }
    val borderColor = if (filled) {
        IPayTheme.colors.borderBrandPrimary
    } else {
        IPayTheme.colors.borderNeutralPrimary
    }

    Box(
        modifier = Modifier
            .size(baseSize)
            .clip(IPayTheme.shapes.full)
            .border(
                width = IPayTheme.stroke.s,
                brush = SolidColor(borderColor),
                shape = IPayTheme.shapes.full,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(targetSize)
                .clip(IPayTheme.shapes.full)
                .background(fillColor),
        )
    }
}

@Composable
private fun ShowDigitSlot(
    filled: Boolean,
    digit: Char?,
) {
    val borderColor = if (filled) {
        IPayTheme.colors.borderBrandPrimary
    } else {
        IPayTheme.colors.borderNeutralPrimary
    }
    Box(
        modifier = Modifier
            .size(IPayPinDotsDefaults.SLOT_SIZE)
            .clip(IPayTheme.shapes.small)
            .border(
                width = IPayTheme.stroke.s,
                brush = SolidColor(borderColor),
                shape = IPayTheme.shapes.small,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (digit != null) {
            Text(
                text = digit.toString(),
                style = IPayTheme.typography.titleLarge,
                color = IPayTheme.colors.textNeutralPrimary,
            )
        }
    }
}

private object IPayPinDotsDefaults {
    val DOT_SIZE: Dp = 16.dp
    val SLOT_SIZE: Dp = 48.dp
    const val EMPTY_SCALE: Float = 0.5f
    const val BOUNCE_DAMPING: Float = 0.5f
    const val BOUNCE_STIFFNESS: Float = 600f
}
