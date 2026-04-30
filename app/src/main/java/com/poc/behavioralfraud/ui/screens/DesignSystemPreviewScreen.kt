@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.poc.behavioralfraud.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import com.poc.behavioralfraud.ui.components.IPayAIChip
import com.poc.behavioralfraud.ui.components.IPayAlertBanner
import com.poc.behavioralfraud.ui.components.IPayAlertVariant
import com.poc.behavioralfraud.ui.components.IPayBottomSheet
import com.poc.behavioralfraud.ui.components.IPayButton
import com.poc.behavioralfraud.ui.components.IPayButtonSize
import com.poc.behavioralfraud.ui.components.IPayButtonVariant
import com.poc.behavioralfraud.ui.components.IPayCard
import com.poc.behavioralfraud.ui.components.IPayCardVariant
import com.poc.behavioralfraud.ui.components.IPayChip
import com.poc.behavioralfraud.ui.components.IPayChipVariant
import com.poc.behavioralfraud.ui.components.IPayHorizontalTabs
import com.poc.behavioralfraud.ui.components.IPayIconButton
import com.poc.behavioralfraud.ui.components.IPayNotificationBadge
import com.poc.behavioralfraud.ui.components.IPaySelection
import com.poc.behavioralfraud.ui.components.IPaySelectionVariant
import com.poc.behavioralfraud.ui.components.IPayStatusBadge
import com.poc.behavioralfraud.ui.components.IPayStatusBadgeVariant
import com.poc.behavioralfraud.ui.components.IPayTextField
import com.poc.behavioralfraud.ui.components.IPayToggle
import com.poc.behavioralfraud.ui.components.IPayTopBar
import com.poc.behavioralfraud.ui.theme.IPayTheme
import com.poc.behavioralfraud.ui.theme.IPayThemeSpec
import com.poc.behavioralfraud.ui.theme.IPayThemes
import kotlinx.coroutines.launch

/**
 * Design System Preview — FR-CL-08 REQ-23.
 *
 * Showcase tất cả 13 foundation components + design tokens cho dev/QA review.
 * Truy cập (POC) qua entry button trên HomeScreen; sau TASK-024 sẽ chuyển vào
 * Dev Menu (long-press logo Home 1.5s).
 *
 * Sections:
 *  1. Theme controls (cycle Default/Dark/Demo + nested override demo)
 *  2. Color palette (16 semantic swatches)
 *  3. Typography scale (16 text styles)
 *  4. Spacing scale (visual)
 *  5. Buttons (variants × sizes + icon button)
 *  6. TextField (states + slots)
 *  7. Cards (3 variants)
 *  8. Chips (default/selected/AI)
 *  9. Alert banners (info/warning/success)
 * 10. Bottom sheet trigger
 * 11. Toggle (on/off/disabled)
 * 12. Selection (radio/checkbox)
 * 13. Tabs
 * 14. Badges (status + notification)
 *
 * Replaces the temporary `ThemePreviewScreen` from TASK-010.
 */
@Composable
fun DesignSystemPreviewScreen(onBack: () -> Unit) {
    var currentSpec by remember { mutableStateOf(NamedSpec.Default) }

    IPayTheme(spec = currentSpec.spec) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(IPayTheme.colors.bgNeutralSecondary),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                IPayTopBar(
                    title = "Design System",
                    onBack = onBack,
                )

                Column(
                    modifier = Modifier.padding(IPayTheme.spacing.s16),
                ) {
                    ThemeControlsSection(
                        currentSpec = currentSpec,
                        onCycle = { currentSpec = currentSpec.next() },
                    )
                    SectionSpacer()

                    ColorPaletteSection()
                    SectionSpacer()

                    TypographySection()
                    SectionSpacer()

                    SpacingSection()
                    SectionSpacer()

                    ButtonsSection()
                    SectionSpacer()

                    TextFieldSection()
                    SectionSpacer()

                    CardsSection()
                    SectionSpacer()

                    ChipsSection()
                    SectionSpacer()

                    AlertsSection()
                    SectionSpacer()

                    BottomSheetSection()
                    SectionSpacer()

                    TogglesSection()
                    SectionSpacer()

                    SelectionsSection()
                    SectionSpacer()

                    TabsSection()
                    SectionSpacer()

                    BadgesSection()
                    SectionSpacer()

                    NestedOverrideSection()

                    Spacer(Modifier.height(IPayTheme.spacing.s40))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = IPayTheme.typography.titleMedium,
        color = IPayTheme.colors.textNeutralPrimary,
        modifier = Modifier.padding(bottom = IPayTheme.spacing.s8),
    )
}

@Composable
private fun SectionSpacer() {
    Spacer(Modifier.height(IPayTheme.spacing.s24))
}

