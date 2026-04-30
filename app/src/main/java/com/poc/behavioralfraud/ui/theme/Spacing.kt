package com.poc.behavioralfraud.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing tokens — Figma `Spacing/sN` variables.
 *
 * Components consume via `IPayTheme.spacing.*`. Values are in `Dp`.
 *
 * Scale: s0/s1/s1_5/s2/s4/s6/s8/s10/s12/s16/s20/s24/s32/s40
 * (s1_5 = 1.5dp — Figma `Spacing/S15`)
 */
@Immutable
data class IPaySpacing(
    val s0: Dp,
    val s1: Dp,
    val s1_5: Dp,
    val s2: Dp,
    val s4: Dp,
    val s6: Dp,
    val s8: Dp,
    val s10: Dp,
    val s12: Dp,
    val s16: Dp,
    val s20: Dp,
    val s24: Dp,
    val s32: Dp,
    val s40: Dp,
)

internal fun defaultIPaySpacing(): IPaySpacing = IPaySpacing(
    s0 = 0.dp,
    s1 = 1.dp,
    s1_5 = 1.5.dp,
    s2 = 2.dp,
    s4 = 4.dp,
    s6 = 6.dp,
    s8 = 8.dp,
    s10 = 10.dp,
    s12 = 12.dp,
    s16 = 16.dp,
    s20 = 20.dp,
    s24 = 24.dp,
    s32 = 32.dp,
    s40 = 40.dp,
)
