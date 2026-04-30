@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.poc.behavioralfraud.ui.screens.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poc.behavioralfraud.R
import com.poc.behavioralfraud.data.mock.MockBank
import com.poc.behavioralfraud.data.mock.MockBanks
import com.poc.behavioralfraud.ui.components.IPayButton
import com.poc.behavioralfraud.ui.components.IPayButtonSize
import com.poc.behavioralfraud.ui.components.IPayButtonVariant
import com.poc.behavioralfraud.ui.components.IPayToggle
import com.poc.behavioralfraud.ui.components.IPayTopBar
import com.poc.behavioralfraud.ui.components.safeClickable
import com.poc.behavioralfraud.ui.theme.IPayPalette
import com.poc.behavioralfraud.ui.theme.IPayTheme
import kotlinx.coroutines.launch

/**
 * Transfer form — Figma `1:15651` "MH 04. Nhập xong số tiền".
 *
 * Layout (top to bottom):
 *   1. Top bar — back + "Khởi tạo giao dịch"
 *   2. Source account card (sticky top, 343×92) — gradient brand badge "Nguồn tiền"
 *      + close-eye icon, white card with backdrop blur, 1dp brand-light border:
 *      VietinBank logo + balance "******* VND" + insurance icon + masked account
 *      + dot + holder name + chevron-down
 *   3. Section "Đến người nhận":
 *      - Recipient card with bank logo + name + dot + bank short name
 *      - Toggle "Lưu người nhận"
 *   4. Section "Thông tin chuyển khoản" + "Hạn mức" ghost link:
 *      - Amount field (96h) — label + value + "VND" pill chip suffix +
 *        supporting text bg with Vietnamese amount-in-words
 *      - Note field (60h)
 *      - Categorization chips row — 5 chips with emoji + label
 *   5. Sticky bottom button bar — "Tiếp tục"
 *
 * Reads/writes state via [TransferOrchestratorViewModel]. State persists
 * across configuration changes.
 */
