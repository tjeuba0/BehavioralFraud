package com.poc.behavioralfraud.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poc.behavioralfraud.ui.theme.IPayTheme

/**
 * Bottom sheet — FR-CL-08 REQ-17.
 *
 * Skin of Material3 [ModalBottomSheet]:
 *  - Top drag handle: pill 40×4dp, color = `borderNeutralSecondary` (matches Ink30 in default theme).
 *  - Top corners: `IPayShapes.large` (r24).
 *  - Background: `bgNeutralPrimary`.
 *
 * Slot-based: optional [header], required [content], optional [footer]. A 1px divider
 * is drawn before [footer] when [content] precedes it (visual separator).
 *
 * Padding inside the sheet uses `s16` horizontal + `s24` vertical bracketing the slots.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IPayBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    header: @Composable (() -> Unit)? = null,
    footer: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = IPayTheme.colors
    val spacing = IPayTheme.spacing

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = IPayTheme.shapes.large,
        containerColor = colors.bgNeutralPrimary,
        dragHandle = { IPayBottomSheetDragHandle() },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (header != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.s16, vertical = spacing.s8),
                ) {
                    header()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.s16, vertical = spacing.s8),
            ) {
                content()
            }

            if (footer != null) {
                HorizontalDivider(
                    color = colors.dividerPrimary,
                    thickness = IPayTheme.stroke.xs,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.s16, vertical = spacing.s16),
                ) {
                    footer()
                }
            }
        }
    }
}

/**
 * iPay drag handle — pill 40×4dp using `borderNeutralSecondary` token (matches Ink30
 * in the default light theme).
 */
@Composable
private fun IPayBottomSheetDragHandle() {
    val colors = IPayTheme.colors
    val spacing = IPayTheme.spacing
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.s12),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(IPayBottomSheetDefaults.HANDLE_WIDTH)
                .height(IPayBottomSheetDefaults.HANDLE_HEIGHT)
                .clip(IPayTheme.shapes.full)
                .background(colors.borderNeutralSecondary),
        )
    }
}

object IPayBottomSheetDefaults {
    /** Drag handle pill width (40dp per Figma spec REQ-17). */
    val HANDLE_WIDTH: Dp = 40.dp

    /** Drag handle pill height (4dp per Figma spec REQ-17). */
    val HANDLE_HEIGHT: Dp = 4.dp
}

// DEVIATION: HANDLE_WIDTH/HANDLE_HEIGHT use literal dp — these are the exact Figma
// values from REQ-17 (40×4dp) and there is no semantic spacing token for "4dp". They
// live in IPayBottomSheetDefaults so component code references constants only.

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun IPayBottomSheetPreview() {
    // Note: ModalBottomSheet renders as overlay — preview shows host stub only.
    // Real usage: trigger via state in calling screen.
    IPayTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(IPayTheme.colors.bgNeutralPrimary)
                .padding(IPayTheme.spacing.s16),
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .width(IPayBottomSheetDefaults.HANDLE_WIDTH)
                        .height(IPayBottomSheetDefaults.HANDLE_HEIGHT)
                        .clip(IPayTheme.shapes.full)
                        .background(IPayTheme.colors.borderNeutralSecondary),
                )
                Text(
                    text = "Bottom sheet preview (handle only — real sheet is overlay)",
                    style = IPayTheme.typography.bodyMedium,
                    color = IPayTheme.colors.textNeutralSecondary,
                    modifier = Modifier.padding(top = IPayTheme.spacing.s16),
                )
            }
        }
    }
}
