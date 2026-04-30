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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poc.behavioralfraud.data.mock.MockTransferLimits
import com.poc.behavioralfraud.ui.components.IPayAlertBanner
import com.poc.behavioralfraud.ui.components.IPayAlertVariant
import com.poc.behavioralfraud.ui.components.IPayButton
import com.poc.behavioralfraud.ui.components.IPayButtonSize
import com.poc.behavioralfraud.ui.components.IPayButtonVariant
import com.poc.behavioralfraud.ui.components.IPayCard
import com.poc.behavioralfraud.ui.components.IPayCardVariant
import com.poc.behavioralfraud.ui.components.IPaySelection
import com.poc.behavioralfraud.ui.components.IPaySelectionVariant
import com.poc.behavioralfraud.ui.components.IPayTextField
import com.poc.behavioralfraud.ui.components.IPayTopBar
import com.poc.behavioralfraud.ui.theme.IPayTheme
import kotlinx.coroutines.launch

/**
 * Transfer form — FR-CL-10 REQ-04.
 *
 * Sticky top: recipient summary card (account + bank from previous screen).
 * Form: amount (thousand-separator) + note + source selector.
 * AlertBanner shows remaining Napas limit (when transferType == Napas).
 * Sticky bottom: "Tiếp tục" button — emits [TransferEvent.NavigateToOtp] or
 * [TransferEvent.ShowOverLimitSheet] via VM.
 *
 * Reads/writes state via [TransferOrchestratorViewModel]. State persists
 * across configuration changes.
 */
@Composable
fun TransferFormScreen(
    viewModel: TransferOrchestratorViewModel,
    onNavigateToOtp: () -> Unit,
    onShowOverLimitSheet: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Collect one-shot events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                TransferEvent.NavigateToOtp -> onNavigateToOtp()
                TransferEvent.ShowOverLimitSheet -> onShowOverLimitSheet()
                TransferEvent.NavigateToSuccess,
                is TransferEvent.ShowError -> {
                    // Form screen does not handle these; OTP screen does.
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IPayTheme.colors.bgNeutralSecondary),
    ) {
        IPayTopBar(title = "Khởi tạo chuyển tiền", onBack = onBack)

        // Sticky recipient card (top)
        RecipientSummaryCard(state)

        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(IPayTheme.spacing.s16)
                    .padding(bottom = STICKY_BUTTON_HEIGHT),
                verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s24),
            ) {
                AmountField(
                    rawDigits = state.amountRaw,
                    onValueChange = viewModel::setAmount,
                )

                NoteField(
                    value = state.note,
                    onValueChange = viewModel::setNote,
                )

                SourceSelector(
                    selected = state.source,
                    onSelected = viewModel::setSource,
                )

                if (state.transferType == TransferType.Napas) {
                    NapasLimitBanner(amountVnd = state.amountVnd, overLimit = state.overLimit)
                }
            }

            // Sticky bottom continue button
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
}

@Composable
private fun RecipientSummaryCard(state: TransferState) {
    IPayCard(
        variant = IPayCardVariant.Plain,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = IPayTheme.spacing.s16, vertical = IPayTheme.spacing.s8),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(IPayTheme.spacing.s40)
                    .clip(IPayTheme.shapes.full)
                    .background(IPayTheme.colors.bgBrandSecondary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = IPayTheme.colors.iconBrandPrimary,
                    modifier = Modifier.size(IPayTheme.spacing.s24),
                )
            }
            Spacer(Modifier.width(IPayTheme.spacing.s12))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.recipientAccount.ifEmpty { "—" },
                    style = IPayTheme.typography.bodyEmphasizedMedium,
                    color = IPayTheme.colors.textNeutralPrimary,
                )
                Text(
                    text = state.recipientBank?.shortName ?: "—",
                    style = IPayTheme.typography.bodySmall,
                    color = IPayTheme.colors.textNeutralTertiary,
                )
            }
        }
    }
}

@Composable
private fun AmountField(
    rawDigits: String,
    onValueChange: (String) -> Unit,
) {
    IPayTextField(
        value = rawDigits,
        onValueChange = onValueChange,
        label = "Số tiền (VND)",
        placeholder = "0",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = remember { ThousandSeparatorVisualTransformation() },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NoteField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    IPayTextField(
        value = value,
        onValueChange = onValueChange,
        label = "Nội dung",
        placeholder = "Nội dung chuyển khoản",
        helperText = "Tối đa 100 ký tự",
        singleLine = false,
        maxLines = 3,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SourceSelector(
    selected: TransferSource,
    onSelected: (TransferSource) -> Unit,
) {
    Column {
        Text(
            text = "Nguồn tiền",
            style = IPayTheme.typography.titleSmall,
            color = IPayTheme.colors.textNeutralPrimary,
        )
        Spacer(Modifier.height(IPayTheme.spacing.s8))
        Column(verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8)) {
            TransferSource.entries.forEach { source ->
                IPaySelection(
                    label = source.label,
                    description = source.maskedAccount,
                    selected = source == selected,
                    onClick = { onSelected(source) },
                    variant = IPaySelectionVariant.Radio,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun NapasLimitBanner(amountVnd: Long, overLimit: Boolean) {
    if (overLimit) {
        IPayAlertBanner(
            text = "Số tiền vượt hạn mức Napas (10,000,000đ/giao dịch). Bạn có thể chuyển bằng kênh thường.",
            variant = IPayAlertVariant.Warning,
            title = "Vượt hạn mức Napas",
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        val remaining = MockTransferLimits.NAPAS_DAILY_LIMIT_VND - amountVnd
        IPayAlertBanner(
            text = "Hạn mức Napas còn ${formatVnd(remaining)}đ cho giao dịch này.",
            variant = IPayAlertVariant.Info,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun formatVnd(amount: Long): String =
    "%,d".format(amount)

private val STICKY_BUTTON_HEIGHT: Dp = 88.dp
