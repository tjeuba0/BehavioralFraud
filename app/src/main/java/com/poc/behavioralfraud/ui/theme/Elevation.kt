package com.poc.behavioralfraud.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Elevation tokens — Figma `dropShadow/Small` + `Shadow/Large`.
 *
 * `IPayElevationSpec` describes shadow geometry — components paint shadow
 * manually using `IPayTheme.colors.shadowColor` (no Material elevation API).
 */
@Immutable
data class IPayElevationSpec(
    val offsetX: Dp,
    val offsetY: Dp,
    val blurRadius: Dp,
    val spreadRadius: Dp,
)

@Immutable
data class IPayElevation(
    val small: IPayElevationSpec,
    val large: IPayElevationSpec,
)

internal fun defaultIPayElevation(): IPayElevation = IPayElevation(
    small = IPayElevationSpec(
        offsetX = 0.dp,
        offsetY = (-2).dp,
        blurRadius = 12.dp,
        spreadRadius = 0.dp,
    ),
    large = IPayElevationSpec(
        offsetX = 0.dp,
        offsetY = 4.dp,
        blurRadius = 16.dp,
        spreadRadius = 0.dp,
    ),
)
