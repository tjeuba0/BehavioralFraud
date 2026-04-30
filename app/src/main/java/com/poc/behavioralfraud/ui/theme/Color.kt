package com.poc.behavioralfraud.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Layer 1 — Palette (primitives).
 *
 * Hard-coded color values from Figma `5JXePCuiqKQFdrjNHNQVbw` node `1:15393`.
 * NEVER consume directly in UI — wire through [IPayColors] semantic tokens instead.
 */
@Suppress("MagicNumber")
internal object IPayPalette {
    // Brand: VietinBank Dark Blue (10..95)
    val VietinDarkBlue10 = Color(0xFFDEF1FF)
    val VietinDarkBlue20 = Color(0xFFB6E4FF)
    val VietinDarkBlue30 = Color(0xFF76CFFF)
    val VietinDarkBlue50 = Color(0xFF029FF5)
    val VietinDarkBlue60 = Color(0xFF007DD2)
    val VietinDarkBlue70 = Color(0xFF005BAA) // anchor: textBrandPrimary
    val VietinDarkBlue80 = Color(0xFF005993)
    val VietinDarkBlue90 = Color(0xFF074673)
    val VietinDarkBlue95 = Color(0xFF042D4D)

    // Brand: VietinBank Red (40..80)
    val VietinRed40 = Color(0xFFFF6A89)
    val VietinRed50 = Color(0xFFFD3664)
    val VietinRed60 = Color(0xFFD71249)
    val VietinRed80 = Color(0xFFA60B3F)

    // Neutral: Ink (slate grayscale, 5..95)
    val Ink5 = Color(0xFFF8FAFC)
    val Ink10 = Color(0xFFF1F5F9)
    val Ink20 = Color(0xFFE2E8F0)
    val Ink30 = Color(0xFFCAD5E2)
    val Ink40 = Color(0xFF90A1B9)
    val Ink50 = Color(0xFF62748E)
    val Ink60 = Color(0xFF45556C)
    val Ink70 = Color(0xFF314158)
    val Ink90 = Color(0xFF0F172B)
    val Ink95 = Color(0xFF020618)

    // Text/icon primary anchor (slightly off Ink95, matches Figma var `text/textNeutralPrimary`)
    val InkPrimary = Color(0xFF061229)

    // Neutral: white + opacities + black overlay
    val White = Color(0xFFFFFFFF)
    val White80 = Color(0xCCFFFFFF)
    val OverlayBlack60 = Color(0x99000000)

    // Brand bg accents
    val BrandBgLight = Color(0xFFEFF8FF)

    // Semantic: Green
    val Green5 = Color(0xFFECFCF1)
    val Green10 = Color(0xFFD8FDE6)
    val Green30 = Color(0xFF82F3AD)
    val Green50 = Color(0xFF1ACB5E)
    val Green70 = Color(0xFF11843E)
    val Green90 = Color(0xFF104C29)
    val GreenIcon = Color(0xFF11A84B)

    // Semantic: Orange (warning)
    val Orange5 = Color(0xFFFFF7ED)
    val Orange10 = Color(0xFFFFEDD4)
    val Orange30 = Color(0xFFFFB86A)
    val Orange50 = Color(0xFFFF6900)
    val Orange60 = Color(0xFFF54900) // iconWarning
    val Orange70 = Color(0xFFCA3500)
    val Orange80 = Color(0xFF9F2D00)

    // Semantic: Blue (info)
    val Blue60 = Color(0xFF2E90FA) // iconInfo
    val BlueAlertBorder = Color(0xFFB2DDFF)

    // AI chip gradient stops
    val AICyan = Color(0xFF76CFFF)       // AI border start
    val AIPurple = Color(0xFF5F59FB)     // AI border middle1
    val AIPink = Color(0xFFF286FF)       // AI border middle2
    val AISalmon = Color(0xFFFFA0B2)     // AI border middle3 + notification end
    val AIWhite50 = Color(0x80FFFFFF)    // AI border end
    val AILabelStart = Color(0xFF005BAA) // matches VietinDarkBlue70
    val AILabelMiddle = Color(0xFF797DFF)
    val AILabelEnd = Color(0xFFFD3664)   // matches VietinRed50