// ─────────────────────────────────────────────────────────────────────────────
// Sections
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ThemeControlsSection(currentSpec: NamedSpec, onCycle: () -> Unit) {
    IPayCard(variant = IPayCardVariant.Outlined, modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                "Active spec: ${currentSpec.label}",
                style = IPayTheme.typography.bodyEmphasizedMedium,
                color = IPayTheme.colors.textBrandPrimary,
            )
            Spacer(Modifier.height(IPayTheme.spacing.s8))
            IPayButton(
                text = "Cycle theme → ${currentSpec.next().label}",
                onClick = onCycle,
                variant = IPayButtonVariant.Primary,
                size = IPayButtonSize.Medium,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ColorPaletteSection() {
    SectionTitle("Color palette (semantic tokens)")
    val swatches = listOf(
        "brandPrimary" to IPayTheme.colors.textBrandPrimary,
        "borderBrandPrimary" to IPayTheme.colors.borderBrandPrimary,
        "bgBrandSecondary" to IPayTheme.colors.bgBrandSecondary,
        "textPrimary" to IPayTheme.colors.textNeutralPrimary,
        "textSecondary" to IPayTheme.colors.textNeutralSecondary,
        "textTertiary" to IPayTheme.colors.textNeutralTertiary,
        "bgPrimary" to IPayTheme.colors.bgNeutralPrimary,
        "bgSecondary" to IPayTheme.colors.bgNeutralSecondary,
        "borderPrimary" to IPayTheme.colors.borderNeutralPrimary,
        "iconSuccess" to IPayTheme.colors.iconSuccess,
        "iconWarning" to IPayTheme.colors.iconWarning,
        "iconInfo" to IPayTheme.colors.iconInfo,
        "tabIndicatorActive" to IPayTheme.colors.tabIndicatorActive,
        "buttonPrimaryStart" to IPayTheme.colors.buttonPrimaryBgStart,
        "buttonPrimaryEnd" to IPayTheme.colors.buttonPrimaryBgEnd,
        "notificationStart" to IPayTheme.colors.notificationBgStart,
    )
    ColorSwatchGrid(swatches)
}

@Composable
private fun ColorSwatchGrid(swatches: List<Pair<String, Color>>) {
    Column(verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8)) {
        swatches.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8)) {
                row.forEach { (label, color) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(IPayTheme.spacing.s40)
                                .background(color, IPayTheme.shapes.small)
                                .border(
                                    IPayTheme.stroke.xs,
                                    IPayTheme.colors.borderNeutralPrimary,
                                    IPayTheme.shapes.small,
                                ),
                        )
                        Spacer(Modifier.height(IPayTheme.spacing.s4))
                        Text(
                            text = label,
                            style = IPayTheme.typography.labelXS,
                            color = IPayTheme.colors.textNeutralTertiary,
                        )
                    }
                }
                // Pad row if last row has fewer than 4 items
                repeat(4 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TypographySection() {
    SectionTitle("Typography (SVN-Gilroy fallback SansSerif)")
    Column(verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s4)) {
        Text("Heading L 30/40", style = IPayTheme.typography.headingLarge)
        Text("Heading S 20/26", style = IPayTheme.typography.headingSmall)
        Text("Heading XS 16/24", style = IPayTheme.typography.headingExtraSmall)
        Text("Title L 18/26", style = IPayTheme.typography.titleLarge)
        Text("Title M 16/24", style = IPayTheme.typography.titleMedium)
        Text("Title S 14/20", style = IPayTheme.typography.titleSmall)
        Text("Body L 16/24 medium", style = IPayTheme.typography.bodyLarge)
        Text("Body M 14/20 medium", style = IPayTheme.typography.bodyMedium)
        Text("Body S 12/18 medium", style = IPayTheme.typography.bodySmall)
        Text("Body Emphasized XL 18/26", style = IPayTheme.typography.bodyEmphasizedXL)
        Text("Body Emphasized L 16/24", style = IPayTheme.typography.bodyEmphasizedLarge)
        Text("Body Emphasized M 14/20", style = IPayTheme.typography.bodyEmphasizedMedium)
        Text("Body Emphasized S 12/18", style = IPayTheme.typography.bodyEmphasizedSmall)
        Text("Label L 16/24", style = IPayTheme.typography.labelLarge)
        Text("Label M 14/20", style = IPayTheme.typography.labelMedium)
        Text("Label S 12/18", style = IPayTheme.typography.labelSmall)
        Text("Label XS 10/14", style = IPayTheme.typography.labelXS)
    }
}

