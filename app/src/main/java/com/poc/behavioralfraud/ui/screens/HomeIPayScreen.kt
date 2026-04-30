@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.poc.behavioralfraud.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poc.behavioralfraud.R
import com.poc.behavioralfraud.ui.components.IPayCard
import com.poc.behavioralfraud.ui.components.IPayCardVariant
import com.poc.behavioralfraud.ui.components.IPayNotificationBadge
import com.poc.behavioralfraud.ui.components.safeClickable
import com.poc.behavioralfraud.ui.theme.IPayPalette
import com.poc.behavioralfraud.ui.theme.IPayTheme

/**
 * Home screen — Figma `5JXePCuiqKQFdrjNHNQVbw` node `1:15540` ("Home").
 *
 * Match Figma 100% fidelity (per Van's rule). Layout structure (top to bottom):
 *
 * 1. Background — radial/linear gradient `BG/premium/new` (recreated with Compose Brush)
 * 2. Top bar — VietinBank logo + iPay text + settings/bell icons (translucent pills)
 * 3. Profile section — avatar (60dp ring) + name + Standard membership badge
 * 4. Action grid card — balance + 2 quick chips + 3x2 product icons + "Xem tất cả dịch vụ"
 * 5. Featured section ("Dành riêng cho bạn"): Mass card / Card / 3 charts / Health score
 * 6. Transaction history (HST)
 * 7. Banner row
 * 8. Bottom nav (floating dock — 2 icons + center QR FAB)
 *
 * Sections 5-7 are below-the-fold and added in subsequent commits on this branch.
 * Section 8 (bottom nav) is rendered fixed at bottom over the scrollable content.
 *
 * Production-feel rule: NO enrollment/verification/profile UI strings (test harness
 * surfaces only via Dev Menu). Long-press logo → Dev Menu.
 */
@Composable
fun HomeIPayScreen(
    onNavigateToTransfer: () -> Unit,
    onNavigateToDevPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        // ─── Layer 1: Background gradient ─────────────────────────────────
        HomeBackground()

        // ─── Layer 2: Scrollable content ──────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            HomeTopBar(onLongPressLogo = onNavigateToDevPreview)
            Spacer(Modifier.height(HomeIPay.Sp.S32))

            HomeProfileSection()
            Spacer(Modifier.height(HomeIPay.Sp.S16))

            HomeActionGridCard(onTapTransfer = onNavigateToTransfer)
            Spacer(Modifier.height(HomeIPay.Sp.S24))

            // ─── Below-the-fold ──────────────────────────────────────────
            HomeFeaturedTitle()
            Spacer(Modifier.height(HomeIPay.Sp.S12))

            HomeMassCard()
            Spacer(Modifier.height(HomeIPay.Sp.S12))

            HomeFinancialOverviewCard()
            Spacer(Modifier.height(HomeIPay.Sp.S12))

            HomeChartsCard()
            Spacer(Modifier.height(HomeIPay.Sp.S12))

            HomeHealthScoreCard()
            Spacer(Modifier.height(HomeIPay.Sp.S24))

            HomeTransactionHistorySection()
            Spacer(Modifier.height(HomeIPay.Sp.S24))

            HomeBannerRow()

            // Bottom spacer so floating nav doesn't overlap last content
            Spacer(Modifier.height(HomeIPay.Sp.NavSpacer))
        }

        // ─── Layer 3: Floating bottom nav ─────────────────────────────────
        HomeBottomNav(
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section: Background
// Figma BG/premium/new (1:15541) — premium gradient background
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    // Recreate "premium/new" — light blue top, neutral grey bottom
                    colorStops = arrayOf(
                        0f to IPayPalette.VietinDarkBlue10,      // #DEF1FF top
                        0.30f to IPayPalette.VietinDarkBlue30,    // #76CFFF mid-light
                        1f to IPayPalette.Ink5,                   // #F8FAFC bottom
                    ),
                ),
            ),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Section: Top Bar (Figma 1:15543)
// VietinBank logo (left) + Settings + Bell (right, translucent circles)
// Long-press logo → Dev Menu (replaces explicit "design system" entry).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeTopBar(onLongPressLogo: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(HomeIPay.Size.TopBarHeight)
            .padding(horizontal = HomeIPay.Sp.S16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Logo — VietinBank | iPay Mobile (long-press → Dev Menu)
        Box(
            modifier = Modifier
                .height(HomeIPay.Size.LogoHeight)
                .width(HomeIPay.Size.LogoWidth)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongPressLogo,
                    role = Role.Button,
                ),
        ) {
            Image(
                painter = painterResource(R.drawable.logo_vietin_full),
                contentDescription = "VietinBank iPay Mobile",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S12),
        ) {
            TopBarIconButton(
                iconRes = R.drawable.ic_settings,
                contentDescription = "Settings",
                onClick = { /* POC no-op */ },
            )
            TopBarIconButton(
                iconRes = R.drawable.ic_bell,
                contentDescription = "Notifications",
                onClick = { /* POC no-op */ },
                showBadge = true,
            )
        }
    }
}

