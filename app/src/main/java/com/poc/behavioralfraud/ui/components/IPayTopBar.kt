package com.poc.behavioralfraud.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poc.behavioralfraud.ui.theme.IPayTheme

/**
 * Top bar — FR-CL-08 REQ-12.
 *
 * Variants:
 *  - [IPayTopBarVariant.Standard]    = bg `bgNeutralPrimary`, primary text, default back icon
 *  - [IPayTopBarVariant.Transparent] = no fill (parent paints background, e.g. gradient header)
 *
 * Layout: [back] (left, optional) + [title] (center) + [trailing] slot (right).
 * Back uses [safeClickable] (350ms debounce).
 */
@Composable
fun IPayTopBar(
    title: String,
    modifier: Modifier = Modifier,
    variant: IPayTopBarVariant = IPayTopBarVariant.Standard,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val bg = when (variant) {
        IPayTopBarVariant.Standard -> IPayTheme.colors.bgNeutralPrimary
        IPayTopBarVariant.Transparent -> Color.Transparent
    }

    // Status-bar inset on the BG layer (so any fill paints behind the clock too)
    // and reapplied to the inner Row so back icon / title sit below the status bar.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(bg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(IPayTopBarDefaults.HEIGHT),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = IPayTheme.spacing.s8),
        ) {
            // Leading slot — back icon if onBack provided, else fixed-width spacer for symmetry.
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(IPayTopBarDefaults.ICON_SLOT)
                        .safeClickable(role = Role.Button, onSafeClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = IPayTheme.colors.iconNeutralPrimary,
                    )
                }
            } else {
                androidx.compose.foundation.layout.Spacer(Modifier.width(IPayTopBarDefaults.ICON_SLOT))
            }

            Text(
                text = title,
                style = IPayTheme.typography.titleLarge,
                color = IPayTheme.colors.textNeutralPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = IPayTheme.spacing.s8),
            )

            // Trailing slot — same fixed width as leading icon for symmetric centering.
            Box(
                modifier = Modifier.size(IPayTopBarDefaults.ICON_SLOT),
                contentAlignment = Alignment.Center,
            ) {
                trailing?.invoke()
            }
        }
    }
}

enum class IPayTopBarVariant { Standard, Transparent }

object IPayTopBarDefaults {
    val HEIGHT: Dp = 56.dp
    val ICON_SLOT: Dp = 40.dp
}
