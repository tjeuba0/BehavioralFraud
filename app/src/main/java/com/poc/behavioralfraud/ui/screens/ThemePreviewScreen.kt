package com.poc.behavioralfraud.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.poc.behavioralfraud.ui.theme.IPayTheme
import com.poc.behavioralfraud.ui.theme.IPayThemeSpec
import com.poc.behavioralfraud.ui.theme.IPayThemes

/**
 * Temporary screen — proves theme architecture works (TASK-010 done-when):
 *  - Runtime swap: cycle button changes root spec; all tokens recompose in 1 frame
 *  - Nested override: bottom card always renders under `IPayThemes.Demo` regardless
 *    of root spec — verifies subtree scope works
 *  - Material3 mapping: [Snackbar] auto-picks brand colors from spec
 *
 * To be replaced/folded into `DesignSystemPreviewScreen` at TASK-014.
 */
@Composable
fun ThemePreviewScreen(onBack: () -> Unit) {
    var currentSpec by remember { mutableStateOf(NamedSpec.Default) }

    IPayTheme(spec = currentSpec.spec) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(IPayTheme.colors.bgNeutralSecondary)
                .verticalScroll(rememberScrollState())
                .padding(IPayTheme.spacing.s24),
        ) {
            Text(
                text = "Theme Preview (TASK-010 proof)",
                style = IPayTheme.typography.headingSmall,
                color = IPayTheme.colors.textNeutralPrimary,
            )

            Spacer(Modifier.height(IPayTheme.spacing.s8))

            Text(
                text = "Active spec: ${currentSpec.label}",
                style = IPayTheme.typography.bodyMedium,
                color = IPayTheme.colors.textNeutralSecondary,
            )

            Spacer(Modifier.height(IPayTheme.spacing.s16))

            Button(
                onClick = { currentSpec = currentSpec.next() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = IPayTheme.colors.buttonPrimaryBgEnd,
                    contentColor = IPayTheme.colors.buttonPrimaryLabel,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cycle theme → next")
            }

            Spacer(Modifier.height(IPayTheme.spacing.s24))

            // ─── Color samples ─────────────────────────────────────────
            Text(
                text = "Brand & semantic colors",
                style = IPayTheme.typography.titleMedium,
                color = IPayTheme.colors.textNeutralPrimary,
            )
            Spacer(Modifier.height(IPayTheme.spacing.s8))

            ColorSwatchRow(
                "brandPrimary" to IPayTheme.colors.textBrandPrimary,
                "borderBrandPrimary" to IPayTheme.colors.borderBrandPrimary,
                "iconWarning" to IPayTheme.colors.iconWarning,
                "iconSuccess" to IPayTheme.colors.iconSuccess,
            )

            Spacer(Modifier.height(IPayTheme.spacing.s16))

            // ─── Typography samples ─────────────────────────────────────
            Text(
                text = "Typography",
                style = IPayTheme.typography.titleMedium,
                color = IPayTheme.colors.textNeutralPrimary,
            )
            Spacer(Modifier.height(IPayTheme.spacing.s4))
            Text("Heading L 30/40", style = IPayTheme.typography.headingLarge)
            Text("Title L 18/26", style = IPayTheme.typography.titleLarge)
            Text("Body M 14/20", style = IPayTheme.typography.bodyMedium)
            Text("Label XS 10/14", style = IPayTheme.typography.labelXS)

            Spacer(Modifier.height(IPayTheme.spacing.s16))

            // ─── Material3 mapping proof ────────────────────────────────
            Text(
                text = "Material3 widget (Snackbar) auto-skinned",
                style = IPayTheme.typography.titleMedium,
                color = IPayTheme.colors.textNeutralPrimary,
            )
            Spacer(Modifier.height(IPayTheme.spacing.s8))
            Snackbar { Text("Material3 Snackbar auto-picks IPayTheme tokens") }

            Spacer(Modifier.height(IPayTheme.spacing.s24))

            // ─── Nested override proof ──────────────────────────────────
            Text(
                text = "Nested override (always Demo regardless of root)",
                style = IPayTheme.typography.titleMedium,
                color = IPayTheme.colors.textNeutralPrimary,
            )
            Spacer(Modifier.height(IPayTheme.spacing.s8))
            IPayTheme(spec = IPayThemes.Demo) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IPayTheme.colors.bgBrandSecondary, IPayTheme.shapes.small)
                        .border(
                            IPayTheme.stroke.s,
                            IPayTheme.colors.borderBrandPrimary,
                            IPayTheme.shapes.small,
                        )
                        .padding(IPayTheme.spacing.s16),
                ) {
                    Text(
                        text = "This subtree uses IPayThemes.Demo (red brand)",
                        style = IPayTheme.typography.bodyEmphasizedMedium,
                        color = IPayTheme.colors.textBrandPrimary,
                    )
                }
            }

            Spacer(Modifier.height(IPayTheme.spacing.s32))

            TextButton(onClick = onBack) {
                Text("Back", color = IPayTheme.colors.buttonGhostLabel)
            }
        }
    }
}

@Composable
private fun ColorSwatchRow(vararg samples: Pair<String, androidx.compose.ui.graphics.Color>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        samples.forEach { (label, color) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(IPayTheme.spacing.s40)
                        .background(color, IPayTheme.shapes.small)
                        .border(
                            IPayTheme.stroke.xs,
                            IPayTheme.colors.borderNeutralPrimary,
                            IPayTheme.shapes.small,
                        ),
                )
                Spacer(Modifier.height(IPayTheme.spacing.s4))
                Text(
                    text = label,
                    style = IPayTheme.typography.labelXS,
                    color = IPayTheme.colors.textNeutralTertiary,
                )
            }
            Spacer(Modifier.width(IPayTheme.spacing.s4))
        }
    }
}

private enum class NamedSpec(val label: String, val spec: IPayThemeSpec) {
    Default("Default (iPay light)", IPayThemes.Default),
    Dark("Dark (stub — mirrors Default)", IPayThemes.Dark),
    Demo("Demo (red brand)", IPayThemes.Demo);

    fun next(): NamedSpec = when (this) {
        Default -> Dark
        Dark -> Demo
        Demo -> Default
    }
}