    // Purple heart (decorative)
    val Purple10 = Color(0xFFDEE4FF)
    val Purple40 = Color(0xFF797DFF)
    val Purple60 = Color(0xFF523DF0)

    // Misc
    val ChipLabelInk = Color(0xFF020618)        // chip label (near Ink95)
    val InputBgDisabled = Color(0x80E2E8F0)     // 50% Ink20
    val ShadowInk10 = Color(0x1A020618)         // ink/95_10 — shadow base
}

/**
 * Layer 2 — Semantic colors.
 *
 * Names mirror Figma var paths (e.g. `text/textBrandPrimary` → `textBrandPrimary`).
 * Components consume these via `IPayTheme.colors.*`.
 *
 * Immutable data class so swapping themes = swapping reference, no field mutation.
 */
@Immutable
data class IPayColors(
    // Text
    val textNeutralPrimary: Color,
    val textNeutralSecondary: Color,
    val textNeutralTertiary: Color,
    val textBrandPrimary: Color,
    val textOnColorPrimary: Color,
    val textSuccess: Color,

    // Icon
    val iconNeutralPrimary: Color,
    val iconNeutralSecondary: Color,
    val iconNeutralTertiary: Color,
    val iconBrandPrimary: Color,
    val iconOnColorPrimary: Color,
    val iconSuccess: Color,
    val iconInfo: Color,
    val iconWarning: Color,

    // Background
    val bgNeutralPrimary: Color,
    val bgNeutralSecondary: Color,
    val bgBrandSecondary: Color,
    val bgBrandTertiary: Color,
    val bgOverlay: Color,

    // Border
    val borderBrandPrimary: Color,
    val borderBrandSecondary: Color,
    val borderNeutralPrimary: Color,
    val borderNeutralSecondary: Color,
    val borderFocus: Color,
    val borderInfo: Color,
    val dividerPrimary: Color,

    // Button — Primary (gradient)
    val buttonPrimaryBgStart: Color,
    val buttonPrimaryBgEnd: Color,
    val buttonPrimaryLabel: Color,

    // Button — Secondary
    val buttonSecondaryBg: Color,
    val buttonSecondaryBorder: Color,
    val buttonSecondaryLabel: Color,
    val buttonSecondaryIcon: Color,

    // Button — Ghost / Tertiary
    val buttonGhostLabel: Color,
    val buttonGhostIcon: Color,
    val buttonTertiaryLabel: Color,

    // Input
    val inputBgPrimaryDefault: Color,
    val inputBgSecondaryDefault: Color,
    val inputBgDisabled: Color,
    val inputBorderDefault: Color,
    val inputBorderActive: Color,
    val inputBorderDisabled: Color,
    val inputLabelDefault: Color,
    val inputLabelActive: Color,
    val inputTextDefault: Color,
    val inputTextDisabled: Color,
    val inputTextPlaceholder: Color,
    val inputCaret: Color,
    val inputIconDefault: Color,
    val focusRing: Color,

    // Chip — default
    val chipBg: Color,
    val chipBorder: Color,
    val chipLabel: Color,
    val chipIcon: Color,

    // Chip — AI (gradients)
    val aiChipBg: Color,
    val aiChipBorderStart: Color,
    val aiChipBorderMiddle1: Color,
    val aiChipBorderMiddle2: Color,
    val aiChipBorderMiddle3: Color,
    val aiChipBorderEnd: Color,
    val aiChipLabelStart: Color,
    val aiChipLabelMiddle: Color,
    val aiChipLabelEnd: Color,

    // Tabs
    val tabLabelDefault: Color,
    val tabLabelActive: Color,
    val tabIndicatorDefault: Color,
    val tabIndicatorActive: Color,

    // Toggle
    val toggleHandleDefault: Color,
    val toggleBgDefaultStart: Color,
    val toggleBgDefaultEnd: Color,

    // Notification badge
    val notificationBgStart: Color,
    val notificationBgEnd: Color,
    val notificationOutsideBorder: Color,
    val notificationLabel: Color,

    // Highlight badge (action — gradient)
    val highlightActionBgStart: Color,
    val highlightActionBgEnd: Color,
    val highlightActionLabel: Color,
    val highlightActionIcon: Color,

    // Alert banner — Info
    val alertInfoBg: Color,
    val alertInfoBorder: Color,
    val alertInfoIcon: Color,
    val alertInfoLabel: Color,

    // Shadow base color (consumed by IPayElevation)
    val shadowColor: Color,
)