@Composable
fun TransferFormScreen(
    viewModel: TransferOrchestratorViewModel,
    onNavigateToOtp: () -> Unit,
    onSheetShown: () -> Unit,
    onSheetDecision: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showOverLimitSheet by remember { mutableStateOf(false) }
    var saveRecipient by remember { mutableStateOf(false) }
    var selectedCategoryIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                TransferEvent.NavigateToOtp -> onNavigateToOtp()
                TransferEvent.ShowOverLimitSheet -> {
                    showOverLimitSheet = true
                }
                TransferEvent.NavigateToSuccess,
                is TransferEvent.ShowError -> Unit  // handled by OTP screen
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IPayTheme.colors.bgNeutralPrimary),
    ) {
        IPayTopBar(title = "Khởi tạo giao dịch", onBack = onBack)

        // Source account card sticky top
        SourceAccountCard()
        Spacer(Modifier.height(IPayTheme.spacing.s24))

        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = IPayTheme.spacing.s16)
                    .padding(bottom = STICKY_BUTTON_HEIGHT),
                verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s24),
            ) {
                RecipientSection(
                    bank = state.recipientBank ?: MockBanks.list.first { it.code == "VCB" },
                    accountNumber = state.recipientAccount.ifEmpty { "107001755352" },
                    holderName = "NGUYEN DAO TRUONG AN",
                    saveRecipient = saveRecipient,
                    onSaveToggle = { saveRecipient = it },
                )

                TransferInfoSection(
                    rawDigits = state.amountRaw,
                    onAmountChange = { newValue ->
                        val previousLength = state.amountRaw.length
                        viewModel.setAmount(newValue)
                        viewModel.collector.onTextChanged(
                            "amount",
                            previousLength,
                            newValue.filter(Char::isDigit).length,
                        )
                    },
                    onAmountFocused = { viewModel.collector.onFieldFocus("amount") },
                    note = state.note,
                    onNoteChange = { newValue ->
                        val previousLength = state.note.length
                        viewModel.setNote(newValue)
                        viewModel.collector.onTextChanged(
                            "note",
                            previousLength,
                            newValue.length,
                        )
                    },
                    onNoteFocused = { viewModel.collector.onFieldFocus("note") },
                    selectedCategoryIndex = selectedCategoryIndex,
                    onCategorySelected = { selectedCategoryIndex = it },
                )
            }

            // Sticky bottom button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(STICKY_BUTTON_HEIGHT)
                    .background(IPayTheme.colors.bgNeutralPrimary)
                    .padding(IPayTheme.spacing.s16)
                    .align(Alignment.BottomCenter),
            ) {
                IPayButton(
                    text = "Tiếp tục",
                    onClick = { scope.launch { viewModel.onFormContinue() } },
                    enabled = state.amountVnd > 0L,
                    variant = IPayButtonVariant.Primary,
                    size = IPayButtonSize.Large,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showOverLimitSheet) {
        OverNapasLimitDialog(
            amountVnd = state.amountVnd,
            onProceed = {
                showOverLimitSheet = false
                onSheetDecision("proceed")
                scope.launch { viewModel.onOverLimitProceed() }
            },
            onDismiss = {
                showOverLimitSheet = false
                onSheetDecision("cancel")
            },
            onShown = onSheetShown,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section: Source account card (Figma 1:15656 "iPay.Cards / Tài khoản nguồn")
// Gradient badge "Nguồn tiền" stacked above white card with backdrop-blur,
// 1dp brand-light border, 16dp radius. Badge overlaps card by 12dp.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SourceAccountCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = IPayTheme.spacing.s16),
        horizontalAlignment = Alignment.Start,
    ) {
        // Highlight badge — gradient brand pill (offset over card by 12dp)
        Row(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 8.dp,
                        topEnd = 8.dp,
                        bottomStart = 0.dp,
                        bottomEnd = 8.dp,
                    ),
                )
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            IPayPalette.VietinDarkBlue80,
                            IPayPalette.VietinDarkBlue60,
                        ),
                    ),
                )
                .padding(horizontal = IPayTheme.spacing.s8, vertical = IPayTheme.spacing.s4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s4),
        ) {
            Text(
                text = "Nguồn tiền",
                style = IPayTheme.typography.bodySmall,
                color = Color.White,
            )
            Icon(
                painter = painterResource(R.drawable.ic_close_eye),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(16.dp),
            )
        }

        // Card content (overlap badge by negative margin)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset()
                .clip(RoundedCornerShape(16.dp))
                .background(IPayTheme.colors.bgNeutralPrimary)
                .border(
                    width = IPayTheme.stroke.xs,
                    color = IPayPalette.VietinDarkBlue20,
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(horizontal = IPayTheme.spacing.s16, vertical = IPayTheme.spacing.s10)
                .padding(top = IPayTheme.spacing.s16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s12),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(IPayPalette.BrandBgLight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.logo_bank_vietinbank),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "******* VND",
                        style = IPayTheme.typography.bodyEmphasizedXL,
                        color = IPayTheme.colors.textNeutralPrimary,
                    )
                    Spacer(Modifier.width(IPayTheme.spacing.s4))
                    Icon(
                        painter = painterResource(R.drawable.ic_insurance),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.height(IPayTheme.spacing.s2))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "32902481933/LUNA",
                        style = IPayTheme.typography.bodyMedium,
                        color = IPayTheme.colors.textNeutralTertiary,
                    )
                    Spacer(Modifier.width(IPayTheme.spacing.s4))
                    Icon(
                        painter = painterResource(R.drawable.ic_dot),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(8.dp),
                    )
                    Spacer(Modifier.width(IPayTheme.spacing.s4))
                    Text(
                        text = "NGUYEN HOANG TRAN LE",
                        style = IPayTheme.typography.bodyMedium,
                        color = IPayTheme.colors.textNeutralTertiary,
                        maxLines = 1,
                    )
                }
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

private fun Modifier.offset(): Modifier = this  // placeholder for negative-margin styling

