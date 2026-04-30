package com.poc.behavioralfraud.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poc.behavioralfraud.ui.theme.IPayTheme

/**
 * Button — FR-CL-08 REQ-09.
 *
 * Variants × sizes:
 *   - [IPayButtonVariant.Primary]   = horizontal gradient (`buttonPrimaryBgStart → End`), label on color
 *   - [IPayButtonVariant.Secondary] = white fill + brand border + brand label
 *   - [IPayButtonVariant.Ghost]     = transparent fill + brand label, no border
 *   - [IPayButtonVariant.Tertiary]  = transparent fill + brand label, no border (used for inline actions)
 *
 *   - [IPayButtonSize.Large]   = h56dp, label `labelLarge`
 *   - [IPayButtonSize.Medium]  = h48dp, label `labelMedium`
 *   - [IPayButtonSize.Small]   = h36dp, label `labelSmall`
 *
 * Always uses [safeClickable] (350ms debounce) — prevents double-tap duplicate actions.
 */
@Composable
fun IPayButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: IPayButtonVariant = IPayButtonVariant.Primary,
    size: IPayButtonSize = IPayButtonSize.Large,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    val colors = variant.colors()
    val height = size.height
    val padding = size.contentPadding
    val labelStyle = size.labelStyle()
    val isInteractive = enabled && !loading

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = height)
            .height(height)
            .clip(IPayTheme.shapes.full)
            .background(colors.bg)
            .border(width = colors.borderWidth, brush = SolidColor(colors.border), shape = IPayTheme.shapes.full)
            .safeClickable(enabled = isInteractive, role = Role.Button, onSafeClick = onClick)
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = colors.label,
                strokeWidth = IPayTheme.stroke.s,
                modifier = Modifier.size(IPayTheme.spacing.s20),
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leadingIcon?.let {
                    Icon(it, contentDescription = null, tint = colors.label, modifier = Modifier.size(IPayTheme.spacing.s20))
                    Spacer(Modifier.width(IPayTheme.spacing.s8))
                }
                Text(
                    text = text,
                    style = labelStyle,
                    color = colors.label,
                    textAlign = TextAlign.Center,
                )
                trailingIcon?.let {
                    Spacer(Modifier.width(IPayTheme.spacing.s8))
                    Icon(it, contentDescription = null, tint = colors.label, modifier = Modifier.size(IPayTheme.spacing.s20))
                }
            }
        }
    }
}

/**
 * Round icon-only button — FR-CL-08 REQ-10. Default 40dp.
 *
 * Variants follow [IPayButton] color rules.
 */
@Composable
fun IPayIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    variant: IPayButtonVariant = IPayButtonVariant.Primary,
    size: Dp = IPayIconButtonDefaults.SIZE,
    enabled: Boolean = true,
) {
    val colors = variant.colors()
    Box(
        modifier = modifier
            .size(size)
            .clip(IPayTheme.shapes.full)
            .background(colors.bg)
            .border(width = colors.borderWidth, brush = SolidColor(colors.border), shape = IPayTheme.shapes.full)
            .safeClickable(enabled = enabled, role = Role.Button, onSafeClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.label,
            modifier = Modifier.size(IPayIconButtonDefaults.ICON_SIZE),
        )
    }
}

object IPayIconButtonDefaults {
    val SIZE: Dp = 40.dp
    val ICON_SIZE: Dp = 20.dp
}

enum class IPayButtonVariant { Primary, Secondary, Ghost, Tertiary }

enum class IPayButtonSize(val height: Dp, val contentPadding: PaddingValues) {
    Large(56.dp, PaddingValues(horizontal = 24.dp)),
    Medium(48.dp, PaddingValues(horizontal = 20.dp)),
    Small(36.dp, PaddingValues(horizontal = 16.dp)),
    ;

    @Composable
    fun labelStyle() = when (this) {
        Large -> IPayTheme.typography.labelLarge
        Medium -> IPayTheme.typography.labelMedium
        Small -> IPayTheme.typography.labelSmall
    }
}

/**
 * Resolved color quad for a button variant. Brush is used for the Primary
 * gradient; SolidColor for flat variants.
 */
private data class IPayButtonColors(
    val bg: Brush,
    val label: Color,
    val border: Color,
    val borderWidth: Dp,
)

@Composable
private fun IPayButtonVariant.colors(): IPayButtonColors = when (this) {
    IPayButtonVariant.Primary -> IPayButtonColors(
        bg = Brush.horizontalGradient(
            listOf(
                IPayTheme.colors.buttonPrimaryBgStart,
                IPayTheme.colors.buttonPrimaryBgEnd,
            ),
        ),
        label = IPayTheme.colors.buttonPrimaryLabel,
        border = Color.Transparent,
        borderWidth = 0.dp,
    )
    IPayButtonVariant.Secondary -> IPayButtonColors(
        bg = SolidColor(IPayTheme.colors.buttonSecondaryBg),
        label = IPayTheme.colors.buttonSecondaryLabel,
        border = IPayTheme.colors.buttonSecondaryBorder,
        borderWidth = IPayTheme.stroke.s,
    )
    IPayButtonVariant.Ghost -> IPayButtonColors(
        bg = SolidColor(Color.Transparent),
        label = IPayTheme.colors.buttonGhostLabel,
        border = Color.Transparent,
        borderWidth = 0.dp,
    )
    IPayButtonVariant.Tertiary -> IPayButtonColors(
        bg = SolidColor(Color.Transparent),
        label = IPayTheme.colors.buttonTertiaryLabel,
        border = Color.Transparent,
        borderWidth = 0.dp,
    )
}
