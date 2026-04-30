package com.poc.behavioralfraud.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.semantics.Role

/**
 * Click handler with debounce — prevents double-tap from triggering action twice.
 *
 * **MANDATORY** for all interactive components per CLAUDE.md and FR-CL-08 constraints.
 * Use this instead of stock [Modifier.clickable] everywhere a click triggers
 * navigation, transaction confirm, or any non-idempotent action.
 *
 * Default debounce window: 350ms — chosen to feel snappy but block accidental
 * double-taps that cause duplicate transfers / repeated dialog dismissals.
 *
 * @param debounceMs Minimum elapsed time between accepted clicks. Defaults to [DEFAULT_DEBOUNCE_MS].
 * @param enabled If false, click handler is no-op.
 * @param role Accessibility role passed through to Compose.
 * @param onSafeClick Invoked when click passes debounce gate.
 */
fun Modifier.safeClickable(
    debounceMs: Long = SafeClickableDefaults.DEFAULT_DEBOUNCE_MS,
    enabled: Boolean = true,
    role: Role? = null,
    onSafeClick: () -> Unit,
): Modifier = composed {
    val lastClickTime = remember { LongRef(0L) }

    this.clickable(
        enabled = enabled,
        role = role,
        onClick = {
            val now = System.currentTimeMillis()
            if (now - lastClickTime.value >= debounceMs) {
                lastClickTime.value = now
                onSafeClick()
            }
        },
    )
}

/**
 * Convenience overload — tap target without ripple/role (for parents that draw
 * their own indication, e.g. [IPayCard] elevated variants).
 */
@Composable
fun rememberSafeClickAction(
    debounceMs: Long = SafeClickableDefaults.DEFAULT_DEBOUNCE_MS,
    onSafeClick: () -> Unit,
): () -> Unit {
    val lastClickTime = remember { LongRef(0L) }
    return {
        val now = System.currentTimeMillis()
        if (now - lastClickTime.value >= debounceMs) {
            lastClickTime.value = now
            onSafeClick()
        }
    }
}

object SafeClickableDefaults {
    const val DEFAULT_DEBOUNCE_MS: Long = 350L
}

/** Mutable Long holder — replaces `mutableStateOf<Long>` to avoid recomposition triggers. */
private class LongRef(var value: Long)
