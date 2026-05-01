package com.poc.behavioralfraud.ui.screens.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poc.behavioralfraud.R
import com.poc.behavioralfraud.data.mock.MockBank
import com.poc.behavioralfraud.data.mock.MockBanks
import com.poc.behavioralfraud.ui.components.IPayHorizontalTabs
import com.poc.behavioralfraud.ui.components.IPayScreenBackground
import com.poc.behavioralfraud.ui.components.IPayTopBar
import com.poc.behavioralfraud.ui.components.IPayTopBarVariant
import com.poc.behavioralfraud.ui.components.safeClickable
import com.poc.behavioralfraud.ui.theme.IPayPalette
import com.poc.behavioralfraud.ui.theme.IPayTheme

/**
 * Recipient screen — Figma `1:15494` "MH 02. Thêm người nhận".
 *
 * Layout (top to bottom):
 *   1. Top bar — back + "Thông tin người nhận" title + "Label" trailing link
 *   2. Horizontal tabs — "Số tài khoản" (active) / "Số thẻ"
 *   3. Active text field — "Số tài khoản/ Alias" with QR scan suffix icon,
 *      brand-blue active border + focus ring shadow
 *   4. Bank selection (single-line picker) — circular pd_new_recipient icon +
 *      "Chọn ngân hàng" text + chevron-down icon
 *   5. "Ngân hàng đề xuất" — horizontal row of 6 bank chip avatars
 *      (VietinBank / Agribank / BIDV / Vietcombank / MB / Techcombank)
 *   6. Sticky bottom button (rendered by parent NavHost-level button bar in
 *      Figma; here implemented inline for POC self-contained)
 *
 * [transferType] is derived later from the bank user picks (VietinBank →
 * Internal, others → Napas) and propagated via [onContinue].
 */