@Composable
private fun TopBarIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    showBadge: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(HomeIPay.Size.TopBarPill)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = HomeIPay.Alpha.TopBarPillBg))
            .safeClickable(onSafeClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(HomeIPay.Size.TopBarIcon),
        )
        if (showBadge) {
            Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = (-4).dp, y = 4.dp)) {
                IPayNotificationBadge()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section: Profile (Figma 1:15562)
// Avatar 60dp (4dp brand-light ring) + name + membership pill (gradient bg).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeProfileSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HomeIPay.Sp.S16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S8),
    ) {
        // Avatar ring 60dp with 4dp Vietin Dark Blue 20 border
        Box(
            modifier = Modifier
                .size(HomeIPay.Size.AvatarRing)
                .clip(CircleShape)
                .border(
                    width = HomeIPay.Sp.S4,
                    color = IPayPalette.VietinDarkBlue20,
                    shape = CircleShape,
                ),
        )

        Column(
            modifier = Modifier.padding(start = HomeIPay.Sp.S0),
            verticalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S4),
        ) {
            Text(
                text = HomeIPayConstants.UserName,
                style = IPayTheme.typography.headingExtraSmall,
                color = Color.White,
            )
            MembershipBadge(
                level = HomeIPayConstants.MembershipLevel,
                onClick = { /* POC no-op */ },
            )
        }
    }
}

@Composable
private fun MembershipBadge(level: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0f to IPayPalette.BrandBgLight,
                        0.765f to IPayPalette.VietinDarkBlue10,
                        1f to IPayPalette.BrandBgLight,
                    ),
                ),
            )
            .border(
                width = IPayTheme.stroke.xs,
                color = IPayPalette.VietinDarkBlue10,
                shape = RoundedCornerShape(percent = 50),
            )
            .safeClickable(onSafeClick = onClick, role = Role.Button)
            .padding(start = HomeIPay.Sp.S6, end = HomeIPay.Sp.S4)
            .padding(vertical = HomeIPay.Sp.S2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S4),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_membership_diamond),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(HomeIPay.Size.IconXS),
        )
        Text(
            text = level,
            style = IPayTheme.typography.bodyMedium,
            color = IPayTheme.colors.textNeutralPrimary,
        )
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(HomeIPay.Size.IconS),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section: Action grid card (Figma 1:15582 "Option 6")
// Top: balance card (160dp) overlapping bank-card decoration
// Quick chips row: "Danh mục tài chính" / "QR của tôi" / "Lịch sử giao dịch"
// 3x2 product icon grid
// "Xem tất cả dịch vụ" pill button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeActionGridCard(onTapTransfer: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BalanceCard()
        Spacer(Modifier.height(HomeIPay.Sp.S16))

        FeaturedSectionCard(onTapTransfer = onTapTransfer)
    }
}

@Composable
private fun BalanceCard() {
    // Balance card per Figma — overlaps bank-card decoration to the right.
    // Width 276dp, height 160dp, padding 12px/16dp, white bg, shadow.
    Box(
        modifier = Modifier
            .padding(horizontal = HomeIPay.Sp.S16)
            .height(HomeIPay.Size.BalanceCardHeight)
            .fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .width(HomeIPay.Size.BalanceCardWidth)
                .fillMaxSize()
                .shadow(
                    elevation = HomeIPay.Sp.S12,
                    shape = RoundedCornerShape(HomeIPay.Sp.S12),
                    ambientColor = IPayPalette.ShadowInk10,
                    spotColor = IPayPalette.ShadowInk10,
                )
                .clip(RoundedCornerShape(HomeIPay.Sp.S12))
                .background(Color.White),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = HomeIPay.Sp.S12, vertical = HomeIPay.Sp.S16),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S8),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = HomeIPayConstants.AccountMaskedPrefix,
                                style = IPayTheme.typography.bodyEmphasizedMedium,
                                color = IPayTheme.colors.textNeutralTertiary,
                            )
                            Text(
                                text = HomeIPayConstants.AccountLastFour,
                                style = IPayTheme.typography.bodyMedium,
                                color = IPayTheme.colors.textNeutralTertiary,
                            )
                        }
                        Spacer(Modifier.height(HomeIPay.Sp.S4))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = HomeIPayConstants.BalanceMasked,
                                style = IPayTheme.typography.bodyEmphasizedMedium,
                                color = IPayTheme.colors.textNeutralPrimary,
                            )
                            Spacer(Modifier.width(HomeIPay.Sp.S4))
                            Text(
                                text = "VND",
                                style = IPayTheme.typography.bodyEmphasizedXL,
                                color = IPayTheme.colors.textNeutralPrimary,
                            )
                        }
                    }

                    EyeToggleButton(onClick = { /* POC no-op */ })
                }
            }
        }
    }
}

@Composable
private fun EyeToggleButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(HomeIPay.Size.EyeButton)
            .clip(CircleShape)
            .background(Color(0xFFC1E7FE)) // exact Figma bg color, not in theme
            .safeClickable(onSafeClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_eye_open),
            contentDescription = "Show balance",
            tint = Color.Unspecified,
            modifier = Modifier.size(HomeIPay.Size.IconM),
        )
    }
}

