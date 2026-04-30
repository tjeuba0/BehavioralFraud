package com.poc.behavioralfraud.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography tokens — Figma `Heading|Title|Body|Label` styles.
 *
 * Components consume via `IPayTheme.typography.*`.
 *
 * Reference: Figma `Brand/Typeface = SVN-Gilroy`. POC fallback to
 * [FontFamily.SansSerif] — swap by replacing `iPayFontFamily` value when
 * `res/font/svn_gilroy_*.ttf` assets are added.
 *
 * Scale (size/lineHeight in sp):
 *   T10/14, T12/18, T14/20, T16/24, T18/26, T20/26, T30/40
 *
 * Weights: 500 = Medium, 600 = SemiBold.
 */

/**
 * Brand typeface — current fallback to [FontFamily.SansSerif].
 *
 * To swap to real SVN-Gilroy:
 *   1. Drop `svn_gilroy_medium.ttf` and `svn_gilroy_semibold.ttf` into
 *      `app/src/main/res/font/`.
 *   2. Replace this constant with `FontFamily(Font(R.font.svn_gilroy_medium, FontWeight.Medium), ...)`.
 *   3. No other code change required — all text styles consume this single ref.
 */
internal val iPayFontFamily: FontFamily = FontFamily.SansSerif

@Immutable
data class IPayTypography(
    val headingLarge: TextStyle,
    val headingSmall: TextStyle,
    val headingExtraSmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val bodyEmphasizedXL: TextStyle,
    val bodyEmphasizedLarge: TextStyle,
    val bodyEmphasizedMedium: TextStyle,
    val bodyEmphasizedSmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
    val labelXS: TextStyle,
)

internal fun defaultIPayTypography(family: FontFamily = iPayFontFamily): IPayTypography {
    val medium = FontWeight.Medium      // 500
    val semiBold = FontWeight.SemiBold  // 600

    fun semi(size: Int, lineHeight: Int) = TextStyle(
        fontFamily = family,
        fontWeight = semiBold,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
    )

    fun med(size: Int, lineHeight: Int) = TextStyle(
        fontFamily = family,
        fontWeight = medium,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
    )

    return IPayTypography(
        // Heading — SemiBold
        headingLarge = semi(30, 40),
        headingSmall = semi(20, 26),
        headingExtraSmall = semi(16, 24),

        // Title — SemiBold
        titleLarge = semi(18, 26),
        titleMedium = semi(16, 24),
        titleSmall = semi(14, 20),

        // Body — Medium
        bodyLarge = med(16, 24),
        bodyMedium = med(14, 20),
        bodySmall = med(12, 18),

        // Body Emphasized — SemiBold
        bodyEmphasizedXL = semi(18, 26),
        bodyEmphasizedLarge = semi(16, 24),
        bodyEmphasizedMedium = semi(14, 20),
        bodyEmphasizedSmall = semi(12, 18),

        // Label — SemiBold
        labelLarge = semi(16, 24),
        labelMedium = semi(14, 20),
        labelSmall = semi(12, 18),
        labelXS = semi(10, 14),
    )
}