@Composable
private fun SpacingSection() {
    SectionTitle("Spacing scale")
    val steps = listOf(
        "s4" to IPayTheme.spacing.s4,
        "s8" to IPayTheme.spacing.s8,
        "s12" to IPayTheme.spacing.s12,
        "s16" to IPayTheme.spacing.s16,
        "s20" to IPayTheme.spacing.s20,
        "s24" to IPayTheme.spacing.s24,
        "s32" to IPayTheme.spacing.s32,
        "s40" to IPayTheme.spacing.s40,
    )
    Column(verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s4)) {
        steps.forEach { (label, dp) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = IPayTheme.typography.labelSmall,
                    color = IPayTheme.colors.textNeutralSecondary,
                    modifier = Modifier.width(IPayTheme.spacing.s40),
                )
                Box(
                    modifier = Modifier
                        .width(dp)
                        .height(IPayTheme.spacing.s12)
                        .background(IPayTheme.colors.borderBrandPrimary, IPayTheme.shapes.r4),
                )
            }
        }
    }
}

@Composable
private fun ButtonsSection() {
    SectionTitle("Buttons")
    Column(verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8)) {
        IPayButton(
            text = "Primary Large",
            onClick = {},
            variant = IPayButtonVariant.Primary,
            size = IPayButtonSize.Large,
            modifier = Modifier.fillMaxWidth(),
        )
        IPayButton(
            text = "Secondary Medium",
            onClick = {},
            variant = IPayButtonVariant.Secondary,
            size = IPayButtonSize.Medium,
            modifier = Modifier.fillMaxWidth(),
        )
        IPayButton(
            text = "Ghost Small",
            onClick = {},
            variant = IPayButtonVariant.Ghost,
            size = IPayButtonSize.Small,
        )
        IPayButton(
            text = "Loading…",
            onClick = {},
            loading = true,
            size = IPayButtonSize.Medium,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8)) {
            IPayIconButton(
                icon = Icons.Default.Send,
                onClick = {},
                variant = IPayButtonVariant.Primary,
                contentDescription = "Send",
            )
            IPayIconButton(
                icon = Icons.Default.AccountBalance,
                onClick = {},
                variant = IPayButtonVariant.Secondary,
                contentDescription = "Bank",
            )
            IPayIconButton(
                icon = Icons.Default.AutoAwesome,
                onClick = {},
                variant = IPayButtonVariant.Ghost,
                contentDescription = "AI",
            )
        }
    }
}