@Composable
fun RecipientScreen(
    transferType: TransferType,
    onContinue: (accountNumber: String, bank: MockBank) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    collector: com.poc.behavioralfraud.data.collector.BehavioralCollector? = null,
) {
    var accountNumber by remember { mutableStateOf("") }
    var selectedBank by remember { mutableStateOf<MockBank?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Số tài khoản, 1 = Số thẻ

    val canContinue = accountNumber.length >= ACCOUNT_NUMBER_MIN_LENGTH && selectedBank != null

    IPayScreenBackground(modifier = modifier) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        IPayTopBar(
            title = "Thông tin người nhận",
            variant = IPayTopBarVariant.Transparent,
            onBack = onBack,
            trailing = {
                Text(
                    text = "Label",
                    style = IPayTheme.typography.bodyEmphasizedMedium,
                    color = IPayTheme.colors.textBrandPrimary,
                    modifier = Modifier.safeClickable(
                        onSafeClick = { /* POC no-op */ },
                        role = Role.Button,
                    ),
                )
            },
        )

        // Tabs row — Figma 1:15505 (343×32 at x=16, y=4)
        Box(modifier = Modifier.padding(horizontal = RecipientLayout.HORIZ_PADDING)) {
            IPayHorizontalTabs(
                tabs = listOf("Số tài khoản", "Số thẻ"),
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(RecipientLayout.GAP_AFTER_TABS))

        // Content — Figma 1:15506 frame (343×248 at x=16, y=60), 16dp gap
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RecipientLayout.HORIZ_PADDING),
            verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s16),
        ) {
            // Active text field — record field focus + text changes for behavioral analysis
            ActiveAccountField(
                value = accountNumber,
                onValueChange = { newValue ->
                    val previousLength = accountNumber.length
                    accountNumber = newValue.filter(Char::isDigit)
                    collector?.onTextChanged("account_number", previousLength, accountNumber.length)
                },
                onFocused = { collector?.onFieldFocus("account_number") },
            )

            // Bank selector
            BankSelector(
                selectedBank = selectedBank,
                onTap = { /* TODO: open bank picker bottom sheet — POC no-op */ },
            )

            // Suggested banks row
            SuggestedBanksRow(
                banks = SUGGESTED_BANK_CODES.mapNotNull { code ->
                    MockBanks.list.firstOrNull { it.code == code }
                },
                onBankPick = { selectedBank = it },
            )
        }

        // Sticky bottom continue button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(IPayTheme.colors.bgNeutralPrimary)
                .padding(IPayTheme.spacing.s16),
        ) {
            com.poc.behavioralfraud.ui.components.IPayButton(
                text = "Tiếp tục",
                onClick = {
                    val bank = selectedBank ?: return@IPayButton
                    onContinue(accountNumber, bank)
                },
                enabled = canContinue,
                variant = com.poc.behavioralfraud.ui.components.IPayButtonVariant.Primary,
                size = com.poc.behavioralfraud.ui.components.IPayButtonSize.Large,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section: Active account number text field (Figma 1:15507)
// White bg, 1.5dp brand-blue active border, 16dp radius, 3dp focus-ring shadow.
// Active label "Số tài khoản/ Alias" inside, blue caret, QR scan suffix icon.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActiveAccountField(
    value: String,
    onValueChange: (String) -> Unit,
    onFocused: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    androidx.compose.runtime.LaunchedEffect(isFocused) {
        if (isFocused) onFocused()
    }
    // Active state default per Figma (auto-focus at screen open)
    val borderColor = if (isFocused || value.isEmpty()) {
        IPayTheme.colors.inputBorderActive
    } else {
        IPayTheme.colors.inputBorderDefault
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Focus ring: shadow #DEF1FF
            .shadow(
                elevation = IPayTheme.spacing.s4,
                shape = RoundedCornerShape(16.dp),
                ambientColor = IPayPalette.VietinDarkBlue10,
                spotColor = IPayPalette.VietinDarkBlue10,
            )
            .clip(RoundedCornerShape(16.dp))
            .background(IPayTheme.colors.inputBgPrimaryDefault)
            .border(
                width = IPayTheme.stroke.s,
                color = borderColor,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = IPayTheme.spacing.s16, vertical = IPayTheme.spacing.s8),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s12),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Số tài khoản/ Alias",
                    style = IPayTheme.typography.bodyMedium,
                    color = IPayTheme.colors.inputLabelActive,
                    maxLines = 1,
                )
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    interactionSource = interactionSource,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = IPayTheme.typography.bodyEmphasizedLarge.copy(
                        color = IPayTheme.colors.inputTextDefault,
                    ),
                    cursorBrush = SolidColor(IPayTheme.colors.inputCaret),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // Suffix: QR scan icon (24dp)
            Icon(
                painter = painterResource(R.drawable.ic_qr_scan),
                contentDescription = "Quét QR",
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(24.dp)
                    .safeClickable(
                        onSafeClick = { /* TODO: open QR scanner — POC no-op */ },
                        role = Role.Button,
                    ),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section: Bank selector picker (Figma 1:15508)
// Single-line row: 44dp circular icon container + "Chọn ngân hàng" + chevron.
// Tap → opens bank picker bottom sheet (TODO).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BankSelector(
    selectedBank: MockBank?,
    onTap: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(IPayTheme.colors.inputBgPrimaryDefault)
            .border(
                width = IPayTheme.stroke.xs,
                color = IPayTheme.colors.inputBorderDefault,
                shape = RoundedCornerShape(16.dp),
            )
            .safeClickable(onSafeClick = onTap, role = Role.Button)
            .padding(horizontal = IPayTheme.spacing.s16, vertical = IPayTheme.spacing.s8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s12),
    ) {
        // 44dp circular icon container with brand-light bg
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(IPayPalette.BrandBgLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.pd_new_recipient),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = selectedBank?.shortName ?: "Chọn ngân hàng",
            style = IPayTheme.typography.bodyEmphasizedLarge,
            color = IPayTheme.colors.textNeutralPrimary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.ic_chevron_down),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(24.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section: Suggested banks row (Figma 1:6826)
// Title "Ngân hàng đề xuất" + 6 bank avatars (44dp circular bg + logo + label).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SuggestedBanksRow(
    banks: List<MockBank>,
    onBankPick: (MockBank) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8)) {
        Text(
            text = "Ngân hàng đề xuất",
            style = IPayTheme.typography.titleSmall,
            color = IPayTheme.colors.textNeutralTertiary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s16),
        ) {
            banks.forEach { bank ->
                SuggestedBankAvatar(bank = bank, onClick = { onBankPick(bank) })
            }
        }
    }
}

@Composable
private fun SuggestedBankAvatar(
    bank: MockBank,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.safeClickable(onSafeClick = onClick, role = Role.Button),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s6),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(IPayPalette.BrandBgLight),
            contentAlignment = Alignment.Center,
        ) {
            val logoRes = SUGGESTED_BANK_LOGO_RES[bank.code] ?: R.drawable.pd_new_recipient
            Icon(
                painter = painterResource(logoRes),
                contentDescription = bank.shortName,
                tint = Color.Unspecified,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = bank.shortName,
            style = IPayTheme.typography.bodyEmphasizedSmall,
            color = IPayTheme.colors.textNeutralSecondary,
            maxLines = 1,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Constants
// ─────────────────────────────────────────────────────────────────────────────

/** Bank codes shown in "Ngân hàng đề xuất" row, in display order. */
private val SUGGESTED_BANK_CODES = listOf("CTG", "AGR", "BIDV", "VCB", "MBB", "TCB")

/** Maps MockBank.code → logo drawable resource for suggested banks row. */
private val SUGGESTED_BANK_LOGO_RES: Map<String, Int> = mapOf(
    "CTG" to R.drawable.logo_bank_vietinbank,
    "AGR" to R.drawable.logo_bank_agribank,
    "BIDV" to R.drawable.logo_bank_bidv,
    "VCB" to R.drawable.logo_bank_vietcombank,
    "MBB" to R.drawable.logo_bank_mb,
    "TCB" to R.drawable.logo_bank_techcombank,
)

private const val ACCOUNT_NUMBER_MIN_LENGTH = 8

private object RecipientLayout {
    val HORIZ_PADDING: Dp = 16.dp
    val GAP_AFTER_TABS: Dp = 24.dp
}
