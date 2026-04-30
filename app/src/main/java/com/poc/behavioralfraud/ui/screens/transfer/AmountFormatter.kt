package com.poc.behavioralfraud.ui.screens.transfer

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Display amount with thousand separators while keeping raw digits in state.
 *
 * Input "1234567" → display "1,234,567". Cursor offset translates correctly
 * so left/right arrow keys work as expected on the formatted view.
 */
class ThousandSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter { it.isDigit() }
        val formatted = formatWithSeparators(raw)
        return TransformedText(
            text = AnnotatedString(formatted),
            offsetMapping = ThousandSeparatorOffsetMapping(rawLength = raw.length),
        )
    }

    private fun formatWithSeparators(raw: String): String {
        if (raw.length <= 3) return raw
        val builder = StringBuilder()
        val firstChunkLen = raw.length % 3
        var index = 0
        if (firstChunkLen > 0) {
            builder.append(raw, 0, firstChunkLen)
            index = firstChunkLen
        }
        while (index < raw.length) {
            if (builder.isNotEmpty()) builder.append(SEPARATOR)
            builder.append(raw, index, index + 3)
            index += 3
        }
        return builder.toString()
    }

    private companion object {
        const val SEPARATOR = ','
    }
}

/**
 * Maps cursor offsets between raw digits and the comma-separated display.
 *
 * For raw of length N, separators land between every group of 3 from the right.
 * Number of separators in the displayed string = (N - 1) / 3 (zero when N <= 3).
 */
private class ThousandSeparatorOffsetMapping(private val rawLength: Int) : OffsetMapping {
    override fun originalToTransformed(offset: Offset): Offset {
        if (rawLength <= 3) return offset
        // Number of separators inserted before position `offset` in the raw string.
        // A separator is inserted to the left of digits at indices N-3, N-6, N-9, ...
        // Equivalent: number of multiples of 3 strictly less than (rawLength - offset).
        val tail = rawLength - offset
        val seps = if (tail > 0) (tail - 1) / 3 else 0
        // But we only count separators that fall inside [0, offset) — since
        // separators are inserted to the LEFT of grouping boundaries from the
        // right, those landing before `offset` move the cursor right.
        val insertedBeforeOffset = totalSeparators(rawLength) - seps
        return offset + insertedBeforeOffset
    }

    override fun transformedToOriginal(offset: Offset): Offset {
        if (rawLength <= 3) return offset
        // Walk from start of formatted string counting separators.
        // Each comma encountered before `offset` reduces the original index by 1.
        val total = totalSeparators(rawLength)
        // Approximate: every 4th char (digit-digit-digit-comma) starting from
        // the front-chunk boundary. Cheaper: clamp via formula.
        var seps = 0
        // Compute prefix length contributed by the (possibly short) first chunk.
        val firstChunkLen = if (rawLength % 3 == 0) 3 else rawLength % 3
        var pos = 0
        while (pos < offset && seps < total) {
            // Chunk of 3 digits then optional separator.
            val chunkSize = if (seps == 0) firstChunkLen else 3
            pos += chunkSize
            if (pos >= offset) break
            // separator
            seps += 1
            pos += 1
        }
        return (offset - seps).coerceIn(0, rawLength)
    }

    private fun totalSeparators(rawLen: Int): Int =
        if (rawLen <= 3) 0 else (rawLen - 1) / 3
}

private typealias Offset = Int
