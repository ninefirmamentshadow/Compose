package com.drafts.compose.core

/**
 * The headline (or body) as one continuous string, plus the bookkeeping needed to
 * map any offset in that string back to the source field it came from.
 *
 * Rules run against the joined text — that is the only way a rule can see across
 * segment boundaries (a sentence spanning two body fields, an emoji budget for the
 * whole headline) — while findings still point at an individual EditText.
 */
class ScopedText private constructor(
    val scope: Scope,
    val text: String,
    private val parts: List<Part>
) {
    /** [start] inclusive, [end] exclusive, both offsets into [text]. */
    data class Part(val field: FieldId, val start: Int, val end: Int)

    val isBlank: Boolean get() = text.isBlank()

    /** The fields that actually contributed text, in order. */
    fun fields(): List<FieldId> = parts.map { it.field }

    /**
     * Translate a range in [text] into a range inside one source field.
     *
     * A range that starts inside a separator (" | " or a paragraph break) is
     * attributed to the next contributing field; a range that runs past the end of
     * its field is clamped, because a highlight may not cross an EditText.
     */
    fun locate(range: IntRange): FieldSpan {
        if (parts.isEmpty()) return FieldSpan(FieldId.HEADLINE_NAME, IntRange.EMPTY)
        val fallback = parts.first()
        if (range.isEmpty()) return FieldSpan(fallback.field, IntRange.EMPTY)

        val part = parts.firstOrNull { range.first >= it.start && range.first < it.end }
            ?: parts.firstOrNull { it.start >= range.first }
            ?: parts.last()

        val startInPart = (range.first - part.start).coerceAtLeast(0)
        val endInPart = (minOf(range.last, part.end - 1) - part.start)
        if (endInPart < startInPart) return FieldSpan(part.field, IntRange.EMPTY)
        return FieldSpan(part.field, startInPart..endInPart)
    }

    /** The exact substring a finding refers to. */
    fun excerpt(range: IntRange): String =
        if (range.isEmpty()) "" else text.substring(range.first, minOf(range.last + 1, text.length))

    companion object {
        /**
         * Join [segments] with [separator], dropping blank ones so that an empty
         * middle segment does not leave a dangling " | " or a phantom paragraph.
         */
        fun join(
            scope: Scope,
            separator: String,
            segments: List<Pair<FieldId, String>>
        ): ScopedText {
            val sb = StringBuilder()
            val parts = mutableListOf<Part>()
            for ((field, raw) in segments) {
                if (raw.isBlank()) continue
                if (sb.isNotEmpty()) sb.append(separator)
                val start = sb.length
                sb.append(raw)
                parts.add(Part(field, start, sb.length))
            }
            return ScopedText(scope, sb.toString(), parts)
        }
    }
}
