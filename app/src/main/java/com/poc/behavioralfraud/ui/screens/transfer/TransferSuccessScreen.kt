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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
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
import com.poc.behavioralfraud.ui.components.safeClickable
import com.poc.behavioralfraud.ui.theme.IPayPalette
import com.poc.behavioralfraud.ui.theme.IPayTheme

/**
 * Transfer success screen — Figma `1:15803` "MH 06. Thành công".
 *
 * Layout (top to bottom):
 *   1. Top app bar — headphone (support, left) + VietinBank centered logo +
 *      home icon (right). Light translucent.
 *   2. Big white success card:
 *      - Green check icon with glow (concentric circles)
 *      - "Giao dịch đã được ghi nhận" (brand color)
 *      - Big amount "5,000,000 VND"
 *      - Date + transaction code
 *      - Divider with chevron-down
 *      - Recipient row: bank logo + name + account + bank full name + branch
 *      - Note row
 *      - "Thu gọn" pill toggle
 *   3. Below card: "Chia sẻ" / "Lưu ảnh" icon buttons (2 columns)
 *   4. Bottom buttons:
 *      - Primary "Giao dịch mới"
 *      - Secondary "Gắn thẻ giao dịch"
 *
 * Production-feel rule (per memory): NO risk score / verification UI displayed.
 * Behavioral verification result lives silently in DataStore + Dev Menu.
 */
