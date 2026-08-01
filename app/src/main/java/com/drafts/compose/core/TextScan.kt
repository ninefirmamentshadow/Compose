package com.drafts.compose.core

/**
 * Sentence and paragraph segmentation, and emoji counting. Deliberately simple and
 * deterministic: rules that reason about "the same sentence" need a definition of
 * sentence that a person can predict, not one that is clever.
 */
object TextScan {

    private const val TERMINATORS = ".!?\n…"

    /**
     * Sentence spans, terminator included so a rule can test for "?" inside the
     * sentence it terminates. Blank runs are skipped; a trailing fragment with no
     * terminator still counts as a sentence.
     */
    fun sentences(text: String): List<IntRange> {
        val out = mutableListOf<IntRange>()
        var start = -1
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c in TERMINATORS) {
                // Keep a visible terminator ("." "?" "!") inside the span so a rule
                // can test for it; a newline is trimmed off, it is just whitespace.
                if (start >= 0) addTrimmed(out, text, start, i)
                start = -1
            } else if (!c.isWhitespace() && start < 0) {
                start = i
            }
            i++
        }
        if (start >= 0) addTrimmed(out, text, start, text.length - 1)
        return out
    }

    private fun addTrimmed(out: MutableList<IntRange>, text: String, start: Int, end: Int) {
        var last = end
        while (last >= start && text[last].isWhitespace()) last--
        if (last >= start) out.add(start..last)
    }

    /** Paragraph spans, split on blank lines. */
    fun paragraphs(text: String): List<IntRange> {
        val out = mutableListOf<IntRange>()
        var start = -1
        var blankRun = 0
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '\n') {
                blankRun++
                if (blankRun >= 2 && start >= 0) {
                    out.add(start..lastNonBlank(text, i))
                    start = -1
                }
            } else if (!c.isWhitespace()) {
                blankRun = 0
                if (start < 0) start = i
            }
            i++
        }
        if (start >= 0) out.add(start..lastNonBlank(text, text.length))
        return out
    }

    private fun lastNonBlank(text: String, exclusiveEnd: Int): Int {
        var j = exclusiveEnd - 1
        while (j > 0 && text[j].isWhitespace()) j--
        return j
    }

    // ---------------------------------------------------------------- emoji

    private val EMOJI_BLOCKS = listOf(
        0x1F000..0x1FAFF, // pictographs, faces, transport, flags, symbols ext-A
        0x2600..0x27BF,   // misc symbols and dingbats
        0x2B00..0x2BFF,   // misc symbols and arrows
        0x1F900..0x1F9FF  // supplemental (inside the first range; kept explicit)
    )

    private const val VARIATION_SELECTOR = 0xFE0F
    private const val ZWJ = 0x200D
    private val SKIN_TONES = 0x1F3FB..0x1F3FF
    private const val KEYCAP = 0x20E3
    private val TAGS = 0xE0020..0xE007F
    private val REGIONAL_INDICATORS = 0x1F1E6..0x1F1FF

    private fun isEmojiBase(cp: Int): Boolean = EMOJI_BLOCKS.any { cp in it }

    private fun isModifier(cp: Int): Boolean =
        cp == VARIATION_SELECTOR || cp in SKIN_TONES || cp == KEYCAP || cp in TAGS

    /**
     * Emoji spans, with ZWJ sequences, skin-tone modifiers and variation selectors
     * folded into the single glyph a reader actually sees. A keycap digit ("1️⃣")
     * counts as one emoji, not as a digit plus two invisibles.
     */
    fun emoji(text: String): List<IntRange> {
        val out = mutableListOf<IntRange>()
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val width = Character.charCount(cp)
            val nextIsVs = i + width < text.length && text.codePointAt(i + width) == VARIATION_SELECTOR
            if (isEmojiBase(cp) || nextIsVs) {
                var end = i + width
                // A flag is two regional indicators showing as one glyph.
                if (cp in REGIONAL_INDICATORS && end < text.length) {
                    val next = text.codePointAt(end)
                    if (next in REGIONAL_INDICATORS) end += Character.charCount(next)
                }
                while (end < text.length) {
                    val next = text.codePointAt(end)
                    val nextWidth = Character.charCount(next)
                    when {
                        isModifier(next) -> end += nextWidth
                        next == ZWJ -> {
                            val after = end + nextWidth
                            if (after < text.length) end = after + Character.charCount(text.codePointAt(after))
                            else break
                        }
                        else -> break
                    }
                }
                out.add(i..end - 1)
                i = end
            } else {
                i += width
            }
        }
        return out
    }

    /** Collapse every whitespace run to a single space and trim. */
    fun collapseWhitespace(text: String): String = text.trim().replace(Regex("\\s+"), " ")

    /** Collapse spaces and tabs but keep line structure. */
    fun collapseInlineWhitespace(text: String): String =
        text.trim().replace(Regex("[ \\t\\x0B\\f\\r]+"), " ").replace(Regex(" *\n *"), "\n")
}