// ─────────────────────────────────────────────────────────────────────────────
// Section: Recipient section ("Đến người nhận")
// Title + recipient card (white bg, 1dp Ink30 border) + toggle "Lưu người nhận"
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecipientSection(
    bank: MockBank,
    accountNumber: String,
    holderName: String,
    saveRecipient: Boolean,
    onSaveToggle: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s12)) {
        Text(
            text = "Đến người nhận",
            style = IPayTheme.typography.titleMedium,
            color = IPayTheme.colors.textNeutralPrimary,
        )

        // Recipient card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(IPayTheme.colors.bgNeutralPrimary)
                .border(
                    width = IPayTheme.stroke.xs,
                    color = IPayTheme.colors.borderNeutralSecondary,
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(horizontal = IPayTheme.spacing.s16, vertical = IPayTheme.spacing.s8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s12),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(IPayPalette.BrandBgLight),
                contentAlignment = Alignment.Center,
            ) {
                val logoRes = bankLogoResFor(bank.code)
                Icon(
                    painter = painterResource(logoRes),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = holderName,
                    style = IPayTheme.typography.bodyEmphasizedLarge,
                    color = IPayTheme.colors.textNeutralPrimary,
                    maxLines = 1,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = accountNumber,
                        style = IPayTheme.typography.bodyMedium,
                        color = IPayTheme.colors.textNeutralTertiary,
                    )
                    Spacer(Modifier.width(IPayTheme.spacing.s4))
                    Icon(
                        painter = painterResource(R.drawable.ic_dot),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(8.dp),
                    )
                    Spacer(Modifier.width(IPayTheme.spacing.s4))
                    Text(
                        text = bank.shortName,
                        style = IPayTheme.typography.bodyMedium,
                        color = IPayTheme.colors.textNeutralTertiary,
                    )
                }
            }
        }

        // Toggle "Lưu người nhận"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8),
        ) {
            IPayToggle(checked = saveRecipient, onCheckedChange = onSaveToggle)
            Text(
                text = "Lưu người nhận",
                style = IPayTheme.typography.titleMedium,
                color = IPayTheme.colors.textNeutralPrimary,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section: Transfer info ("Thông tin chuyển khoản")
// Title + "Hạn mức" link + amount field (with VND chip + supporting text) +
// note field + categorization chips row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TransferInfoSection(
    rawDigits: String,
    onAmountChange: (String) -> Unit,
    onAmountFocused: () -> Unit = {},
    note: String,
    onNoteChange: (String) -> Unit,
    onNoteFocused: () -> Unit = {},
    selectedCategoryIndex: Int,
    onCategorySelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s12)) {
        // Title row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Thông tin chuyển khoản",
                style = IPayTheme.typography.titleMedium,
                color = IPayTheme.colors.textNeutralPrimary,
            )
            Text(
                text = "Hạn mức",
                style = IPayTheme.typography.titleSmall,
                color = IPayTheme.colors.buttonGhostLabel,
                modifier = Modifier.safeClickable(
                    onSafeClick = { /* POC no-op */ },
                    role = Role.Button,
                ),
            )
        }

        // Amount field (taller, with VND chip + supporting text)
        AmountFieldWithChip(
            rawDigits = rawDigits,
            onValueChange = onAmountChange,
            onFocused = onAmountFocused,
        )

        // Note field
        SimpleNoteField(
            value = note,
            onValueChange = onNoteChange,
            onFocused = onNoteFocused,
        )

        // Categorization chips
        CategoryChipsSection(
            selectedIndex = selectedCategoryIndex,
            onSelected = onCategorySelected,
        )
    }
}

@Composable
private fun AmountFieldWithChip(
    rawDigits: String,
    onValueChange: (String) -> Unit,
    onFocused: () -> Unit = {},
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    LaunchedEffect(isFocused) { if (isFocused) onFocused() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = IPayTheme.stroke.xs,
                color = IPayTheme.colors.inputBorderDefault,
                shape = RoundedCornerShape(16.dp),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(IPayTheme.colors.inputBgPrimaryDefault)
                .padding(horizontal = IPayTheme.spacing.s16, vertical = IPayTheme.spacing.s8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s12),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Số tiền",
                    style = IPayTheme.typography.bodyMedium,
                    color = IPayTheme.colors.inputLabelDefault,
                )
                BasicTextField(
                    value = rawDigits,
                    onValueChange = onValueChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = IPayTheme.typography.bodyEmphasizedLarge.copy(
                        color = IPayTheme.colors.inputTextDefault,
                    ),
                    cursorBrush = SolidColor(IPayTheme.colors.inputCaret),
                    visualTransformation = remember { ThousandSeparatorVisualTransformation() },
                    interactionSource = interactionSource,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // VND pill chip suffix
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(IPayPalette.Ink10)
                    .padding(horizontal = IPayTheme.spacing.s8, vertical = IPayTheme.spacing.s2),
            ) {
                Text(
                    text = "VND",
                    style = IPayTheme.typography.bodyEmphasizedMedium,
                    color = IPayTheme.colors.textNeutralPrimary,
                )
            }
        }
        // Supporting text (Vietnamese amount in words / hint)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(IPayTheme.colors.inputBgSecondaryDefault)
                .padding(horizontal = IPayTheme.spacing.s16, vertical = IPayTheme.spacing.s8),
        ) {
            Text(
                text = vietnameseAmountHint(rawDigits),
                style = IPayTheme.typography.bodyMedium,
                color = IPayTheme.colors.textNeutralSecondary,
            )
        }
    }
}