/**
 * Default semantic colors — light mode, full iPay (VietinBank) palette.
 * Reference: Figma `1:15393` variables.
 */
internal fun defaultIPayColors(): IPayColors = IPayColors(
    textNeutralPrimary = IPayPalette.InkPrimary,
    textNeutralSecondary = IPayPalette.Ink70,
    textNeutralTertiary = IPayPalette.Ink50,
    textBrandPrimary = IPayPalette.VietinDarkBlue70,
    textOnColorPrimary = IPayPalette.White,
    textSuccess = IPayPalette.Green70,

    iconNeutralPrimary = IPayPalette.InkPrimary,
    iconNeutralSecondary = IPayPalette.Ink50,
    iconNeutralTertiary = IPayPalette.Ink40,
    iconBrandPrimary = IPayPalette.VietinDarkBlue70,
    iconOnColorPrimary = IPayPalette.White,
    iconSuccess = IPayPalette.GreenIcon,
    iconInfo = IPayPalette.Blue60,
    iconWarning = IPayPalette.Orange60,

    bgNeutralPrimary = IPayPalette.White,
    bgNeutralSecondary = IPayPalette.Ink5,
    bgBrandSecondary = IPayPalette.BrandBgLight,
    bgBrandTertiary = IPayPalette.BrandBgLight,
    bgOverlay = IPayPalette.OverlayBlack60,

    borderBrandPrimary = IPayPalette.VietinDarkBlue70,
    borderBrandSecondary = IPayPalette.VietinDarkBlue20,
    borderNeutralPrimary = IPayPalette.Ink20,
    borderNeutralSecondary = IPayPalette.Ink30,
    borderFocus = IPayPalette.VietinDarkBlue60,
    borderInfo = IPayPalette.BlueAlertBorder,
    dividerPrimary = IPayPalette.Ink20,

    buttonPrimaryBgStart = IPayPalette.VietinDarkBlue80,
    buttonPrimaryBgEnd = IPayPalette.VietinDarkBlue60,
    buttonPrimaryLabel = IPayPalette.White,

    buttonSecondaryBg = IPayPalette.White,
    buttonSecondaryBorder = IPayPalette.VietinDarkBlue20,
    buttonSecondaryLabel = IPayPalette.VietinDarkBlue70,
    buttonSecondaryIcon = IPayPalette.VietinDarkBlue70,

    buttonGhostLabel = IPayPalette.VietinDarkBlue70,
    buttonGhostIcon = IPayPalette.VietinDarkBlue70,
    buttonTertiaryLabel = IPayPalette.VietinDarkBlue70,

    inputBgPrimaryDefault = IPayPalette.White,
    inputBgSecondaryDefault = IPayPalette.Ink5,
    inputBgDisabled = IPayPalette.InputBgDisabled,
    inputBorderDefault = IPayPalette.Ink30,
    inputBorderActive = IPayPalette.VietinDarkBlue60,
    inputBorderDisabled = IPayPalette.Ink20,
    inputLabelDefault = IPayPalette.Ink50,
    inputLabelActive = IPayPalette.VietinDarkBlue70,
    inputTextDefault = IPayPalette.InkPrimary,
    inputTextDisabled = IPayPalette.Ink70,
    inputTextPlaceholder = IPayPalette.Ink30,
    inputCaret = IPayPalette.VietinDarkBlue70,
    inputIconDefault = IPayPalette.InkPrimary,
    focusRing = IPayPalette.VietinDarkBlue10,

    chipBg = IPayPalette.White,
    chipBorder = IPayPalette.Ink30,
    chipLabel = IPayPalette.ChipLabelInk,
    chipIcon = IPayPalette.InkPrimary,

    aiChipBg = IPayPalette.White,
    aiChipBorderStart = IPayPalette.AICyan,
    aiChipBorderMiddle1 = IPayPalette.AIPurple,
    aiChipBorderMiddle2 = IPayPalette.AIPink,
    aiChipBorderMiddle3 = IPayPalette.AISalmon,
    aiChipBorderEnd = IPayPalette.AIWhite50,
    aiChipLabelStart = IPayPalette.AILabelStart,
    aiChipLabelMiddle = IPayPalette.AILabelMiddle,
    aiChipLabelEnd = IPayPalette.AILabelEnd,

    tabLabelDefault = IPayPalette.Ink70,
    tabLabelActive = IPayPalette.VietinDarkBlue70,
    tabIndicatorDefault = IPayPalette.Ink20,
    tabIndicatorActive = IPayPalette.VietinRed60,

    toggleHandleDefault = IPayPalette.White,
    toggleBgDefaultStart = IPayPalette.Ink30,
    toggleBgDefaultEnd = IPayPalette.Ink30,

    notificationBgStart = IPayPalette.VietinRed60,
    notificationBgEnd = IPayPalette.AISalmon,
    notificationOutsideBorder = IPayPalette.White,
    notificationLabel = IPayPalette.White,

    highlightActionBgStart = IPayPalette.VietinDarkBlue80,
    highlightActionBgEnd = IPayPalette.VietinDarkBlue60,
    highlightActionLabel = IPayPalette.White,
    highlightActionIcon = IPayPalette.White,

    alertInfoBg = IPayPalette.BrandBgLight,
    alertInfoBorder = IPayPalette.BlueAlertBorder,
    alertInfoIcon = IPayPalette.Blue60,
    alertInfoLabel = IPayPalette.ChipLabelInk,

    shadowColor = IPayPalette.ShadowInk10,
)

