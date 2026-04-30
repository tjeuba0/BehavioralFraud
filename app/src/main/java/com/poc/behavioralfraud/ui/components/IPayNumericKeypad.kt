package com.poc.behavioralfraud.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poc.behavioralfraud.ui.theme.IPayTheme

/**
 * Numeric keypad — FR-CL-09 REQ-03.
 *
 * 3×4 grid layout:
 * ```
 * 1   2   3
 * 4   5   6
 * 7   8   9
 * BIO 0   ⌫
 * ```
 *
 * Each button is a 64dp round tap target with [safeClickable] (350ms debounce).
 * Background transparent — parent provides bg.
 *
 * @param onDigitTap     receives '0'..'9'
 * @param onBackspaceTap invoked on backspace tap
 * @param onBiometricTap null hides the biometric (left-bottom) slot, leaving
 *                       only an empty placeholder so the grid stays 3×4. Pass
 *                       a lambda only when device supports biometric.
 * @param enabled        applies to ALL keys; pass false to gate during loading.
 */
@Composable
fun IPayNumericKeypad(
    onDigitTap: (digit: Char) -> Unit,
    onBackspaceTap: () -> Unit,
    modifier: Modifier = Modifier,
    onBiometricTap: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s12),
    ) {
        KeypadRow {
            DigitKey('1', onDigitTap, enabled)
            DigitKey('2', onDigitTap, enabled)
            DigitKey('3', onDigitTap, enabled)
        }
        KeypadRow {
            DigitKey('4', onDigitTap, enabled)
            DigitKey('5', onDigitTap, enabled)
            DigitKey('6', onDigitTap, enabled)
        }
        KeypadRow {
            DigitKey('7', onDigitTap, enabled)
            DigitKey('8', onDigitTap, enabled)
            DigitKey('9', onDigitTap, enabled)
        }
        KeypadRow {
            if (onBiometricTap != null) {
                IconKey(
                    icon = Icons.Default.Fingerprint,
                    contentDescription = "Touch ID",
                    onClick = onBiometricTap,
                    enabled = enabled,
                )
            } else {
                EmptySlot()
            }
            DigitKey('0', onDigitTap, enabled)
            IconKey(
                icon = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                onClick = onBackspaceTap,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun KeypadRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            IPayTheme.spacing.s12,
            Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
private fun DigitKey(
    digit: Char,
    onDigitTap: (Char) -> Unit,
    enabled: Boolean,
) {
    KeyButton(
        onClick = { onDigitTap(digit) },
        enabled = enabled,
        contentDescription = "Digit $digit",
    ) {
        Text(
            text = digit.toString(),
            style = IPayTheme.typography.headingSmall,
            color = IPayTheme.colors.textNeutralPrimary,
        )
    }
}

@Composable
private fun IconKey(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    KeyButton(
        onClick = onClick,
        enabled = enabled,
        contentDescription = contentDescription,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = IPayTheme.colors.textNeutralPrimary,
            modifier = Modifier.size(IPayNumericKeypadDefaults.ICON_SIZE),
        )
    }
}

@Composable
private fun KeyButton(
    onClick: () -> Unit,
    enabled: Boolean,
    @Suppress("UNUSED_PARAMETER") contentDescription: String,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(IPayNumericKeypadDefaults.KEY_SIZE)
            .clip(IPayTheme.shapes.full)
            .safeClickable(
                enabled = enabled,
                role = Role.Button,
                onSafeClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun EmptySlot() {
    Spacer(modifier = Modifier.size(IPayNumericKeypadDefaults.KEY_SIZE))
}

private object IPayNumericKeypadDefaults {
    val KEY_SIZE: Dp = 64.dp
    val ICON_SIZE: Dp = 24.dp
}