@Composable
private fun SimpleNoteField(
    value: String,
    onValueChange: (String) -> Unit,
    onFocused: () -> Unit = {},
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    LaunchedEffect(isFocused) { if (isFocused) onFocused() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(IPayTheme.colors.inputBgPrimaryDefault)
            .border(
                width = IPayTheme.stroke.xs,
                color = IPayTheme.colors.inputBorderDefault,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = IPayTheme.spacing.s16, vertical = IPayTheme.spacing.s8),
    ) {
        Text(
            text = "Nội dung",
            style = IPayTheme.typography.bodyMedium,
            color = IPayTheme.colors.inputLabelDefault,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = IPayTheme.typography.bodyEmphasizedLarge.copy(
                color = IPayTheme.colors.inputTextDefault,
            ),
            cursorBrush = SolidColor(IPayTheme.colors.inputCaret),
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CategoryChipsSection(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s12)) {
        Text(
            text = "Phân loại giao dịch",
            style = IPayTheme.typography.titleSmall,
            color = IPayTheme.colors.textNeutralTertiary,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScrollOrSpacedRow(),
            horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8),
        ) {
            CATEGORIES.forEachIndexed { index, label ->
                CategoryChip(
                    label = label,
                    selected = index == selectedIndex,
                    onClick = { onSelected(index) },
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(
                if (selected) {
                    IPayTheme.colors.bgBrandSecondary
                } else {
                    Color.Transparent
                },
            )
            .border(
                width = IPayTheme.stroke.s,
                color = if (selected) {
                    IPayTheme.colors.borderBrandPrimary
                } else {
                    IPayTheme.colors.chipBorder
                },
                shape = RoundedCornerShape(percent = 50),
            )
            .safeClickable(onSafeClick = onClick, role = Role.Button)
            .padding(horizontal = IPayTheme.spacing.s12, vertical = IPayTheme.spacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = IPayTheme.typography.titleSmall,
            color = if (selected) {
                IPayTheme.colors.textBrandPrimary
            } else {
                IPayTheme.colors.chipLabel
            },
        )
    }
}

private fun Modifier.horizontalScrollOrSpacedRow(): Modifier =
    this  // POC: simple Row; if overflow, future: wrap with horizontalScroll

// ─────────────────────────────────────────────────────────────────────────────
// Over-Napas-limit alert dialog (Figma 1:15679 "iPay.Dialogs" overlay)
// Centered Material3 AlertDialog with brand styling — replaces previous bottom sheet.
// FR-CL-10 REQ-13 — captures decisionTimeOverLimitMs via [onShown]/[onDecision].
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OverNapasLimitDialog(
    amountVnd: Long,
    onProceed: () -> Unit,
    onDismiss: () -> Unit,
    onShown: () -> Unit,
) {
    LaunchedEffect(Unit) { onShown() }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IPayTheme.colors.bgNeutralPrimary,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(IPayTheme.colors.iconWarning.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = IPayTheme.colors.iconWarning,
                    modifier = Modifier.size(28.dp),
                )
            }
        },
        title = {
            Text(
                text = "Vượt hạn mức Napas",
                style = IPayTheme.typography.titleLarge,
                color = IPayTheme.colors.textNeutralPrimary,
            )
        },
        text = {
            Text(
                text = "Số tiền ${"%,d".format(amountVnd)} VND vượt hạn mức Napas. " +
                    "Bạn có thể tiếp tục bằng kênh chuyển thường, thời gian xử lý sẽ lâu hơn.",
                style = IPayTheme.typography.bodyMedium,
                color = IPayTheme.colors.textNeutralSecondary,
            )
        },
        confirmButton = {
            IPayButton(
                text = "Chuyển bằng kênh thường",
                onClick = onProceed,
                variant = IPayButtonVariant.Primary,
                size = IPayButtonSize.Medium,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        dismissButton = {
            IPayButton(
                text = "Huỷ",
                onClick = onDismiss,
                variant = IPayButtonVariant.Ghost,
                size = IPayButtonSize.Medium,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private val CATEGORIES = listOf(
    "🍔  Ăn uống",
    "🛍  Mua sắm",
    "✈️  Du lịch",
    "📊 Đầu tư",
    "🎬  Giải trí",
)

private fun bankLogoResFor(code: String): Int = when (code) {
    "CTG" -> R.drawable.logo_bank_vietinbank
    "AGR" -> R.drawable.logo_bank_agribank
    "BIDV" -> R.drawable.logo_bank_bidv
    "VCB" -> R.drawable.logo_bank_vietcombank
    "MBB" -> R.drawable.logo_bank_mb
    "TCB" -> R.drawable.logo_bank_techcombank
    else -> R.drawable.pd_new_recipient
}

/** Approximate Vietnamese amount-in-words hint. POC: format-based, not full conversion. */
private fun vietnameseAmountHint(rawDigits: String): String {
    val n = rawDigits.toLongOrNull() ?: 0L
    return when {
        n <= 0L -> "Nhập số tiền cần chuyển"
        n < 1_000L -> "$n đồng"
        n < 1_000_000L -> "${n / 1_000} nghìn đồng"
        n < 1_000_000_000L -> "${n / 1_000_000} triệu đồng"
        else -> "${n / 1_000_000_000} tỷ đồng"
    }
}

private val STICKY_BUTTON_HEIGHT: Dp = 88.dp