@Composable
private fun FeaturedSectionCard(onTapTransfer: () -> Unit) {
    // Featured section: white-ish bg with rounded top corners, contains:
    // 1. Quick chips row (Danh mục tài chính / QR của tôi / Lịch sử giao dịch)
    // 2. 3x2 product grid with "Xem tất cả dịch vụ" pill below
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = HomeIPay.Sp.S16, topEnd = HomeIPay.Sp.S16))
            .background(IPayPalette.Ink5)
            .padding(top = HomeIPay.Sp.S16, bottom = HomeIPay.Sp.S0),
        verticalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S24),
    ) {
        QuickChipsRow()
        ProductGridCard(onTapTransfer = onTapTransfer)
    }
}

@Composable
private fun QuickChipsRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HomeIPay.Sp.S16),
        horizontalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S12),
    ) {
        QuickChip(
            iconRes = R.drawable.ic_wallet,
            label = "Danh mục tài chính",
            onClick = { /* POC no-op */ },
        )
        QuickChip(
            iconRes = R.drawable.ic_qr,
            label = "QR của tôi",
            onClick = { /* POC no-op */ },
        )
        QuickChip(
            iconRes = R.drawable.ic_history,
            label = "Lịch sử giao dịch",
            onClick = { /* POC no-op */ },
        )
    }
}

@Composable
private fun QuickChip(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(HomeIPay.Size.QuickChipHeight)
            .clip(RoundedCornerShape(percent = 50))
            .background(IPayTheme.colors.buttonSecondaryBg)
            .border(
                width = IPayTheme.stroke.xs,
                color = IPayTheme.colors.buttonSecondaryBorder,
                shape = RoundedCornerShape(percent = 50),
            )
            .safeClickable(onSafeClick = onClick, role = Role.Button)
            .padding(horizontal = HomeIPay.Sp.S16, vertical = HomeIPay.Sp.S12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S8),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(HomeIPay.Size.IconM),
        )
        Text(
            text = label,
            style = IPayTheme.typography.titleSmall,
            color = IPayTheme.colors.buttonSecondaryLabel,
            maxLines = 1,
        )
    }
}

@Composable
private fun ProductGridCard(onTapTransfer: () -> Unit) {
    // White card containing 3x2 grid of product items + "Xem tất cả dịch vụ" pill.
    IPayCard(
        variant = IPayCardVariant.Outlined,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HomeIPay.Sp.S16),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = HomeIPay.Sp.S8,
            vertical = HomeIPay.Sp.S12,
        ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S4)) {
            // Row 1: Chuyển tiền | Thanh toán hóa đơn (-500k badge) | Tiết kiệm
            Row(modifier = Modifier.fillMaxWidth()) {
                ProductItem(
                    iconRes = R.drawable.pd_transfer_part2,  // composite TBD; using main path for now
                    label = "Chuyển tiền\ntrong nước",
                    onClick = onTapTransfer,
                    modifier = Modifier.weight(1f),
                )
                ProductItem(
                    iconRes = R.drawable.pd_billing,
                    label = "Thanh toán\nhóa đơn",
                    onClick = { /* POC no-op */ },
                    modifier = Modifier.weight(1f),
                    badgeText = "-500k",
                )
                ProductItem(
                    iconRes = R.drawable.pd_saving,
                    label = "Tiết kiệm",
                    onClick = { /* POC no-op */ },
                    modifier = Modifier.weight(1f),
                )
            }
            // Row 2: Vay & tín dụng | Đầu tư chứng khoán | Nạp tiền điện thoại
            Row(modifier = Modifier.fillMaxWidth()) {
                ProductItem(
                    iconRes = R.drawable.pd_loan,
                    label = "Dịch vụ vay\n& tín dụng",
                    onClick = { /* POC no-op */ },
                    modifier = Modifier.weight(1f),
                )
                ProductItem(
                    iconRes = R.drawable.pd_stock,
                    label = "Đầu tư\nchứng khoán",
                    onClick = { /* POC no-op */ },
                    modifier = Modifier.weight(1f),
                )
                ProductItem(
                    iconRes = R.drawable.pd_top_up,
                    label = "Nạp tiền điện thoại",
                    onClick = { /* POC no-op */ },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(HomeIPay.Sp.S8))
            // CTA pill — "Xem tất cả dịch vụ"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                ViewAllServicesPill(onClick = { /* POC no-op */ })
            }
        }
    }
}