/**
 * Demo theme colors — alternate brand (red as primary).
 *
 * Proves theme swap works without component code changes — overrides only the
 * brand-tinted tokens. Used in `IPayThemes.Demo`.
 */
internal fun demoIPayColors(): IPayColors = defaultIPayColors().copy(
    textBrandPrimary = IPayPalette.VietinRed60,
    iconBrandPrimary = IPayPalette.VietinRed60,
    borderBrandPrimary = IPayPalette.VietinRed60,
    borderBrandSecondary = IPayPalette.VietinRed40,

    buttonPrimaryBgStart = IPayPalette.VietinRed60,
    buttonPrimaryBgEnd = IPayPalette.VietinRed50,
    buttonSecondaryBorder = IPayPalette.VietinRed40,
    buttonSecondaryLabel = IPayPalette.VietinRed60,
    buttonSecondaryIcon = IPayPalette.VietinRed60,
    buttonGhostLabel = IPayPalette.VietinRed60,
    buttonGhostIcon = IPayPalette.VietinRed60,
    buttonTertiaryLabel = IPayPalette.VietinRed60,

    inputBorderActive = IPayPalette.VietinRed60,
    inputLabelActive = IPayPalette.VietinRed60,
    inputCaret = IPayPalette.VietinRed60,

    tabLabelActive = IPayPalette.VietinRed60,

    highlightActionBgStart = IPayPalette.VietinRed60,
    highlightActionBgEnd = IPayPalette.VietinRed50,
)
