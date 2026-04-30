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

            // ─── Below-the-fold (added in next commits) ───────────────────
            // TODO(commit 2): Featured section "Dành riêng cho bạn"
            // TODO(commit 3): 3 charts + health score
            // TODO(commit 4): HST (transaction history)
            // TODO(commit 5): Banner row

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
            painter = painterResource(R.drawable.ic_membership_crown),
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
    }

    object Alpha {
        const val TopBarPillBg: Float = 0.20f
    }
}