@Composable
private fun ProductItem(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
) {
    Column(
        modifier = modifier
            .safeClickable(onSafeClick = onClick, role = Role.Button)
            .padding(HomeIPay.Sp.S8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S2),
    ) {
        Box(
            modifier = Modifier
                .size(HomeIPay.Size.ProductIcon)
                .padding(HomeIPay.Sp.S4),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.fillMaxSize(),
            )
            badgeText?.let {
                Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-4).dp)) {
                    DiscountBadge(text = it)
                }
            }
        }
        Text(
            text = label,
            style = IPayTheme.typography.titleSmall,
            color = IPayTheme.colors.textNeutralPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun DiscountBadge(text: String) {
    // Red gradient pill — same as IPayNotificationBadge but with text content
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(HomeIPay.Sp.S12))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        IPayPalette.AISalmon,
                        IPayPalette.VietinRed60,
                    ),
                ),
            )
            .border(
                width = IPayTheme.stroke.xs,
                color = Color.White,
                shape = RoundedCornerShape(HomeIPay.Sp.S12),
            )
            .padding(horizontal = HomeIPay.Sp.S4, vertical = HomeIPay.Sp.S2),
    ) {
        Text(
            text = text,
            style = IPayTheme.typography.labelXS,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun ViewAllServicesPill(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(HomeIPay.Size.PillHeight)
            .clip(RoundedCornerShape(percent = 50))
            .background(IPayTheme.colors.buttonSecondaryBg)
            .border(
                width = IPayTheme.stroke.xs,
                color = IPayTheme.colors.buttonSecondaryBorder,
                shape = RoundedCornerShape(percent = 50),
            )
            .safeClickable(onSafeClick = onClick, role = Role.Button)
            .padding(horizontal = HomeIPay.Sp.S16, vertical = HomeIPay.Sp.S12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Xem tất cả dịch vụ",
            style = IPayTheme.typography.titleSmall,
            color = IPayTheme.colors.buttonSecondaryLabel,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section: Featured "Dành riêng cho bạn" title + Mass card (Figma 1:15585..1:15588)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeFeaturedTitle() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HomeIPay.Sp.S16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Dành riêng cho bạn",
            style = IPayTheme.typography.headingExtraSmall,
            color = IPayTheme.colors.textNeutralPrimary,
        )
        Text(
            text = "Cài đặt",
            style = IPayTheme.typography.bodyEmphasizedMedium,
            color = IPayTheme.colors.textBrandPrimary,
        )
    }
}

/**
 * Mass / Dành riêng cho bạn card — Figma 1:10692.
 *
 * Featured promo card with gradient bg, headline (gradient text), 3 chat bubbles
 * (sender style — top-left sharp), and 2 AI shortcut chips at bottom.
 */
@Composable
private fun HomeMassCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HomeIPay.Sp.S16)
            .clip(RoundedCornerShape(HomeIPay.Sp.S12))
            .background(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to IPayPalette.Ink10,
                        0.30f to IPayPalette.VietinDarkBlue10,
                        0.70f to IPayPalette.Purple10.copy(alpha = 0.5f),
                        1f to IPayPalette.Ink10,
                    ),
                ),
            )
            .border(
                width = IPayTheme.stroke.xs,
                color = IPayPalette.VietinDarkBlue20,
                shape = RoundedCornerShape(HomeIPay.Sp.S12),
            )
            .padding(HomeIPay.Sp.S12),
        verticalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S20),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S16)) {
            // Headline with gradient text — vietinDarkBlue/80 → purple → vietinRed/60
            Text(
                text = "Thẻ tín dụng dành riêng cho hệ GenZ chúng ta đâyyyy 🔥",
                style = IPayTheme.typography.titleLarge.copy(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            IPayPalette.VietinDarkBlue80,
                            IPayPalette.AILabelMiddle,
                            IPayPalette.VietinRed60,
                        ),
                    ),
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            // 3 chat bubbles (sender style — sharp top-left, rounded other 3 corners)
            Column(verticalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S12)) {
                ChatBubbleText(
                    text = "✨ Tada! Biết bạn là dân sành ăn, mê công nghệ và máu du lịch chính hiệu rồi nha!",
                )
                ChatBubbleImage(description = "Thẻ Droppii")
                ChatBubbleText(
                    text = "Tụi mình đã có bí kíp cho bạn nè, 'Vũ khí bí mật' giúp bạn nâng tầm lifestyle, " +
                        "thỏa sức trải nghiệm mà không lo 'đau ví'",
                )
            }
        }

        // AI shortcut chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S12),
        ) {
            AIShortcutChip(text = "Nhanh tay mở thẻ ngay", onClick = { /* POC no-op */ })
            AIShortcutChip(text = "Chat với chuyên gia thẻ", onClick = { /* POC no-op */ })
        }
    }
}

/** Sender chat bubble — sharp top-left, rounded 24dp other corners. */
@Composable
private fun ChatBubbleText(text: String) {
    Box(
        modifier = Modifier
            .clip(
                RoundedCornerShape(
                    topStart = HomeIPay.Sp.S0,
                    topEnd = HomeIPay.Sp.S24,
                    bottomEnd = HomeIPay.Sp.S24,
                    bottomStart = HomeIPay.Sp.S24,
                ),
            )
            .background(Color.White)
            .padding(horizontal = HomeIPay.Sp.S12, vertical = HomeIPay.Sp.S8),
    ) {
        Text(
            text = text,
            style = IPayTheme.typography.bodyMedium,
            color = IPayTheme.colors.textNeutralPrimary,
        )
    }
}

/** Image chat bubble — rounded 20dp (sharp top-left), 222×125 image + description. */
@Composable
private fun ChatBubbleImage(description: String) {
    Column(
        modifier = Modifier
            .clip(
                RoundedCornerShape(
                    topStart = HomeIPay.Sp.S0,
                    topEnd = HomeIPay.Sp.S20,
                    bottomEnd = HomeIPay.Sp.S20,
                    bottomStart = HomeIPay.Sp.S20,
                ),
            )
            .background(Color.White)
            .padding(HomeIPay.Sp.S4),
        verticalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S4),
    ) {
        // Image placeholder — Figma references "image 860" but no SVG export
        // available; using brand-tinted Box at exact 222×125 from spec.
        Box(
            modifier = Modifier
                .width(222.dp)
                .height(125.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = HomeIPay.Sp.S0,
                        topEnd = HomeIPay.Sp.S16,
                        bottomEnd = HomeIPay.Sp.S16,
                        bottomStart = HomeIPay.Sp.S16,
                    ),
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            IPayPalette.VietinDarkBlue30,
                            IPayPalette.AIPurple,
                        ),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HomeIPay.Sp.S8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = description,
                style = IPayTheme.typography.bodySmall,
                color = IPayTheme.colors.textNeutralTertiary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_volume_max),
                contentDescription = null,
                tint = IPayTheme.colors.textNeutralTertiary,
                modifier = Modifier.size(HomeIPay.Size.IconS),
            )
        }
    }
}