@Composable
fun TransferSuccessScreen(
    viewModel: TransferOrchestratorViewModel,
    onNavigateHome: () -> Unit,
    onNavigateNewTransfer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    IPayScreenBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            SuccessTopBar(onHome = onNavigateHome)
            Spacer(Modifier.height(SuccessLayout.GAP_S))

            SuccessCard(
                amountVnd = state.amountVnd.coerceAtLeast(5_000_000L),
                txTimeLabel = "17/04/2025 09:56",
                txCode = "124S2561D6T6C4PD",
                recipientName = "NGUYEN DAO TRUONG AN",
                recipientAccount = state.recipientAccount.ifEmpty { "9704322345" },
                recipientBank = state.recipientBank ?: MockBanks.list.first { it.code == "VCB" },
                branchName = "CN KINH BAC",
                noteText = state.note.ifEmpty { "NGUYEN TRAN HOANG LE chuyen tien" },
            )
            Spacer(Modifier.height(SuccessLayout.GAP_M))

            ShareSaveRow()
            Spacer(Modifier.weight(1f))

            BottomActionButtons(
                onNewTransfer = onNavigateNewTransfer,
                onTagTransaction = { /* POC no-op */ },
            )
            Spacer(Modifier.height(SuccessLayout.GAP_M))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar — headphone + VietinBank logo + home icon
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SuccessTopBar(onHome: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = SuccessLayout.HORIZ_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .safeClickable(
                    onSafeClick = { /* POC no-op */ },
                    role = Role.Button,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Headphones,
                contentDescription = "Hỗ trợ",
                tint = IPayTheme.colors.textBrandPrimary,
                modifier = Modifier.size(24.dp),
            )
        }

        // Centered VietinBank logo
        Icon(
            painter = painterResource(R.drawable.logo_vietin_full),
            contentDescription = "VietinBank",
            tint = Color.Unspecified,
            modifier = Modifier.height(28.dp),
        )

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .safeClickable(onSafeClick = onHome, role = Role.Button),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Trang chủ",
                tint = IPayTheme.colors.textBrandPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Success card — green check + amount + tx info + recipient + note
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SuccessCard(
    amountVnd: Long,
    txTimeLabel: String,
    txCode: String,
    recipientName: String,
    recipientAccount: String,
    recipientBank: MockBank,
    branchName: String,
    noteText: String,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = SuccessLayout.HORIZ_PADDING)
            .fillMaxWidth()
            .shadow(
                elevation = IPayTheme.spacing.s8,
                shape = RoundedCornerShape(20.dp),
                ambientColor = IPayPalette.ShadowInk10,
                spotColor = IPayPalette.ShadowInk10,
            )
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(IPayTheme.spacing.s24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s16),
    ) {
        // Big green check with glow
        SuccessCheckGlow()

        Text(
            text = "Giao dịch đã được ghi nhận",
            style = IPayTheme.typography.titleLarge,
            color = IPayTheme.colors.textBrandPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "${"%,d".format(amountVnd)} VND",
            style = IPayTheme.typography.headingLarge.copy(fontSize = 28.sp),
            color = IPayTheme.colors.textNeutralPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "$txTimeLabel  •  $txCode",
            style = IPayTheme.typography.bodySmall,
            color = IPayTheme.colors.textNeutralTertiary,
            textAlign = TextAlign.Center,
        )

        // Divider with chevron in middle
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IPayTheme.stroke.xs)
                    .background(IPayTheme.colors.dividerPrimary)
                    .align(Alignment.Center),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.White)
                    .padding(horizontal = IPayTheme.spacing.s8),
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = IPayTheme.colors.iconBrandPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // Recipient row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
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
                    painter = painterResource(bankLogoForCode(recipientBank.code)),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipientName,
                    style = IPayTheme.typography.bodyEmphasizedLarge,
                    color = IPayTheme.colors.textNeutralPrimary,
                )
                Text(
                    text = recipientAccount,
                    style = IPayTheme.typography.bodyMedium,
                    color = IPayTheme.colors.textNeutralTertiary,
                )
                Text(
                    text = "${recipientBank.fullName} - $branchName",
                    style = IPayTheme.typography.bodyMedium,
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

        // Thu gọn pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .border(
                    width = IPayTheme.stroke.xs,
                    color = IPayTheme.colors.borderBrandSecondary,
                    shape = RoundedCornerShape(percent = 50),
                )
                .background(Color.White)
                .safeClickable(
                    onSafeClick = { /* POC no-op (collapse toggle) */ },
                    role = Role.Button,
                )
                .padding(horizontal = IPayTheme.spacing.s16, vertical = IPayTheme.spacing.s4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s4),
        ) {
            Text(
                text = "Thu gọn",
                style = IPayTheme.typography.bodyEmphasizedMedium,
                color = IPayTheme.colors.textBrandPrimary,
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = IPayTheme.colors.iconBrandPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun SuccessCheckGlow() {
    Box(
        modifier = Modifier.size(100.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2
            // Outermost glow ring (alpha 0.05)
            drawCircle(
                color = Color(0xFF11A84B).copy(alpha = 0.06f),
                radius = size.minDimension / 2,
                center = androidx.compose.ui.geometry.Offset(cx, cy),
            )
            // Mid glow (alpha 0.18)
            drawCircle(
                color = Color(0xFF11A84B).copy(alpha = 0.18f),
                radius = size.minDimension / 2.6f,
                center = androidx.compose.ui.geometry.Offset(cx, cy),
            )
            // Inner solid green (success color)
            drawCircle(
                color = Color(0xFF1ACB5E),
                radius = size.minDimension / 4f,
                center = androidx.compose.ui.geometry.Offset(cx, cy),
            )
        }
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Share / Save row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ShareSaveRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SuccessLayout.HORIZ_PADDING),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        IconLabelButton(
            icon = Icons.Default.Share,
            label = "Chia sẻ",
            onClick = { /* POC no-op */ },
        )
        IconLabelButton(
            icon = Icons.Default.SaveAlt,
            label = "Lưu ảnh",
            onClick = { /* POC no-op */ },
        )
    }
}

@Composable
private fun IconLabelButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.safeClickable(onSafeClick = onClick, role = Role.Button),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s4),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = IPayTheme.colors.iconBrandPrimary,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = IPayTheme.typography.titleSmall,
            color = IPayTheme.colors.textBrandPrimary,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom action buttons
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BottomActionButtons(
    onNewTransfer: () -> Unit,
    onTagTransaction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SuccessLayout.HORIZ_PADDING),
        verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8),
    ) {
        IPayButton(
            text = "Giao dịch mới",
            onClick = onNewTransfer,
            variant = IPayButtonVariant.Primary,
            size = IPayButtonSize.Large,
            modifier = Modifier.fillMaxWidth(),
        )
        IPayButton(
            text = "Gắn thẻ giao dịch",
            onClick = onTagTransaction,
            variant = IPayButtonVariant.Secondary,
            size = IPayButtonSize.Large,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun bankLogoForCode(code: String): Int = when (code) {
    "CTG" -> R.drawable.logo_bank_vietinbank
    "AGR" -> R.drawable.logo_bank_agribank
    "BIDV" -> R.drawable.logo_bank_bidv
    "VCB" -> R.drawable.logo_bank_vietcombank
    "MBB" -> R.drawable.logo_bank_mb
    "TCB" -> R.drawable.logo_bank_techcombank
    else -> R.drawable.pd_new_recipient
}

private object SuccessLayout {
    val HORIZ_PADDING: Dp = 16.dp
    val GAP_S: Dp = 16.dp
    val GAP_M: Dp = 24.dp
}
