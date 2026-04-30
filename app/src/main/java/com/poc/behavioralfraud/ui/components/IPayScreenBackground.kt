package com.poc.behavioralfraud.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.poc.behavioralfraud.R
import com.poc.behavioralfraud.ui.theme.IPayTheme

/**
 * Light iPay screen background — Figma `iPay.Background` instance shared across
 * Recipient / TransferForm / OTP / Success screens.
 *
 * Subtle vertical gradient (top blue tint → near-white). PNG exported from Figma
 * (375×812, drawable-nodpi). `ContentScale.Crop` + `TopCenter` so the gradient's
 * top tint always anchors to the system status bar area on any phone width.
 *
 * Use as the OUTERMOST wrapper of a screen:
 * ```
 * IPayScreenBackground {
 *     Column { … screen content … }
 * }
 * ```
 *
 * Falls back to `bgNeutralPrimary` solid fill behind the Image (so content is
 * never on a transparent background if the drawable fails to load).
 */
@Composable
fun IPayScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IPayTheme.colors.bgNeutralPrimary),
    ) {
        Image(
            painter = painterResource(R.drawable.bg_ipay_screen),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize(),
        )
        content()
    }
}