/**
 * AI shortcut chip — used in Mass card footer.
 *
 * White bg, brand-cyan border, gradient label (vietinDarkBlue/80 → purple → red),
 * trailing AI sparkle icon.
 */
@Composable
private fun AIShortcutChip(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(HomeIPay.Size.AIChipHeight)
            .clip(RoundedCornerShape(percent = 50))
            .background(IPayPalette.White)
            .border(
                width = IPayTheme.stroke.xs,
                color = IPayPalette.AICyan,
                shape = RoundedCornerShape(percent = 50),
            )
            .safeClickable(onSafeClick = onClick, role = Role.Button)
            .padding(horizontal = HomeIPay.Sp.S16, vertical = HomeIPay.Sp.S8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S8),
    ) {
        Text(
            text = text,
            style = IPayTheme.typography.titleSmall.copy(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        IPayPalette.VietinDarkBlue80,
                        IPayPalette.AILabelMiddle,
                        IPayPalette.VietinRed50,
                    ),
                ),
            ),
            maxLines = 1,
        )
        Icon(
            painter = painterResource(R.drawable.ic_ai_sparkle),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(HomeIPay.Size.IconM),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section: Financial Overview Card "Quản lý tài chính cá nhân" (Figma 1:10807)
// Layout (top to bottom):
//   1. Title row: "Quản lý tài chính cá nhân" + subtitle + chevron icon
//   2. Tooltip badge (right-aligned): black pill "Chi tăng 19% so với tháng 6"
//      with arrow pointing down + connection dot
//   3. Two horizontal bars: red "Chi" 200dp (gradient red40→red60) over blue "Thu"
//      full-width (gradient darkBlue30→darkBlue70). Vertical black divider at x=200.
//   4. Scale labels: 0 / 10 tr / 20 tr / 30 tr (4 evenly spaced)
//   5. Bottom amounts row: Chi icon + "17,390,000 VND" left | Thu icon + "30,290,000 VND" right
//   6. Divider
//   7. Footer: reload icon + timestamp left | "Lên kế hoạch" ghost button right
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeFinancialOverviewCard() {
    IPayCard(
        variant = IPayCardVariant.Outlined,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HomeIPay.Sp.S16),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(HomeIPay.Sp.S12),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S16)) {
            FinanceCardTitleRow()
            FinanceCardChartArea()
            FinanceCardDivider()
            FinanceCardFooter()
        }
    }
}

@Composable
private fun FinanceCardTitleRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S16),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = "Quản lý tài chính cá nhân",
                style = IPayTheme.typography.titleLarge,
                color = IPayTheme.colors.textNeutralPrimary,
            )
            Text(
                text = "Tất cả các tài khoản thanh toán",
                style = IPayTheme.typography.bodyMedium,
                color = IPayTheme.colors.textNeutralTertiary,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_finance_chevron),
            contentDescription = "Mở chi tiết",
            tint = Color.Unspecified,
            modifier = Modifier.size(HomeIPay.Size.IconL),
        )
    }
}

@Composable
private fun FinanceCardChartArea() {
    // Two-bar chart with tooltip badge floating above the Chi bar end (200dp).
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S12),
    ) {
        FinanceTooltipBadge()
        FinanceChartBars()
        FinanceChartScale()
        FinanceAmountsRow()
    }
}

@Composable
private fun FinanceTooltipBadge() {
    // Black pill 213×31 with chip + text, arrow at left=88, dot at left=92
    Box(
        modifier = Modifier
            .width(213.dp)
            .height(45.dp),
    ) {
        // Pill at top
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(IPayPalette.Ink90)
                .padding(start = HomeIPay.Sp.S6, end = HomeIPay.Sp.S8, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S4),
        ) {
            // Inner chip: Ink60 circle with frame icon
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(IPayPalette.Ink60)
                    .padding(2.dp)
                    .size(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_finance_chip),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Text(
                text = "Chi tăng 19% so với tháng 6",
                style = IPayTheme.typography.bodySmall,
                color = Color.White,
            )
        }
        // Arrow pointing down (polygon) — 14×14 at left=88, top=24
        Icon(
            painter = painterResource(R.drawable.ic_polygon_down),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .size(14.dp)
                .offset(x = 88.dp, y = 24.dp),
        )
        // Connection dot — 6×6 at left=92, top=39
        Box(
            modifier = Modifier
                .size(6.dp)
                .offset(x = 92.dp, y = 39.dp)
                .clip(CircleShape)
                .background(IPayPalette.Ink90),
        )
    }
}

