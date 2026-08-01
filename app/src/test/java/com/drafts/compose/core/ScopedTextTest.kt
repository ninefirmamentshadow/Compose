package com.drafts.compose.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScopedTextTest {

    private fun headline(vararg segments: String) = ScopedText.join(
        Scope.HEADLINE,
        " | ",
        listOf(
            FieldId.HEADLINE_NAME to segments.getOrElse(0) { "" },
            FieldId.HEADLINE_CATEGORY to segments.getOrElse(1) { "" },
            FieldId.HEADLINE_FILTER to segments.getOrElse(2) { "" }
        )
    )

    @Test
    fun `segments join with the separator`() {
        assertEquals("Dana | Evenings | North", headline("Dana", "Evenings", "North").text)
    }

    @Test
    fun `blank segments are dropped without leaving a dangling separator`() {
        assertEquals("Dana | North", headline("Dana", "", "North").text)
        assertEquals("Dana", headline("Dana", "", "").text)
        assertEquals("", headline("", "", "").text)
    }

    @Test
    fun `only contributing fields are listed`() {
        assertEquals(
            listOf(FieldId.HEADLINE_NAME, FieldId.HEADLINE_FILTER),
            headline("Dana", "  ", "North").fields()
        )
    }

    @Test
    fun `an offset maps back to the field that owns it`() {
        val doc = headline("Dana", "Evenings", "North")
        assertEquals(FieldId.HEADLINE_NAME, doc.locate(0..3).field)
        assertEquals(FieldId.HEADLINE_CATEGORY, doc.locate(7..14).field)
        assertEquals(FieldId.HEADLINE_FILTER, doc.locate(18..22).field)
    }

    @Test
    fun `an offset maps to a range usable against the field's own text`() {
        val doc = headline("Dana", "Evenings", "North")
        val span = doc.locate(18..22)
        assertEquals(0..4, span.range)
        assertEquals("North", "North".substring(span.range.first, span.range.last + 1))
    }

    @Test
    fun `a range starting inside a separator is attributed to the next field`() {
        val doc = headline("Dana", "Evenings", "North")
        // index 4..6 is " | " between the first two segments
        assertEquals(FieldId.HEADLINE_CATEGORY, doc.locate(5..9).field)
    }

    @Test
    fun `a range spanning two fields is clamped to the first`() {
        val doc = headline("Dana", "Evenings", "North")
        val span = doc.locate(0..12)
        assertEquals(FieldId.HEADLINE_NAME, span.field)
        assertEquals(0..3, span.range)
    }

    @Test
    fun `an empty range means the whole field`() {
        val doc = headline("Dana")
        assertTrue(doc.locate(IntRange.EMPTY).isWholeField)
    }

    @Test
    fun `excerpt returns the exact substring`() {
        val doc = headline("Dana", "Evenings", "North")
        assertEquals("Evenings", doc.excerpt(7..14))
        assertEquals("", doc.excerpt(IntRange.EMPTY))
    }

    @Test
    fun `locating against an empty document does not throw`() {
        val doc = headline("", "", "")
        assertTrue(doc.isBlank)
        assertTrue(doc.locate(0..5).isWholeField)
    }
}
