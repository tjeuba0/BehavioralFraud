package com.poc.behavioralfraud.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.poc.behavioralfraud.ui.theme.IPayTheme

/**
 * Alert banner — FR-CL-08 REQ-16.
 *
 * Variants: [IPayAlertVariant.Info] / [IPayAlertVariant.Warning] / [IPayAlertVariant.Success].
 *
 * Container radius `r16` (`IPayShapes.small`), padding `s16`, leading icon, body
 * text column (optional [title]), optional close action on the right.
 *
 * **POC compromise**: warning + success use `alertInfoBg` for background while their
 * border + icon switch to semantic warning/success tokens. Dedicated
 * `alertWarning*` / `alertSuccess*` semantic tokens TBD when Figma exposes them.
 */
@Composable
fun IPayAlertBanner(
    text: String,
    modifier: Modifier = Modifier,
    variant: IPayAlertVariant = IPayAlertVariant.Info,
    title: String? = null,
    onClose: (() -> Unit)? = null,
) {
    val spec = variant.spec()
    val spacing = IPayTheme.spacing
    val typography = IPayTheme.typography

    Row(
        modifier = modifier
            .clip(IPayTheme.shapes.small)
            .background(spec.bg)
            .border(width = IPayTheme.stroke.s, brush = SolidColor(spec.border), shape = IPayTheme.shapes.small)
            .padding(spacing.s16),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = spec.icon,
            contentDescription = null,
            tint = spec.iconTint,
            modifier = Modifier.size(spacing.s20),
        )
        Spacer(Modifier.width(spacing.s12))

        Column(modifier = Modifier.weight(1f)) {
            if (title != null) {
                Text(
                    text = title,
                    style = typography.bodyEmphasizedMedium,
                    color = spec.label,
                )
                Spacer(Modifier.size(spacing.s4))
            }
            Text(
                text = text,
                style = typography.bodySmall,
                color = spec.label,
            )
        }

        if (onClose != null) {
            Spacer(Modifier.width(spacing.s12))
            Box(
                modifier = Modifier
                    .size(spacing.s20)
                    .safeClickable(role = Role.Button, onSafeClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = spec.iconTint,
                    modifier = Modifier.size(spacing.s16),
                )
            }
        }
    }
}

enum class IPayAlertVariant { Info, Warning, Success }

/** Resolved color/icon set for a given alert variant. */
private data class IPayAlertSpec(
    val bg: Color,
    val border: Color,
    val icon: ImageVector,
    val iconTint: Color,
    val label: Color,
)

@Composable
private fun IPayAlertVariant.spec(): IPayAlertSpec {
    val c = IPayTheme.colors
    return when (this) {
        IPayAlertVariant.Info -> IPayAlertSpec(
            bg = c.alertInfoBg,
            border = c.alertInfoBorder,
            icon = Icons.Default.Info,
            iconTint = c.alertInfoIcon,
            label = c.alertInfoLabel,
        )
        IPayAlertVariant.Warning -> IPayAlertSpec(
            bg = c.alertInfoBg,
            border = c.iconWarning,
            icon = Icons.Default.Warning,
            iconTint = c.iconWarning,
            label = c.textNeutralPrimary,
        )
        IPayAlertVariant.Success -> IPayAlertSpec(
            bg = c.alertInfoBg,
            border = c.iconSuccess,
            icon = Icons.Default.CheckCircle,
            iconTint = c.iconSuccess,
            label = c.textNeutralPrimary,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun IPayAlertBannerInfoPreview() {
    IPayTheme {
        IPayAlertBanner(
            text = "Phiên giao dịch sẽ hết hạn sau 5 phút.",
            title = "Thông báo",
            variant = IPayAlertVariant.Info,
            onClose = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IPayAlertBannerWarningPreview() {
    IPayTheme {
        IPayAlertBanner(
            text = "Vui lòng kiểm tra lại số tài khoản người nhận.",
            variant = IPayAlertVariant.Warning,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IPayAlertBannerSuccessPreview() {
    IPayTheme {
        IPayAlertBanner(
            text = "Giao dịch đã được xác nhận thành công.",
            title = "Thành công",
            variant = IPayAlertVariant.Success,
            onClose = {},
        )
    }
}