@Composable
private fun FinanceChartBars() {
    // Stack: vertical black line (2px, height 104) at x=200 + Chi bar (200×) + Thu bar (full)
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S12)) {
            // Chi bar — gradient red, 200dp wide
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(IPayPalette.VietinRed40, IPayPalette.VietinRed60),
                        ),
                    )
                    .padding(horizontal = HomeIPay.Sp.S12, vertical = HomeIPay.Sp.S8),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "Chi",
                    style = IPayTheme.typography.titleSmall,
                    color = Color.White,
                )
            }
            // Thu bar — gradient blue, full width
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(IPayPalette.VietinDarkBlue30, IPayPalette.VietinDarkBlue70),
                        ),
                    )
                    .padding(horizontal = HomeIPay.Sp.S12, vertical = HomeIPay.Sp.S8),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "Thu",
                    style = IPayTheme.typography.titleSmall,
                    color = Color.White,
                )
            }
        }
        // Black vertical divider 2dp wide at x=200, spanning ~104dp from top
        Box(
            modifier = Modifier
                .offset(x = 200.dp)
                .width(2.dp)
                .height(104.dp)
                .background(IPayPalette.Ink90),
        )
    }
}

@Composable
private fun FinanceChartScale() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        listOf("0", "10 tr", "20 tr", "30 tr").forEach { label ->
            Text(
                text = label,
                style = IPayTheme.typography.labelXS,
                color = IPayTheme.colors.textNeutralTertiary,
            )
        }
    }
}

@Composable
private fun FinanceAmountsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_finance_chi),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "17,390,000 VND",
                style = IPayTheme.typography.bodyMedium,
                color = IPayPalette.Ink95,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_finance_thu),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "30,290,000 VND",
                style = IPayTheme.typography.bodyMedium,
                color = IPayPalette.Ink95,
            )
        }
    }
}

@Composable
private fun FinanceCardDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IPayTheme.stroke.xs)
            .background(IPayTheme.colors.dividerPrimary),
    )
}

@Composable
private fun FinanceCardFooter() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S4),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_reload),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "11:23 - 09/07/2025",
                style = IPayTheme.typography.bodySmall,
                color = IPayTheme.colors.textNeutralSecondary,
            )
        }
        Text(
            text = "Lên kế hoạch",
            style = IPayTheme.typography.titleSmall,
            color = IPayTheme.colors.buttonGhostLabel,
            modifier = Modifier.safeClickable(
                onSafeClick = { /* POC no-op */ },
                role = Role.Button,
            ),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section: 3-charts card (Figma 1:15590, 343×160)
// Three 60dp circular progress charts: 52% / 22% (with alert) / 15%.
// Charts drawn with Compose Canvas — no SVG export needed.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeChartsCard() {
    IPayCard(
        variant = IPayCardVariant.Outlined,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HomeIPay.Sp.S16),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(HomeIPay.Sp.S12),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S20)) {
            Text(
                text = "Kế hoạch tài chính",
                style = IPayTheme.typography.titleLarge,
                color = IPayTheme.colors.textNeutralPrimary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ProgressChart(percent = 0.52f, label = "52%", showAlert = false)
                ProgressChart(percent = 0.22f, label = "22%", showAlert = true)
                ProgressChart(percent = 0.15f, label = "15%", showAlert = false)
            }
        }
    }
}

@Composable
private fun ProgressChart(percent: Float, label: String, showAlert: Boolean) {
    val ringColor = IPayPalette.VietinDarkBlue60
    val trackColor = IPayPalette.Ink20
    val sweepDeg = (360f * percent.coerceIn(0f, 1f))

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S4),
    ) {
        Box(
            modifier = Modifier.size(60.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(52.dp)) {
                val stroke = 6.dp.toPx()
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
                )
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = sweepDeg,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    ),
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S4),
        ) {
            if (showAlert) {
                Box(
                    modifier = Modifier
                        .size(HomeIPay.Size.IconS)
                        .clip(CircleShape)
                        .background(IPayPalette.Orange60),
                )
            }
            Text(
                text = label,
                style = IPayTheme.typography.bodyEmphasizedSmall,
                color = IPayTheme.colors.textNeutralPrimary,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section: Health Score Card (Figma 1:15623, 343×120)
// Title + score "B - Tạm ổn (72 / 100)" + status badge "+3 điểm nữa để lên hạng".
// Background has a decorative vector (Vector 21 visible).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeHealthScoreCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HomeIPay.Sp.S16)
            .height(120.dp)
            .clip(RoundedCornerShape(HomeIPay.Sp.S12))
            .border(
                width = IPayTheme.stroke.xs,
                color = IPayTheme.colors.borderNeutralPrimary,
                shape = RoundedCornerShape(HomeIPay.Sp.S12),
            )
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        IPayPalette.White,
                        IPayPalette.VietinDarkBlue10.copy(alpha = 0.5f),
                    ),
                ),
            )
            .padding(HomeIPay.Sp.S12),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S12),
        ) {
            Text(
                text = "Điểm sức khoẻ tài chính",
                style = IPayTheme.typography.titleLarge,
                color = IPayTheme.colors.textNeutralPrimary,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S8),
            ) {
                Box(
                    modifier = Modifier
                        .size(HomeIPay.Size.IconM)
                        .clip(CircleShape)
                        .background(IPayPalette.GreenIcon),
                )
                Text(
                    text = "B - Tạm ổn (72 / 100)",
                    style = IPayTheme.typography.titleMedium,
                    color = IPayTheme.colors.textNeutralPrimary,
                )
            }
            // Status badge — light green tint pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(IPayPalette.Green10),
            ) {
                Text(
                    text = "+3 điểm nữa để lên hạng",
                    style = IPayTheme.typography.bodyEmphasizedSmall,
                    color = IPayTheme.colors.textSuccess,
                    modifier = Modifier.padding(horizontal = HomeIPay.Sp.S8, vertical = HomeIPay.Sp.S2),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section: Transaction History (Figma 1:15638 "HST", 375×508)
// Header "Lịch sử giao dịch" + "Xem tất cả" link + list of mock transactions.
// Each item: icon + counterparty name + transaction note + amount + date.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeTransactionHistorySection() {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HomeIPay.Sp.S16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Lịch sử giao dịch",
                style = IPayTheme.typography.headingExtraSmall,
                color = IPayTheme.colors.textNeutralPrimary,
            )
            Text(
                text = "Xem tất cả",
                style = IPayTheme.typography.bodyEmphasizedMedium,
                color = IPayTheme.colors.textBrandPrimary,
            )
        }
        Spacer(Modifier.height(HomeIPay.Sp.S12))
        IPayCard(
            variant = IPayCardVariant.Outlined,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HomeIPay.Sp.S16),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(HomeIPay.Sp.S0),
        ) {
            Column {
                MockHomeTransactions.list.forEachIndexed { index, tx ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IPayTheme.stroke.xs)
                                .background(IPayTheme.colors.dividerPrimary),
                        )
                    }
                    HomeTransactionRow(tx)
                }
            }
        }
    }
}

