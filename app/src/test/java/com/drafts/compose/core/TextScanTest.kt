package com.drafts.compose.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TextScanTest {

    private fun slices(text: String, ranges: List<IntRange>) =
        ranges.map { text.substring(it.first, it.last + 1) }

    // ------------------------------------------------------------- sentences

    @Test
    fun `sentences split on terminators and keep them`() {
        val text = "One thing. Two things! Three?"
        assertEquals(listOf("One thing.", "Two things!", "Three?"), slices(text, TextScan.sentences(text)))
    }

    @Test
    fun `a trailing fragment with no terminator is still a sentence`() {
        val text = "One thing. Two things"
        assertEquals(listOf("One thing.", "Two things"), slices(text, TextScan.sentences(text)))
    }

    @Test
    fun `line breaks end a sentence`() {
        val text = "One thing\nTwo things"
        assertEquals(listOf("One thing", "Two things"), slices(text, TextScan.sentences(text)))
    }

    @Test
    fun `runs of terminators and blank space produce no empty sentences`() {
        val text = "One?!  \n\n  Two."
        assertEquals(listOf("One?", "Two."), slices(text, TextScan.sentences(text)))
    }

    @Test
    fun `blank text has no sentences`() {
        assertEquals(emptyList<IntRange>(), TextScan.sentences("   \n  "))
    }

    // ------------------------------------------------------------ paragraphs

    @Test
    fun `paragraphs split on blank lines`() {
        val text = "One.\n\nTwo.\n\nThree."
        assertEquals(listOf("One.", "Two.", "Three."), slices(text, TextScan.paragraphs(text)))
    }

    @Test
    fun `a single line break does not start a paragraph`() {
        val text = "One.\nStill one.\n\nTwo."
        assertEquals(listOf("One.\nStill one.", "Two."), slices(text, TextScan.paragraphs(text)))
    }

    @Test
    fun `several blank lines still separate exactly one paragraph`() {
        val text = "One.\n\n\n\nTwo."
        assertEquals(2, TextScan.paragraphs(text).size)
    }

    @Test
    fun `blank text has no paragraphs`() {
        assertEquals(emptyList<IntRange>(), TextScan.paragraphs("\n\n   \n"))
    }

    // ----------------------------------------------------------------- emoji

    @Test
    fun `plain emoji are counted individually`() {
        assertEquals(3, TextScan.emoji("a ✅ b 🔥 c ⭐").size)
    }

    @Test
    fun `text with no emoji counts zero`() {
        assertEquals(0, TextScan.emoji("Nothing here, just words and 123.").size)
    }

    @Test
    fun `a zero-width-joiner sequence counts as one emoji`() {
        assertEquals(1, TextScan.emoji("👩‍💻").size)
    }

    @Test
    fun `a skin tone modifier does not add to the count`() {
        assertEquals(1, TextScan.emoji("👍🏽").size)
    }

    @Test
    fun `a keycap counts as one emoji, not as a digit`() {
        assertEquals(1, TextScan.emoji("1️⃣").size)
    }

    @Test
    fun `a flag counts as one emoji`() {
        assertEquals(1, TextScan.emoji("🇺🇸").size)
    }

    @Test
    fun `an emoji span covers the whole glyph`() {
        val text = "x👩‍💻y"
        val span = TextScan.emoji(text).single()
        assertEquals("👩‍💻", text.substring(span.first, span.last + 1))
    }

    // ------------------------------------------------------------ whitespace

    @Test
    fun `collapseWhitespace flattens everything to single spaces`() {
        assertEquals("a b c", TextScan.collapseWhitespace("  a \n\n b \t c  "))
    }

    @Test
    fun `collapseInlineWhitespace keeps line structure`() {
        assertEquals("a b\nc", TextScan.collapseInlineWhitespace("  a   b \n  c  "))
    }

    @Test
    fun `collapseInlineWhitespace preserves blank lines between paragraphs`() {
        assertEquals("a\n\nb", TextScan.collapseInlineWhitespace("a\n\nb"))
    }
}
