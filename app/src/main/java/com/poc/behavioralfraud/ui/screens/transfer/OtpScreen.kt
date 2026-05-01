package com.poc.behavioralfraud.ui.screens.transfer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poc.behavioralfraud.R
import com.poc.behavioralfraud.data.mock.MockBank
import com.poc.behavioralfraud.data.mock.MockBanks
import com.poc.behavioralfraud.ui.components.IPayButton
import com.poc.behavioralfraud.ui.components.IPayButtonSize
import com.poc.behavioralfraud.ui.components.IPayButtonVariant
import com.poc.behavioralfraud.ui.components.IPayScreenBackground
import com.poc.behavioralfraud.ui.components.IPayTopBar
import com.poc.behavioralfraud.ui.components.IPayTopBarVariant
import com.poc.behavioralfraud.ui.theme.IPayPalette
import com.poc.behavioralfraud.ui.theme.IPayTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Transaction confirmation + Soft OTP screen — Figma `1:15801` "MH 05".
 *
 * Layout:
 *   1. Top bar — back + "Xác nhận giao dịch"
 *   2. Big confirmation card showing source → destination + amount + note
 *   3. Soft OTP card — label + 8-digit OTP display + countdown circle
 *   4. Primary "Xác nhận & Hoàn tất" button
 *   5. Footnote
 *
 * Note: Figma shows Soft OTP DISPLAYED (not user-entered). User reads OTP
 * from token app and confirms. POC mock generates a random 8-digit code for
 * display realism. Tap confirm → emits [TransferEvent.NavigateToSuccess].
 */
@Composable
fun OtpScreen(
    viewModel: TransferOrchestratorViewModel,
    onNavigateToSuccess: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // POC mock OTP code (8 digits per Figma)
    val mockOtp = remember { generateMockOtp() }

    // Countdown 60 seconds — POC visual only (no auto-refresh trigger)
    var secondsLeft by remember { mutableIntStateOf(OTP_COUNTDOWN_SECONDS) }
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1_000L)
            secondsLeft -= 1
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event == TransferEvent.NavigateToSuccess) onNavigateToSuccess()
        }
    }

    IPayScreenBackground(modifier = modifier) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        IPayTopBar(
            title = "Xác nhận giao dịch",
            variant = IPayTopBarVariant.Transparent,
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = IPayTheme.spacing.s16),
            verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s16),
        ) {
            Spacer(Modifier.height(IPayTheme.spacing.s8))
            ConfirmationCard(
                sourceName = "NGUYEN HOANG TRAN LE",
                sourceAccount = "32902481933/LUNA",
                destName = "NGUYEN DAO TRUONG AN",
                destAccount = state.recipientAccount.ifEmpty { "107001755352" },
                destBank = state.recipientBank ?: MockBanks.list.first { it.code == "VCB" },
                destBranch = "CN KINH BAC",
                amountVnd = state.amountVnd.coerceAtLeast(5_000_000L),
                noteText = state.note.ifEmpty { "NGUYEN TRAN LE chuyen tien" },
            )

            Spacer(Modifier.weight(1f))

            SoftOtpCard(otp = mockOtp, secondsLeft = secondsLeft)

            IPayButton(
                text = "Xác nhận & Hoàn tất",
                onClick = { scope.launch { viewModel.onOtpComplete() } },
                variant = IPayButtonVariant.Primary,
                size = IPayButtonSize.Large,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "Vui lòng kiểm tra kỹ thông tin trước khi giao dịch.",
                style = IPayTheme.typography.bodySmall,
                color = IPayTheme.colors.textNeutralTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(IPayTheme.spacing.s16))
        }
    }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Confirmation card — source → destination summary
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ConfirmationCard(
    sourceName: String,
    sourceAccount: String,
    destName: String,
    destAccount: String,
    destBank: MockBank,
    destBranch: String,
    amountVnd: Long,
    noteText: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = IPayTheme.spacing.s4,
                shape = RoundedCornerShape(16.dp),
                ambientColor = IPayPalette.ShadowInk10,
                spotColor = IPayPalette.ShadowInk10,
            )
            .clip(RoundedCornerShape(16.dp))
            .background(IPayTheme.colors.bgNeutralPrimary)
            .padding(IPayTheme.spacing.s16),
        verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s12),
    ) {
        // Source row
        AccountRow(
            logoRes = R.drawable.logo_bank_vietinbank,
            name = sourceName,
            sub = sourceAccount,
        )

        // Divider with transfer-arrow chevrons in middle
        TransferArrowDivider()

        // Destination row
        AccountRow(
            logoRes = bankLogoRes(destBank.code),
            name = destName,
            sub = "$destAccount  •  ${destBank.shortName}",
            extraSub = destBranch,
        )

        // Spacer divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IPayTheme.stroke.xs)
                .background(IPayTheme.colors.dividerPrimary),
        )

        // Amount row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Số tiền",
                style = IPayTheme.typography.bodyMedium,
                color = IPayTheme.colors.textNeutralTertiary,
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${"%,d".format(amountVnd)} VND",
                    style = IPayTheme.typography.titleLarge,
                    color = IPayTheme.colors.textBrandPrimary,
                )
                Text(
                    text = vietnameseAmountWords(amountVnd),
                    style = IPayTheme.typography.bodySmall,
                    color = IPayTheme.colors.textNeutralTertiary,
                )
            }
        }

        // Note row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Nội dung",
                style = IPayTheme.typography.bodyMedium,
                color = IPayTheme.colors.textNeutralTertiary,
            )
            Text(
                text = noteText,
                style = IPayTheme.typography.bodyEmphasizedMedium,
                color = IPayTheme.colors.textNeutralPrimary,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(start = IPayTheme.spacing.s24),
            )
        }
    }
}

