package com.poc.behavioralfraud.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.poc.behavioralfraud.ui.theme.IPayTheme

/**
 * Underline tabs — FR-CL-08 REQ-20.
 *
 * Hàng tab bằng nhau (mỗi tab `weight(1f)`). Active tab hiển thị label màu
 * `tabLabelActive`, default `tabLabelDefault`. Indicator dày `stroke.lg` (2dp)
 * trượt theo tab được chọn (animation 240ms), full bottom border.
 *
 * Click tab → [onTabSelected] với index tương ứng (qua [safeClickable]).
 */
@Composable
fun IPayHorizontalTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = IPayTheme.colors
    val safeIndex = selectedIndex.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val totalWidth = maxWidth
        val tabWidth: Dp = if (tabs.isEmpty()) totalWidth else with(LocalDensity.current) {
            (constraints.maxWidth.toFloat() / tabs.size).toDp()
        }

        val indicatorOffset: Dp by animateDpAsState(
            targetValue = tabWidth * safeIndex,
            animationSpec = tween(durationMillis = IPayHorizontalTabsConstants.ANIMATION_MS),
            label = "IPayTabsIndicatorOffset",
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { index, label ->
                    val isActive = index == safeIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .safeClickable(
                                role = Role.Tab,
                                onSafeClick = { onTabSelected(index) },
                            )
                            .padding(
                                horizontal = IPayTheme.spacing.s12,
                                vertical = IPayTheme.spacing.s12,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = if (isActive) {
                                IPayTheme.typography.bodyEmphasizedMedium
                            } else {
                                IPayTheme.typography.bodyMedium
                            },
                            color = if (isActive) colors.tabLabelActive else colors.tabLabelDefault,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // Default underline (full row).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IPayTheme.stroke.lg)
                    .background(colors.tabIndicatorDefault),
            ) {
                // Active indicator slides on top.
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(tabWidth)
                        .height(IPayTheme.stroke.lg)
                        .background(colors.tabIndicatorActive),
                )
            }
        }
    }
}

private object IPayHorizontalTabsConstants {
    const val ANIMATION_MS: Int = 240
}

@Preview(name = "Tabs — 3 items", showBackground = true, widthDp = 360)
@Composable
private fun IPayHorizontalTabsPreview3() {
    IPayTheme {
        Column(modifier = Modifier.padding(IPayTheme.spacing.s16)) {
            IPayHorizontalTabs(
                tabs = listOf("Tài khoản", "Thẻ", "Khoản vay"),
                selectedIndex = 1,
                onTabSelected = {},
            )
        }
    }
}

@Preview(name = "Tabs — 2 items first selected", showBackground = true, widthDp = 360)
@Composable
private fun IPayHorizontalTabsPreview2() {
    IPayTheme {
        Column(modifier = Modifier.padding(IPayTheme.spacing.s16)) {
            IPayHorizontalTabs(
                tabs = listOf("Đang xử lý", "Hoàn tất"),
                selectedIndex = 0,
                onTabSelected = {},
            )
        }
    }
}
