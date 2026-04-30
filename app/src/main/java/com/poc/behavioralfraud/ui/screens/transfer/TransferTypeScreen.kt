package com.poc.behavioralfraud.ui.screens.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.poc.behavioralfraud.ui.components.IPaySelection
import com.poc.behavioralfraud.ui.components.IPaySelectionVariant
import com.poc.behavioralfraud.ui.components.IPayTopBar
import com.poc.behavioralfraud.ui.theme.IPayTheme

/**
 * Transfer type selection — FR-CL-10 REQ-02.
 *
 * Two cards: VietinBank internal vs Napas inter-bank. Tap → navigate to
 * RecipientScreen with [TransferType] arg.
 *
 * Behavioral session is already active here (started by HomeIPayScreen tap;
 * collector wiring lands at TASK-023).
 */
@Composable
fun TransferTypeScreen(
    onTypeSelected: (TransferType) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IPayTheme.colors.bgNeutralSecondary),
    ) {
        IPayTopBar(title = "Chuyển tiền", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(IPayTheme.spacing.s16),
            verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s12),
        ) {
            IPaySelection(
                label = "Chuyển trong VietinBank",
                description = "Phí 0đ • Tức thì",
                selected = false,
                onClick = { onTypeSelected(TransferType.Internal) },
                variant = IPaySelectionVariant.Radio,
                leadingIcon = Icons.Default.AccountBalance,
                modifier = Modifier.fillMaxWidth(),
            )
            IPaySelection(
                label = "Chuyển liên ngân hàng (Napas)",
                description = "Phí 5,500đ • Trong vài phút",
                selected = false,
                onClick = { onTypeSelected(TransferType.Napas) },
                variant = IPaySelectionVariant.Radio,
                leadingIcon = Icons.Default.SwapHoriz,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Transfer channel selected at TransferTypeScreen — passed downstream as nav arg. */
enum class TransferType { Internal, Napas }