@Composable
private fun AccountRow(
    logoRes: Int,
    name: String,
    sub: String,
    extraSub: String? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s12),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(IPayPalette.BrandBgLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(logoRes),
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.Unspecified,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = IPayTheme.typography.bodyEmphasizedLarge,
                color = IPayTheme.colors.textNeutralPrimary,
                maxLines = 1,
            )
            Text(
                text = sub,
                style = IPayTheme.typography.bodyMedium,
                color = IPayTheme.colors.textNeutralTertiary,
                maxLines = 1,
            )
            if (extraSub != null) {
                Text(
                    text = extraSub,
                    style = IPayTheme.typography.bodyMedium,
                    color = IPayTheme.colors.textNeutralTertiary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TransferArrowDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(IPayTheme.stroke.xs)
                .background(IPayTheme.colors.dividerPrimary),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = IPayTheme.spacing.s8),
        ) {
            // Two chevrons-down stacked tightly for "transfer downward" feel
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = IPayTheme.colors.iconBrandPrimary,
                modifier = Modifier.size(20.dp),
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = IPayTheme.colors.iconBrandPrimary,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = (-12).dp.coerceAtLeast(0.dp)),
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(IPayTheme.stroke.xs)
                .background(IPayTheme.colors.dividerPrimary),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Soft OTP card — Ink5 bg + 8-digit OTP + circular countdown indicator
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SoftOtpCard(otp: String, secondsLeft: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(IPayPalette.Ink5)
            .padding(IPayTheme.spacing.s16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Mã Soft OTP",
                style = IPayTheme.typography.bodyMedium,
                color = IPayTheme.colors.textNeutralTertiary,
            )
            Spacer(Modifier.height(IPayTheme.spacing.s4))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                otp.forEach { digit ->
                    Text(
                        text = digit.toString(),
                        style = IPayTheme.typography.headingSmall.copy(
                            fontSize = 24.sp,
                        ),
                        color = IPayTheme.colors.textNeutralPrimary,
                    )
                }
            }
        }
        CountdownIndicator(secondsLeft = secondsLeft, totalSeconds = OTP_COUNTDOWN_SECONDS)
    }
}

@Composable
private fun CountdownIndicator(secondsLeft: Int, totalSeconds: Int) {
    val ringColor = IPayTheme.colors.borderBrandPrimary
    val trackColor = IPayPalette.VietinDarkBlue20
    val progress = (secondsLeft.toFloat() / totalSeconds).coerceIn(0f, 1f)

    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 3.dp.toPx()
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke),
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            text = secondsLeft.toString(),
            style = IPayTheme.typography.bodyEmphasizedMedium,
            color = IPayTheme.colors.textBrandPrimary,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private const val OTP_COUNTDOWN_SECONDS = 60

private fun generateMockOtp(): String =
    (1..8).joinToString("") { (0..9).random().toString() }

private fun bankLogoRes(code: String): Int = when (code) {
    "CTG" -> R.drawable.logo_bank_vietinbank
    "AGR" -> R.drawable.logo_bank_agribank
    "BIDV" -> R.drawable.logo_bank_bidv
    "VCB" -> R.drawable.logo_bank_vietcombank
    "MBB" -> R.drawable.logo_bank_mb
    "TCB" -> R.drawable.logo_bank_techcombank
    else -> R.drawable.pd_new_recipient
}

private fun vietnameseAmountWords(amount: Long): String = when {
    amount <= 0L -> ""
    amount < 1_000L -> "$amount đồng"
    amount < 1_000_000L -> "${amount / 1_000} nghìn đồng"
    amount < 1_000_000_000L -> "${amount / 1_000_000} triệu đồng"
    else -> "${amount / 1_000_000_000} tỷ đồng"
}