@Composable
private fun HomeTransactionRow(tx: MockHomeTransaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(HomeIPay.Sp.S12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S12),
    ) {
        // Transaction icon — circular bg with first letter (no SVG icons exported)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(IPayPalette.BrandBgLight),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tx.counterparty.take(1),
                style = IPayTheme.typography.titleMedium,
                color = IPayTheme.colors.textBrandPrimary,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.counterparty,
                style = IPayTheme.typography.bodyEmphasizedMedium,
                color = IPayTheme.colors.textNeutralPrimary,
                maxLines = 1,
            )
            Text(
                text = tx.note,
                style = IPayTheme.typography.bodySmall,
                color = IPayTheme.colors.textNeutralTertiary,
                maxLines = 1,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            val sign = if (tx.amountVnd < 0) "-" else "+"
            val color = if (tx.amountVnd < 0) {
                IPayTheme.colors.textNeutralPrimary
            } else {
                IPayTheme.colors.textSuccess
            }
            Text(
                text = "$sign${"%,d".format(kotlin.math.abs(tx.amountVnd))}đ",
                style = IPayTheme.typography.bodyEmphasizedMedium,
                color = color,
                maxLines = 1,
            )
            Text(
                text = tx.dateLabel,
                style = IPayTheme.typography.labelSmall,
                color = IPayTheme.colors.textNeutralTertiary,
                maxLines = 1,
            )
        }
    }
}

private data class MockHomeTransaction(
    val counterparty: String,
    val note: String,
    val amountVnd: Long,
    val dateLabel: String,
)

private object MockHomeTransactions {
    val list = listOf(
        MockHomeTransaction("Nguyễn Văn An", "Chuyển tiền cafe sáng", -45_000L, "Hôm nay"),
        MockHomeTransaction("Lương VietinBank", "Lương tháng 4", 18_500_000L, "Hôm qua"),
        MockHomeTransaction("Grab", "Đặt xe", -68_000L, "2 ngày trước"),
        MockHomeTransaction("Trần Thị Bình", "Trả tiền nhà", -3_500_000L, "3 ngày trước"),
        MockHomeTransaction("Shopee", "Đơn hàng #SH123", -245_000L, "5 ngày trước"),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Section: Banner Row (Figma 1:15639, 343×120)
// Horizontally scrollable banner cards. Figma has 2 instances; second is the
// active variant with text "Hè này Thoải mái vi vu". Image asset not exported
// (would need PNG). Using gradient placeholder that matches summer-vibe palette.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeBannerRow() {
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = HomeIPay.Sp.S16),
        horizontalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S12),
    ) {
        items(HomeBannerData.list) { banner ->
            HomeBannerCard(banner)
        }
    }
}

