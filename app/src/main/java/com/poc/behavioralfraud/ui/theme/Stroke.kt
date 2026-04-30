package com.poc.behavioralfraud.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Stroke (border width) tokens — Figma `Stroke/xs|s|md|lg|xl`.
 *
 * Components consume via `IPayTheme.stroke.*`. Values are in `Dp`.
 */
@Immutable
data class IPayStroke(
    val xs: Dp,
    val s: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
)

internal fun defaultIPayStroke(): IPayStroke = IPayStroke(
    xs = 1.dp,
    s = 1.5.dp,
    md = 1.75.dp,
    lg = 2.dp,
    xl = 4.dp,
)
