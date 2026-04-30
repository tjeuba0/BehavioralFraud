package com.poc.behavioralfraud.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poc.behavioralfraud.ui.theme.IPayTheme

/**
 * Card container — FR-CL-08 REQ-13.
 *
 * Variants:
 *  - [IPayCardVariant.Plain]    = bg `bgNeutralPrimary`, no border, no shadow
 *  - [IPayCardVariant.Elevated] = bg `bgNeutralPrimary` + `IPayElevation.large` shadow
 *  - [IPayCardVariant.Outlined] = bg `bgNeutralPrimary` + 1.5dp `borderNeutralPrimary` border
 *
 * Default radius `r16` (`IPayShapes.small`). Slot-based content.
 *
 * Tap support: pass [onClick] to make the card a tappable surface (uses
 * [safeClickable] 350ms debounce). Without [onClick] the card is purely visual.
 */
@Composable
fun IPayCard(
    modifier: Modifier = Modifier,
    variant: IPayCardVariant = IPayCardVariant.Plain,
    shape: CornerBasedShape = IPayTheme.shapes.small,
    contentPadding: PaddingValues = PaddingValues(IPayTheme.spacing.s16),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = IPayTheme.colors
    val elevation = IPayTheme.elevation.large

    var base: Modifier = modifier
    if (variant == IPayCardVariant.Elevated) {
        base = base.shadow(
            elevation = elevation.blurRadius,
            shape = shape,
            ambientColor = colors.shadowColor,
            spotColor = colors.shadowColor,
        )
    }

    base = base
        .clip(shape)
        .background(colors.bgNeutralPrimary)

    if (variant == IPayCardVariant.Outlined) {
        base = base.border(
            width = IPayTheme.stroke.s,
            brush = SolidColor(colors.borderNeutralPrimary),
            shape = shape,
        )
    }

    if (onClick != null) {
        base = base.safeClickable(role = Role.Button, onSafeClick = onClick)
    }

    Box(modifier = base.padding(contentPadding)) {
        content()
    }
}

enum class IPayCardVariant { Plain, Elevated, Outlined }

object IPayCardDefaults {
    val DEFAULT_PADDING: Dp = 16.dp
    val DEFAULT_BORDER_COLOR: Color = Color.Unspecified // resolved at compose time via IPayTheme
}
