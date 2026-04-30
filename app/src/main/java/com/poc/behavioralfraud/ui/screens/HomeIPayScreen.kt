package com.poc.behavioralfraud.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.poc.behavioralfraud.ui.components.IPayCard
import com.poc.behavioralfraud.ui.components.IPayCardVariant
import com.poc.behavioralfraud.ui.components.IPayNotificationBadge
import com.poc.behavioralfraud.ui.components.safeClickable
import com.poc.behavioralfraud.ui.theme.IPayTheme

/**
 * Home — FR-CL-10 REQ-01.
 *
 * Production-feel iPay clone: gradient header + greeting + balance card +
 * 4×2 action grid + promotions row. Replaces the previous POC HomeScreen which
 * exposed enrollment/verification/profile UI; that test harness moves to the
 * Dev Menu (TASK-024).
 *
 * Hidden affordance: long-press the brand logo top-left for 1.5s to navigate
 * to the Dev Menu (currently routed to Design System Preview as a placeholder
 * until TASK-024 lands the real menu).
 *
 * Mock data (balance, action labels, promo cards) lives inline for now and
 * will move to `data/mock/` at TASK-018.
 */
@Composable
fun HomeIPayScreen(
    onNavigateToTransfer: () -> Unit,
    onNavigateToDevPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IPayTheme.colors.bgNeutralSecondary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            HomeHeader(onLogoLongPress = onNavigateToDevPreview)

            // Quick balance card overlaps the header by 20dp (negative offset).
            QuickBalanceCard(
                modifier = Modifier
                    .padding(horizontal = IPayTheme.spacing.s16)
                    .offset(y = -HomeIPayConstants.BALANCE_CARD_OVERLAP),
            )

            Spacer(Modifier.height(IPayTheme.spacing.s8))

            ActionGrid(
                modifier = Modifier.padding(horizontal = IPayTheme.spacing.s16),
                onTransferClick = onNavigateToTransfer,
            )

            Spacer(Modifier.height(IPayTheme.spacing.s24))

            PromotionsSection()

            Spacer(Modifier.height(IPayTheme.spacing.s40))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header — gradient brand background, avatar + greeting + bell badge.
// Long-press logo affordance routes to Dev Menu placeholder.
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeHeader(onLogoLongPress: () -> Unit) {
    val gradient = Brush.verticalGradient(
        listOf(
            IPayTheme.colors.buttonPrimaryBgStart,
            IPayTheme.colors.buttonPrimaryBgEnd,
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HomeIPayConstants.HEADER_HEIGHT)
            .background(gradient),
    ) {
        // Brand logo top-left — long-press 1.5s navigates to Dev Menu.
        // DEVIATION: combinedClickable instead of safeClickable — Compose's
        // safeClickable does not support long-press. This is the documented
        // exception per TASK-017 spec.
        Icon(
            imageVector = Icons.Default.AccountBalance,
            contentDescription = null,
            tint = IPayTheme.colors.textOnColorPrimary,
            modifier = Modifier
                .padding(
                    start = IPayTheme.spacing.s16,
                    top = IPayTheme.spacing.s16,
                )
                .size(HomeIPayConstants.LOGO_SIZE)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLogoLongPress,
                    role = Role.Button,
                ),
        )

        // Greeting row (avatar + greeting + bell + badge).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = IPayTheme.spacing.s16,
                    end = IPayTheme.spacing.s16,
                    top = IPayTheme.spacing.s40 + IPayTheme.spacing.s24,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar placeholder — circle 40dp.
            Box(
                modifier = Modifier
                    .size(HomeIPayConstants.AVATAR_SIZE)
                    .clip(CircleShape)
                    .background(IPayTheme.colors.textOnColorPrimary.copy(alpha = HomeIPayConstants.AVATAR_BG_ALPHA)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = HomeIPayConstants.AVATAR_INITIAL,
                    style = IPayTheme.typography.titleMedium,
                    color = IPayTheme.colors.textOnColorPrimary,
                )
            }

            Spacer(Modifier.width(IPayTheme.spacing.s12))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = HomeIPayConstants.GREETING_PRIMARY,
                    style = IPayTheme.typography.titleMedium,
                    color = IPayTheme.colors.textOnColorPrimary,
                )
                Text(
                    text = HomeIPayConstants.GREETING_SECONDARY,
                    style = IPayTheme.typography.bodySmall,
                    color = IPayTheme.colors.textOnColorPrimary.copy(alpha = HomeIPayConstants.SUBTITLE_ALPHA),
                )
            }

            // Bell + count badge stacked.
            Box {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = IPayTheme.colors.textOnColorPrimary,
                    modifier = Modifier.size(HomeIPayConstants.BELL_SIZE),
                )
                IPayNotificationBadge(
                    count = HomeIPayConstants.UNREAD_NOTIFICATIONS,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(
                            x = IPayTheme.spacing.s4,
                            y = -IPayTheme.spacing.s2,
                        ),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick balance card — overlaps header.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuickBalanceCard(modifier: Modifier = Modifier) {
    IPayCard(
        modifier = modifier.fillMaxWidth(),
        variant = IPayCardVariant.Elevated,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = HomeIPayConstants.BALANCE_AMOUNT,
                    style = IPayTheme.typography.headingSmall,
                    color = IPayTheme.colors.textNeutralPrimary,
                )
                Spacer(Modifier.height(IPayTheme.spacing.s4))
                Text(
                    text = HomeIPayConstants.BALANCE_SUBTITLE,
                    style = IPayTheme.typography.bodySmall,
                    color = IPayTheme.colors.textNeutralTertiary,
                )
            }
            Box(
                modifier = Modifier
                    .size(HomeIPayConstants.HIDE_BALANCE_BUTTON_SIZE)
                    .clip(IPayTheme.shapes.full)
                    .background(IPayTheme.colors.bgNeutralSecondary)
                    .safeClickable(role = Role.Button) { /* decorative */ },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = HomeIPayConstants.HIDE_BALANCE_LABEL,
                    tint = IPayTheme.colors.iconNeutralSecondary,
                    modifier = Modifier.size(IPayTheme.spacing.s20),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Action grid — 4 columns × 2 rows = 8 quick actions.
// "Chuyển tiền trong nước" is the only wired action; others are no-op (POC).
// ─────────────────────────────────────────────────────────────────────────────

private data class HomeAction(
    val label: String,
    val icon: ImageVector,
)

@Composable
private fun ActionGrid(
    modifier: Modifier = Modifier,
    onTransferClick: () -> Unit,
) {
    val actions = listOf(
        HomeAction("Chuyển tiền trong nước", Icons.Default.Send),
        HomeAction("Nạp điện thoại", Icons.Default.PhoneAndroid),
        HomeAction("Thanh toán hóa đơn", Icons.Default.Receipt),
        HomeAction("Tiết kiệm", Icons.Default.Savings),
        HomeAction("Vay tiêu dùng", Icons.Default.RequestQuote),
        HomeAction("Thẻ tín dụng", Icons.Default.CreditCard),
        HomeAction("Đầu tư", Icons.Default.TrendingUp),
        HomeAction("Tất cả", Icons.Default.Apps),
    )

    IPayCard(
        modifier = modifier.fillMaxWidth(),
        variant = IPayCardVariant.Plain,
        contentPadding = PaddingValues(IPayTheme.spacing.s8),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8)) {
            actions.chunked(HomeIPayConstants.ACTION_GRID_COLUMNS).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s4),
                ) {
                    rowItems.forEach { action ->
                        ActionGridItem(
                            action = action,
                            modifier = Modifier.weight(1f),
                            onClick = if (action.label == "Chuyển tiền trong nước") {
                                onTransferClick
                            } else {
                                { /* no-op for POC */ }
                            },
                        )
                    }
                    // Pad partial last row (defensive — currently 8 items = 2 full rows).
                    repeat(HomeIPayConstants.ACTION_GRID_COLUMNS - rowItems.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionGridItem(
    action: HomeAction,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(IPayTheme.shapes.xsmall)
            .safeClickable(role = Role.Button, onSafeClick = onClick)
            .padding(vertical = IPayTheme.spacing.s12, horizontal = IPayTheme.spacing.s4),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8),
        ) {
            Box(
                modifier = Modifier
                    .size(HomeIPayConstants.ACTION_ICON_BG_SIZE)
                    .clip(IPayTheme.shapes.small)
                    .background(IPayTheme.colors.bgBrandSecondary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = IPayTheme.colors.iconBrandPrimary,
                    modifier = Modifier.size(HomeIPayConstants.ACTION_ICON_SIZE),
                )
            }
            Text(
                text = action.label,
                style = IPayTheme.typography.bodySmall,
                color = IPayTheme.colors.textNeutralPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Promotions row — horizontal LazyRow of 3 outlined cards.
// Plain IPayCard Outlined — explicitly NOT IPayAIChip (AIChip is reserved for
// AI-powered features per FR-CL-08 REQ-15).
// ─────────────────────────────────────────────────────────────────────────────

private data class HomePromo(
    val title: String,
    val description: String,
)

@Composable
private fun PromotionsSection() {
    val promos = listOf(
        HomePromo(
            title = "Hoàn 1% chuyển tiền liên ngân hàng",
            description = "Áp dụng đến 31/12 cho mọi giao dịch Napas",
        ),
        HomePromo(
            title = "Mở thẻ tín dụng — Tặng 500K",
            description = "Hoàn tiền khi chi tiêu trong 30 ngày đầu",
        ),
        HomePromo(
            title = "Vay nhanh 24/7",
            description = "Lãi suất từ 0.99% — Duyệt online trong 5 phút",
        ),
    )

    Column {
        Text(
            text = HomeIPayConstants.PROMOTIONS_HEADER,
            style = IPayTheme.typography.titleMedium,
            color = IPayTheme.colors.textNeutralPrimary,
            modifier = Modifier.padding(horizontal = IPayTheme.spacing.s16),
        )
        Spacer(Modifier.height(IPayTheme.spacing.s12))
        LazyRow(
            contentPadding = PaddingValues(horizontal = IPayTheme.spacing.s16),
            horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s12),
        ) {
            items(promos) { promo -> PromoCard(promo = promo) }
        }
    }
}

@Composable
private fun PromoCard(promo: HomePromo) {
    IPayCard(
        modifier = Modifier.width(HomeIPayConstants.PROMO_CARD_WIDTH),
        variant = IPayCardVariant.Outlined,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8)) {
            Text(
                text = promo.title,
                style = IPayTheme.typography.titleSmall,
                color = IPayTheme.colors.textNeutralPrimary,
            )
            Text(
                text = promo.description,
                style = IPayTheme.typography.bodySmall,
                color = IPayTheme.colors.textNeutralSecondary,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Constants — non-design values + inline mock data per FR-CL-10 REQ-01 scope.
// ─────────────────────────────────────────────────────────────────────────────

private object HomeIPayConstants {
    val HEADER_HEIGHT = 200.dp
    val LOGO_SIZE = 32.dp
    val AVATAR_SIZE = 40.dp
    val BELL_SIZE = 24.dp
    val BALANCE_CARD_OVERLAP = 20.dp
    val HIDE_BALANCE_BUTTON_SIZE = 36.dp
    val ACTION_ICON_BG_SIZE = 48.dp
    val ACTION_ICON_SIZE = 24.dp
    val PROMO_CARD_WIDTH = 280.dp

    const val ACTION_GRID_COLUMNS = 4
    const val UNREAD_NOTIFICATIONS = 3

    const val AVATAR_BG_ALPHA = 0.2f
    const val SUBTITLE_ALPHA = 0.85f

    // Inline mock data — to migrate into `data/mock/` at TASK-018.
    const val AVATAR_INITIAL = "V"
    const val GREETING_PRIMARY = "Xin chào, Vandz"
    const val GREETING_SECONDARY = "Chúc bạn một ngày tốt lành"
    const val BALANCE_AMOUNT = "12,345,678 VND"
    const val BALANCE_SUBTITLE = "Tài khoản thanh toán • 9999 1111"
    const val HIDE_BALANCE_LABEL = "Ẩn số dư"
    const val PROMOTIONS_HEADER = "Ưu đãi cho bạn"
}