@Composable
private fun HomeBannerCard(data: HomeBanner) {
    // Banner shape: 343×120 rounded card with gradient bg + decorative overlapping
    // circles that match Figma's "image 351/352" composition layout (raster image
    // PNGs not exported — abstract circles approximate the visual feel).
    Box(
        modifier = Modifier
            .width(343.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(HomeIPay.Sp.S12))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(data.bgStart, data.bgEnd),
                ),
            ),
    ) {
        // Decorative circle 1 — large, top-right offscreen (mimics "image 352" pos)
        Box(
            modifier = Modifier
                .size(258.dp)
                .offset(x = 152.dp, y = (-88).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f)),
        )
        // Decorative circle 2 — medium, mid-right offscreen (mimics "image 351" pos)
        Box(
            modifier = Modifier
                .size(181.dp)
                .offset(x = 162.dp, y = (-15).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
        )

        // Foreground text content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(HomeIPay.Sp.S16),
        ) {
            Text(
                text = data.headline,
                style = IPayTheme.typography.titleLarge,
                color = Color.White,
            )
            Spacer(Modifier.height(HomeIPay.Sp.S4))
            Text(
                text = data.subline,
                style = IPayTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

private data class HomeBanner(
    val headline: String,
    val subline: String,
    val bgStart: Color,
    val bgEnd: Color,
)

private object HomeBannerData {
    val list = listOf(
        HomeBanner(
            headline = "Hè này Thoải mái vi vu",
            subline = "Hoàn 5% chuyến bay nội địa",
            bgStart = IPayPalette.VietinDarkBlue80,
            bgEnd = IPayPalette.AIPurple,
        ),
        HomeBanner(
            headline = "Mở thẻ tín dụng GenZ",
            subline = "Tặng 500K + miễn phí thường niên",
            bgStart = IPayPalette.VietinRed60,
            bgEnd = IPayPalette.AISalmon,
        ),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Section: Bottom Nav (Figma 1:15650)
// Floating dock — 2 icons (left/right) + center QR FAB (gradient) over a
// gradient backdrop fading from white at bottom.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeBottomNav(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(HomeIPay.Size.BottomNavHeight)
            .background(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        0.668f to Color.White.copy(alpha = 0f),
                        1f to Color.White,
                    ),
                ),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .shadow(
                    elevation = HomeIPay.Sp.S8,
                    shape = RoundedCornerShape(percent = 50),
                    ambientColor = IPayPalette.ShadowInk10,
                    spotColor = IPayPalette.ShadowInk10,
                )
                .background(Color.White)
                .padding(horizontal = HomeIPay.Sp.S20, vertical = HomeIPay.Sp.S6),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HomeIPay.Sp.S24),
        ) {
            BottomNavItem(
                iconRes = R.drawable.ic_nav_left,
                contentDescription = "Membership",
                onClick = { /* POC no-op */ },
            )
            BottomNavCenterButton(onClick = { /* POC no-op */ })
            BottomNavItem(
                iconRes = R.drawable.ic_nav_right,
                contentDescription = "Promotions",
                onClick = { /* POC no-op */ },
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(HomeIPay.Size.BottomNavItem)
            .safeClickable(onSafeClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = Color.Unspecified,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun BottomNavCenterButton(onClick: () -> Unit) {
    // 64dp circle with conic gradient (vietinDarkBlue/90 → 50 → 90 sweep).
    // White 4dp border + ink/95_8 shadow.
    Box(
        modifier = Modifier
            .size(HomeIPay.Size.BottomNavCenter)
            .clip(CircleShape)
            .border(
                width = HomeIPay.Sp.S4,
                color = Color.White,
                shape = CircleShape,
            )
            .background(
                // Approximate conic gradient using sweep — Compose has Brush.sweepGradient
                brush = Brush.sweepGradient(
                    colors = listOf(
                        IPayPalette.VietinDarkBlue90,
                        IPayPalette.VietinDarkBlue80,
                        IPayPalette.VietinDarkBlue50,
                        IPayPalette.VietinDarkBlue80,
                        IPayPalette.VietinDarkBlue90,
                    ),
                ),
            )
            .safeClickable(onSafeClick = onClick, role = Role.Button),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Constants
// Mock copy + dimensions. Move to data/mock/ at production-data integration.
// ─────────────────────────────────────────────────────────────────────────────

private object HomeIPayConstants {
    const val UserName = "NGUYEN HOANG TRAN LE"
    const val MembershipLevel = "Standard"
    const val AccountMaskedPrefix = "********"
    const val AccountLastFour = "6690"
    const val BalanceMasked = "✱✱✱ ✱✱✱ ✱✱✱"
}

/** Spacing & size constants matching Figma 1:15540 measurements. */
private object HomeIPay {
    object Sp {
        val S0: Dp = 0.dp
        val S2: Dp = 2.dp
        val S4: Dp = 4.dp
        val S6: Dp = 6.dp
        val S8: Dp = 8.dp
        val S12: Dp = 12.dp
        val S16: Dp = 16.dp
        val S20: Dp = 20.dp
        val S24: Dp = 24.dp
        val S32: Dp = 32.dp
        val NavSpacer: Dp = 120.dp // bottom nav height + gap
    }

    object Size {
        // Top bar
        val TopBarHeight: Dp = 40.dp
        val LogoHeight: Dp = 40.dp
        val LogoWidth: Dp = 97.dp
        val TopBarPill: Dp = 40.dp
        val TopBarIcon: Dp = 24.dp

        // Profile
        val AvatarRing: Dp = 60.dp
        val IconXS: Dp = 14.dp
        val IconS: Dp = 16.dp
        val IconM: Dp = 20.dp
        val IconL: Dp = 24.dp

        // Action grid
        val BalanceCardWidth: Dp = 276.dp
        val BalanceCardHeight: Dp = 160.dp
        val EyeButton: Dp = 40.dp
        val QuickChipHeight: Dp = 40.dp
        val ProductIcon: Dp = 36.dp // icon area incl. padding
        val PillHeight: Dp = 36.dp

        // Bottom nav
        val BottomNavHeight: Dp = 110.dp
        val BottomNavItem: Dp = 40.dp
        val BottomNavCenter: Dp = 64.dp

        // Featured / AI chip
        val AIChipHeight: Dp = 36.dp
    }

    object Alpha {
        const val TopBarPillBg: Float = 0.20f
    }
}
