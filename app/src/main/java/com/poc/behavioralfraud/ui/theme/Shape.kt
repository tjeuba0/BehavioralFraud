package com.poc.behavioralfraud.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

/**
 * Shape (corner radius) tokens — Figma `Corner Radius/rN` & `Radius/Small|Medium|Large|Full`.
 *
 * Components consume via `IPayTheme.shapes.*`. Type is [CornerBasedShape] so
 * Material3 [androidx.compose.material3.Shapes] can consume directly.
 *
 * Scale: none(0) / r4(4) / xsmall(8) / small(16) / medium(20) / large(24) / full(50%)
 */
@Immutable
data class IPayShapes(
    val none: CornerBasedShape,
    val r4: CornerBasedShape,
    val xsmall: CornerBasedShape,
    val small: CornerBasedShape,
    val medium: CornerBasedShape,
    val large: CornerBasedShape,
    val full: CornerBasedShape,
)

internal fun defaultIPayShapes(): IPayShapes = IPayShapes(
    none = RoundedCornerShape(0.dp),
    r4 = RoundedCornerShape(4.dp),
    xsmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    full = RoundedCornerShape(50), // percent — pill / round
)
