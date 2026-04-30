package com.poc.behavioralfraud.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poc.behavioralfraud.ui.theme.IPayTheme

/**
 * Text input — FR-CL-08 REQ-11.
 *
 * States:
 *  - Default       (border `inputBorderDefault`)
 *  - Active/focus  (border `inputBorderActive`, label `inputLabelActive`)
 *  - Error         (border `iconWarning`, error text below)
 *  - Disabled      (border `inputBorderDisabled`, bg `inputBgDisabled`, no input)
 *
 * Slots: [leading] / [trailing] for prefix/suffix icons (e.g. currency icon, clear-x).
 *
 * Built on [BasicTextField] for full style control — Material3 OutlinedTextField
 * is NOT used (deviates from iPay visual spec).
 */
@Composable
fun IPayTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    isError: Boolean = errorText != null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val colors = IPayTheme.colors
    val typography = IPayTheme.typography
    val spacing = IPayTheme.spacing

    val borderColor = when {
        !enabled -> colors.inputBorderDisabled
        isError -> colors.iconWarning
        isFocused -> colors.inputBorderActive
        else -> colors.inputBorderDefault
    }
    val bgColor = if (!enabled) colors.inputBgDisabled else colors.inputBgPrimaryDefault
    val labelColor = when {
        !enabled -> colors.inputLabelDefault
        isFocused -> colors.inputLabelActive
        else -> colors.inputLabelDefault
    }
    val textColor = if (enabled) colors.inputTextDefault else colors.inputTextDisabled

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                style = typography.bodyEmphasizedSmall,
                color = labelColor,
                modifier = Modifier.padding(bottom = spacing.s6),
            )
        }

        Box(
            modifier = Modifier
                .height(IPayTextFieldDefaults.HEIGHT)
                .clip(IPayTheme.shapes.small)
                .background(bgColor)
                .border(width = IPayTheme.stroke.s, brush = SolidColor(borderColor), shape = IPayTheme.shapes.small)
                .padding(horizontal = spacing.s12),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxHeight(),
            ) {
                leading?.let {
                    it()
                    Spacer(Modifier.width(spacing.s8))
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    visualTransformation = visualTransformation,
                    interactionSource = interactionSource,
                    textStyle = typography.bodyLarge.copy(color = textColor),
                    cursorBrush = SolidColor(colors.inputCaret),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty() && placeholder != null) {
                                Text(
                                    text = placeholder,
                                    style = typography.bodyLarge,
                                    color = colors.inputTextPlaceholder,
                                )
                            }
                            innerTextField()
                        }
                    },
                )

                trailing?.let {
                    Spacer(Modifier.width(spacing.s8))
                    it()
                }
            }
        }

        // Error supersedes helper.
        val belowText = errorText ?: helperText
        if (belowText != null) {
            Text(
                text = belowText,
                style = typography.bodySmall,
                color = if (isError) colors.iconWarning else colors.textNeutralTertiary,
                modifier = Modifier.padding(top = spacing.s4, start = spacing.s4),
            )
        }
    }
}

object IPayTextFieldDefaults {
    val HEIGHT: Dp = 56.dp
}
