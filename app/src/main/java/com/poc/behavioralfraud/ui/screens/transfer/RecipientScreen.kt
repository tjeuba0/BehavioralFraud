package com.poc.behavioralfraud.ui.screens.transfer

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import com.poc.behavioralfraud.data.mock.MockBank
import com.poc.behavioralfraud.data.mock.MockBanks
import com.poc.behavioralfraud.data.mock.MockRecipient
import com.poc.behavioralfraud.data.mock.MockRecipients
import com.poc.behavioralfraud.ui.components.IPayButton
import com.poc.behavioralfraud.ui.components.IPayButtonSize
import com.poc.behavioralfraud.ui.components.IPayButtonVariant
import com.poc.behavioralfraud.ui.components.IPayCard
import com.poc.behavioralfraud.ui.components.IPayCardVariant
import com.poc.behavioralfraud.ui.components.IPayChip
import com.poc.behavioralfraud.ui.components.IPayChipVariant
import com.poc.behavioralfraud.ui.components.IPayTextField
import com.poc.behavioralfraud.ui.components.IPayTopBar
import com.poc.behavioralfraud.ui.components.safeClickable
import com.poc.behavioralfraud.ui.theme.IPayTheme

/**
 * Recipient selection — FR-CL-10 REQ-03.
 *
 * Sections (top to bottom):
 *   1. STK input (focus first — `account_number` field)
 *   2. Bank list (~20 banks scrollable; only shown when [transferType] == Napas)
 *   3. Recent recipients (chip row — tap to autofill STK + bank)
 *   4. Sticky "Tiếp tục" button — disabled until STK + bank both set
 *
 * Internal transfer (VietinBank) skips bank selection — bank pre-set to VietinBank.
 */
@Composable
fun RecipientScreen(
    transferType: TransferType,
    onContinue: (accountNumber: String, bank: MockBank) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var accountNumber by remember { mutableStateOf("") }
    var selectedBank by remember {
        mutableStateOf<MockBank?>(
            // Internal transfer: pre-select VietinBank, hide bank list
            if (transferType == TransferType.Internal) MockBanks.list.first { it.code == "CTG" } else null,
        )
    }

    val canContinue = accountNumber.length >= ACCOUNT_NUMBER_MIN_LENGTH && selectedBank != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IPayTheme.colors.bgNeutralSecondary),
    ) {
        IPayTopBar(title = "Người nhận", onBack = onBack)

        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(IPayTheme.spacing.s16)
                    .padding(bottom = STICKY_BUTTON_HEIGHT_DP),
                verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s24),
            ) {
                AccountNumberSection(
                    value = accountNumber,
                    onValueChange = { accountNumber = it.filter { ch -> ch.isDigit() } },
                )

                if (MockRecipients.list.isNotEmpty()) {
                    RecentRecipientsSection(
                        recipients = MockRecipients.list,
                        onRecipientTap = { recipient ->
                            accountNumber = recipient.accountNumber
                            selectedBank = MockBanks.list.firstOrNull { it.code == recipient.bankCode }
                        },
                    )
                }

                if (transferType == TransferType.Napas) {
                    BankListSection(
                        banks = MockBanks.list,
                        selectedBank = selectedBank,
                        onBankTap = { selectedBank = it },
                    )
                }
            }

            // Sticky bottom button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(STICKY_BUTTON_HEIGHT_DP)
                    .background(IPayTheme.colors.bgNeutralPrimary)
                    .padding(IPayTheme.spacing.s16)
                    .align(Alignment.BottomCenter),
            ) {
                IPayButton(
                    text = "Tiếp tục",
                    onClick = {
                        val bank = selectedBank ?: return@IPayButton
                        onContinue(accountNumber, bank)
                    },
                    enabled = canContinue,
                    variant = IPayButtonVariant.Primary,
                    size = IPayButtonSize.Large,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun AccountNumberSection(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column {
        SectionTitle("Nhập số tài khoản")
        Spacer(Modifier.height(IPayTheme.spacing.s8))
        IPayTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = "VD: 0123456789",
            helperText = "Tối đa 19 chữ số",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RecentRecipientsSection(
    recipients: List<MockRecipient>,
    onRecipientTap: (MockRecipient) -> Unit,
) {
    Column {
        SectionTitle("Gần đây")
        Spacer(Modifier.height(IPayTheme.spacing.s8))
        Row(horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8)) {
            recipients.forEach { recipient ->
                IPayChip(
                    text = recipient.name,
                    onClick = { onRecipientTap(recipient) },
                    variant = IPayChipVariant.Default,
                )
            }
        }
    }
}

@Composable
private fun BankListSection(
    banks: List<MockBank>,
    selectedBank: MockBank?,
    onBankTap: (MockBank) -> Unit,
) {
    Column {
        SectionTitle("Chọn ngân hàng")
        Spacer(Modifier.height(IPayTheme.spacing.s8))
        Column(verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8)) {
            banks.forEach { bank ->
                BankRow(
                    bank = bank,
                    selected = bank.code == selectedBank?.code,
                    onClick = { onBankTap(bank) },
                )
            }
        }
    }
}

@Composable
private fun BankRow(
    bank: MockBank,
    selected: Boolean,
    onClick: () -> Unit,
) {
    IPayCard(
        variant = if (selected) IPayCardVariant.Outlined else IPayCardVariant.Plain,
        modifier = Modifier
            .fillMaxWidth()
            .safeClickable(onSafeClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(IPayTheme.spacing.s40)
                    .clip(IPayTheme.shapes.full)
                    .background(IPayTheme.colors.bgBrandSecondary),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = IPayTheme.colors.iconBrandPrimary,
                    modifier = Modifier.size(IPayTheme.spacing.s24),
                )
            }
            Spacer(Modifier.width(IPayTheme.spacing.s12))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bank.shortName,
                    style = IPayTheme.typography.bodyEmphasizedMedium,
                    color = IPayTheme.colors.textNeutralPrimary,
                )
                Text(
                    text = bank.fullName,
                    style = IPayTheme.typography.bodySmall,
                    color = IPayTheme.colors.textNeutralTertiary,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = IPayTheme.typography.titleSmall,
        color = IPayTheme.colors.textNeutralPrimary,
    )
}

private const val ACCOUNT_NUMBER_MIN_LENGTH = 8
private val STICKY_BUTTON_HEIGHT_DP = androidx.compose.ui.unit.Dp(88f)