@Composable
private fun TextFieldSection() {
    SectionTitle("TextField")
    var value by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8)) {
        IPayTextField(
            value = value,
            onValueChange = { value = it },
            label = "Số tài khoản",
            placeholder = "Nhập STK",
            helperText = "Tối đa 15 chữ số",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        IPayTextField(
            value = "Lỗi giả lập",
            onValueChange = {},
            label = "With error",
            errorText = "Giá trị không hợp lệ",
            modifier = Modifier.fillMaxWidth(),
        )
        IPayTextField(
            value = "Disabled value",
            onValueChange = {},
            label = "Disabled",
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CardsSection() {
    SectionTitle("Cards")
    Column(verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8)) {
        IPayCard(variant = IPayCardVariant.Plain, modifier = Modifier.fillMaxWidth()) {
            Text("Plain card", style = IPayTheme.typography.bodyMedium)
        }
        IPayCard(variant = IPayCardVariant.Elevated, modifier = Modifier.fillMaxWidth()) {
            Text("Elevated card (shadow)", style = IPayTheme.typography.bodyMedium)
        }
        IPayCard(variant = IPayCardVariant.Outlined, modifier = Modifier.fillMaxWidth()) {
            Text("Outlined card", style = IPayTheme.typography.bodyMedium)
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ChipsSection() {
    SectionTitle("Chips")
    var selected by remember { mutableStateOf(false) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8)) {
        IPayChip(
            text = "Default",
            onClick = {},
            variant = IPayChipVariant.Default,
        )
        IPayChip(
            text = if (selected) "Tap to deselect" else "Tap to select",
            onClick = { selected = !selected },
            variant = if (selected) IPayChipVariant.Selected else IPayChipVariant.Default,
        )
        IPayAIChip(text = "AI Suggestion")
    }
}

@Composable
private fun AlertsSection() {
    SectionTitle("Alert banners")
    Column(verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8)) {
        IPayAlertBanner(
            text = "Hạn mức Napas còn 5 triệu cho hôm nay.",
            variant = IPayAlertVariant.Info,
            title = "Thông tin",
            modifier = Modifier.fillMaxWidth(),
        )
        IPayAlertBanner(
            text = "Số tiền vượt hạn mức Napas — vui lòng chọn kênh khác.",
            variant = IPayAlertVariant.Warning,
            title = "Cảnh báo",
            modifier = Modifier.fillMaxWidth(),
        )
        IPayAlertBanner(
            text = "Giao dịch đã ghi nhận thành công.",
            variant = IPayAlertVariant.Success,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetSection() {
    SectionTitle("Bottom sheet")
    var show by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    IPayButton(
        text = "Open IPayBottomSheet",
        onClick = { show = true },
        variant = IPayButtonVariant.Secondary,
        size = IPayButtonSize.Medium,
    )

    if (show) {
        IPayBottomSheet(
            onDismissRequest = { show = false },
            sheetState = sheetState,
            header = {
                Text(
                    "Header slot",
                    style = IPayTheme.typography.titleLarge,
                    color = IPayTheme.colors.textNeutralPrimary,
                )
            },
            footer = {
                Row(horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8)) {
                    IPayButton(
                        text = "Confirm",
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion { show = false }
                        },
                        variant = IPayButtonVariant.Primary,
                        size = IPayButtonSize.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    IPayButton(
                        text = "Cancel",
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion { show = false }
                        },
                        variant = IPayButtonVariant.Ghost,
                        size = IPayButtonSize.Medium,
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        ) {
            Text(
                "Body content slot — explain what this sheet is for, list options, etc.",
                style = IPayTheme.typography.bodyMedium,
                color = IPayTheme.colors.textNeutralSecondary,
            )
        }
    }
}

@Composable
private fun TogglesSection() {
    SectionTitle("Toggles")
    var on by remember { mutableStateOf(true) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s24),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Interactive", style = IPayTheme.typography.labelSmall)
            Spacer(Modifier.height(IPayTheme.spacing.s4))
            IPayToggle(checked = on, onCheckedChange = { on = it })
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Off (static)", style = IPayTheme.typography.labelSmall)
            Spacer(Modifier.height(IPayTheme.spacing.s4))
            IPayToggle(checked = false, onCheckedChange = {})
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Disabled", style = IPayTheme.typography.labelSmall)
            Spacer(Modifier.height(IPayTheme.spacing.s4))
            IPayToggle(checked = true, onCheckedChange = {}, enabled = false)
        }
    }
}

@Composable
private fun SelectionsSection() {
    SectionTitle("Selection cards")
    var radio by remember { mutableStateOf(0) }
    var checks by remember { mutableStateOf(setOf(0)) }
    Column(verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8)) {
        listOf("Trong VietinBank", "Liên ngân hàng (Napas)").forEachIndexed { idx, label ->
            IPaySelection(
                label = label,
                description = if (idx == 0) "Phí 0đ, tức thì" else "Phí 5,500đ, 1 phút",
                selected = radio == idx,
                onClick = { radio = idx },
                variant = IPaySelectionVariant.Radio,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        listOf("Lưu STK gần đây", "Nhận biên lai email").forEachIndexed { idx, label ->
            IPaySelection(
                label = label,
                selected = idx in checks,
                onClick = {
                    checks = if (idx in checks) checks - idx else checks + idx
                },
                variant = IPaySelectionVariant.Checkbox,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TabsSection() {
    SectionTitle("Tabs")
    var selected by remember { mutableStateOf(0) }
    IPayHorizontalTabs(
        tabs = listOf("Tất cả", "Đang chờ", "Hoàn tất"),
        selectedIndex = selected,
        onTabSelected = { selected = it },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(IPayTheme.spacing.s8))
    Text(
        "Selected tab: $selected",
        style = IPayTheme.typography.bodySmall,
        color = IPayTheme.colors.textNeutralTertiary,
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun BadgesSection() {
    SectionTitle("Badges")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8),
        verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8),
    ) {
        IPayStatusBadgeVariant.entries.forEach { variant ->
            IPayStatusBadge(text = variant.name, variant = variant)
        }
    }
    Spacer(Modifier.height(IPayTheme.spacing.s8))
    Row(
        horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Notification:", style = IPayTheme.typography.labelSmall)
        IPayNotificationBadge() // dot
        IPayNotificationBadge(count = 3)
        IPayNotificationBadge(count = 12)
    }
}

@Composable
private fun NestedOverrideSection() {
    SectionTitle("Nested override (forces Demo regardless of root)")
    IPayTheme(spec = IPayThemes.Demo) {
        IPayCard(
            variant = IPayCardVariant.Outlined,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Text(
                    "Subtree dùng IPayThemes.Demo (red brand)",
                    style = IPayTheme.typography.bodyEmphasizedMedium,
                    color = IPayTheme.colors.textBrandPrimary,
                )
                Spacer(Modifier.height(IPayTheme.spacing.s8))
                IPayButton(
                    text = "Red brand button",
                    onClick = {},
                    variant = IPayButtonVariant.Primary,
                    size = IPayButtonSize.Small,
                )
            }
        }
    }
}

private enum class NamedSpec(val label: String, val spec: IPayThemeSpec) {
    Default("Default (iPay light)", IPayThemes.Default),
    Dark("Dark (stub)", IPayThemes.Dark),
    Demo("Demo (red brand)", IPayThemes.Demo);

    fun next(): NamedSpec = when (this) {
        Default -> Dark
        Dark -> Demo
        Demo -> Default
    }
}
