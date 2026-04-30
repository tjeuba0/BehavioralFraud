package com.poc.behavioralfraud.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Layer 3a — Theme spec.
 *
 * Immutable bundle of all 6 token types. "1 theme = 1 [IPayThemeSpec] instance".
 * Swap themes by swapping spec reference (no field mutation).
 */
@Immutable
data class IPayThemeSpec(
    val colors: IPayColors,
    val typography: IPayTypography,
    val spacing: IPaySpacing,
    val shapes: IPayShapes,
    val stroke: IPayStroke,
    val elevation: IPayElevation,
)

/**
 * Layer 3b — Theme registry.
 *
 * Adding a new theme = thêm 1 entry vào object này. ZERO thay đổi component code.
 *
 * Variants:
 *  - [Default] — full iPay light, Figma `1:15393` reference
 *  - [Dark]    — POC stub (mirror Default colors). Khi cần ship dark thật:
 *                viết `darkIPayColors()` factory rồi `Default.copy(colors = ...)`.
 *  - [Demo]    — alternative palette (red brand) — proof of switchability
 */
object IPayThemes {
    val Default: IPayThemeSpec = IPayThemeSpec(
        colors = defaultIPayColors(),
        typography = defaultIPayTypography(),
        spacing = defaultIPaySpacing(),
        shapes = defaultIPayShapes(),
        stroke = defaultIPayStroke(),
        elevation = defaultIPayElevation(),
    )

    /** Dark mode stub — currently mirrors Default. Replace `colors` when shipping. */
    val Dark: IPayThemeSpec = Default

    /** Demo theme — red brand override; proves swap works without component changes. */
    val Demo: IPayThemeSpec = Default.copy(
        colors = demoIPayColors(),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// CompositionLocal — Layer 3 plumbing.
// Components consume tokens via `IPayTheme.*` (object accessor), which reads
// these locals. Provider [IPayTheme] swaps locals when spec changes.
// ─────────────────────────────────────────────────────────────────────────────

private val LocalIPayColors = staticCompositionLocalOf<IPayColors> {
    error("IPayColors not provided — wrap content in IPayTheme { ... }")
}
private val LocalIPayTypography = staticCompositionLocalOf<IPayTypography> {
    error("IPayTypography not provided — wrap content in IPayTheme { ... }")
}
private val LocalIPaySpacing = staticCompositionLocalOf<IPaySpacing> {
    error("IPaySpacing not provided — wrap content in IPayTheme { ... }")
}
private val LocalIPayShapes = staticCompositionLocalOf<IPayShapes> {
    error("IPayShapes not provided — wrap content in IPayTheme { ... }")
}
private val LocalIPayStroke = staticCompositionLocalOf<IPayStroke> {
    error("IPayStroke not provided — wrap content in IPayTheme { ... }")
}
private val LocalIPayElevation = staticCompositionLocalOf<IPayElevation> {
    error("IPayElevation not provided — wrap content in IPayTheme { ... }")
}

/**
 * Theme provider. Wrap app root (or any subtree for nested override).
 *
 * Usage:
 * ```
 * IPayTheme { /* uses Default */ }
 * IPayTheme(spec = IPayThemes.Demo) { /* uses Demo for this subtree */ }
 * ```
 *
 * Internally:
 *  - Provides 6 [staticCompositionLocalOf] from [spec] for [IPayTheme] accessor.
 *  - Maps spec to [MaterialTheme] colorScheme/typography/shapes so Material3
 *    widgets (Snackbar, AlertDialog, ModalBottomSheet…) auto-pick brand colors.
 */
@Composable
fun IPayTheme(
    spec: IPayThemeSpec = IPayThemes.Default,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalIPayColors provides spec.colors,
        LocalIPayTypography provides spec.typography,
        LocalIPaySpacing provides spec.spacing,
        LocalIPayShapes provides spec.shapes,
        LocalIPayStroke provides spec.stroke,
        LocalIPayElevation provides spec.elevation,
    ) {
        MaterialTheme(
            colorScheme = spec.toMaterialColorScheme(),
            typography = spec.toMaterialTypography(),
            shapes = spec.toMaterialShapes(),
            content = content,
        )
    }
}

/**
 * Accessor object — components read tokens via `IPayTheme.colors.brandPrimary` etc.
 * Each property reads the [staticCompositionLocalOf]; when provider's [spec]
 * changes, all consumers recompose with new values.
 */
object IPayTheme {
    val colors: IPayColors
        @Composable @ReadOnlyComposable get() = LocalIPayColors.current
    val typography: IPayTypography
        @Composable @ReadOnlyComposable get() = LocalIPayTypography.current
    val spacing: IPaySpacing
        @Composable @ReadOnlyComposable get() = LocalIPaySpacing.current
    val shapes: IPayShapes
        @Composable @ReadOnlyComposable get() = LocalIPayShapes.current
    val stroke: IPayStroke
        @Composable @ReadOnlyComposable get() = LocalIPayStroke.current
    val elevation: IPayElevation
        @Composable @ReadOnlyComposable get() = LocalIPayElevation.current
}

// ─────────────────────────────────────────────────────────────────────────────
// Material3 mapping — keeps Material widgets in sync with IPay tokens.
// ─────────────────────────────────────────────────────────────────────────────

private fun IPayThemeSpec.toMaterialColorScheme() = lightColorScheme(
    primary = colors.textBrandPrimary,
    onPrimary = colors.textOnColorPrimary,
    primaryContainer = colors.bgBrandSecondary,
    onPrimaryContainer = colors.textBrandPrimary,
    secondary = colors.tabIndicatorActive,
    onSecondary = colors.textOnColorPrimary,
    background = colors.bgNeutralPrimary,
    onBackground = colors.textNeutralPrimary,
    surface = colors.bgNeutralPrimary,
    onSurface = colors.textNeutralPrimary,
    surfaceVariant = colors.bgNeutralSecondary,
    onSurfaceVariant = colors.textNeutralSecondary,
    outline = colors.borderNeutralPrimary,
    error = colors.iconWarning,
    onError = colors.textOnColorPrimary,
)

private fun IPayThemeSpec.toMaterialTypography() = Typography(
    displayLarge = typography.headingLarge,
    headlineLarge = typography.headingSmall,
    headlineMedium = typography.headingExtraSmall,
    headlineSmall = typography.titleLarge,
    titleLarge = typography.titleLarge,
    titleMedium = typography.titleMedium,
    titleSmall = typography.titleSmall,
    bodyLarge = typography.bodyLarge,
    bodyMedium = typography.bodyMedium,
    bodySmall = typography.bodySmall,
    labelLarge = typography.labelLarge,
    labelMedium = typography.labelMedium,
    labelSmall = typography.labelSmall,
)

private fun IPayThemeSpec.toMaterialShapes() = Shapes(
    extraSmall = shapes.xsmall,
    small = shapes.small,
    medium = shapes.medium,
    large = shapes.large,
    extraLarge = shapes.large,
)

// ─────────────────────────────────────────────────────────────────────────────
// Backwards-compat alias.
//
// Existing call sites (MainActivity) use `BehavioralFraudTheme { … }`. Keep alias
// so this task doesn't break the build before MainActivity migrates to NavHost
// (TASK-015).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BehavioralFraudTheme(content: @Composable () -> Unit) {
    IPayTheme(spec = IPayThemes.Default, content = content)
}
