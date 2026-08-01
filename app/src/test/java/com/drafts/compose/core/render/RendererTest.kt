package com.drafts.compose.core.render

import com.drafts.compose.core.FieldId
import com.drafts.compose.core.Fixtures
import com.drafts.compose.data.entity.PlatformProfile
import com.drafts.compose.data.entity.Register
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RendererTest {

    private val draft = Fixtures.listing(
        name = "Dana",
        category = "Evenings",
        filter = "North",
        who = "First field.",
        how = "Second field.\nSecond field continued.",
        contact = "Third field."
    )

    private fun profile(register: Register, headlineLimit: Int = 0, bodyLimit: Int = 0) =
        PlatformProfile(
            id = 1L,
            name = "TEST",
            headlineCharLimit = headlineLimit,
            bodyCharLimit = bodyLimit,
            register = register
        )

    // -------------------------------------------------------------- headline

    @Test
    fun `headline renders as name pipe category pipe filter`() {
        assertEquals("Dana | Evenings | North", Renderer.headline(draft))
    }

    @Test
    fun `headline drops blank segments`() {
        assertEquals("Dana | North", Renderer.headline(draft.copy(headlineCategory = "   ")))
        assertEquals("Dana", Renderer.headline(draft.copy(headlineCategory = "", headlineFilter = "")))
        assertEquals("", Renderer.headline(Fixtures.listing()))
    }

    @Test
    fun `headline collapses stray whitespace inside a segment`() {
        assertEquals(
            "Dana | Evenings | North",
            Renderer.headline(draft.copy(headlineCategory = "  Evenings \n "))
        )
    }

    @Test
    fun `headline shape does not vary by platform`() {
        val shapes = Register.entries.map { Renderer.render(draft, profile(it)).headline }.toSet()
        assertEquals(1, shapes.size)
    }

    // ------------------------------------------------------------------ body

    @Test
    fun `short scannable puts one line per field`() {
        assertEquals(
            "First field.\nSecond field. Second field continued.\nThird field.",
            Renderer.body(draft, Register.SHORT_SCANNABLE)
        )
    }

    @Test
    fun `long form separates fields with a blank line and keeps internal breaks`() {
        assertEquals(
            "First field.\n\nSecond field.\nSecond field continued.\n\nThird field.",
            Renderer.body(draft, Register.LONG_FORM)
        )
    }

    @Test
    fun `blunt collapses everything into one paragraph`() {
        assertEquals(
            "First field. Second field. Second field continued. Third field.",
            Renderer.body(draft, Register.BLUNT)
        )
    }

    @Test
    fun `body drops blank fields in every register`() {
        val sparse = draft.copy(bodyHowItWorks = "  ")
        assertEquals("First field.\nThird field.", Renderer.body(sparse, Register.SHORT_SCANNABLE))
        assertEquals("First field.\n\nThird field.", Renderer.body(sparse, Register.LONG_FORM))
        assertEquals("First field. Third field.", Renderer.body(sparse, Register.BLUNT))
    }

    @Test
    fun `an empty draft renders empty in every register`() {
        Register.entries.forEach { assertEquals("", Renderer.body(Fixtures.listing(), it)) }
    }

    @Test
    fun `rendering never adds or removes a word`() {
        val words = { s: String -> s.split(Regex("\\s+")).filter { it.isNotEmpty() } }
        val source = words(draft.bodyWhoYouAre + " " + draft.bodyHowItWorks + " " + draft.bodyContact)
        Register.entries.forEach { register ->
            assertEquals(
                "register $register altered the words",
                source,
                words(Renderer.body(draft, register))
            )
        }
    }

    // ------------------------------------------------------------- rendering

    @Test
    fun `full rendering is headline then body`() {
        val rendering = Renderer.render(draft, profile(Register.BLUNT))
        assertEquals(rendering.headline + "\n\n" + rendering.body, rendering.full)
    }

    @Test
    fun `full rendering omits an empty half`() {
        val headlineOnly = Fixtures.listing(name = "Dana")
        assertEquals("Dana", Renderer.render(headlineOnly, profile(Register.BLUNT)).full)
    }

    @Test
    fun `counts are measured against the platform limits`() {
        val rendering = Renderer.render(draft, profile(Register.BLUNT, headlineLimit = 10, bodyLimit = 500))
        assertEquals("Dana | Evenings | North".length, rendering.headlineCount)
        assertTrue(rendering.headlineOverLimit)
        assertEquals(23 - 10, rendering.headlineOverBy)
        assertFalse(rendering.bodyOverLimit)
    }

    @Test
    fun `a zero limit means unmeasured, never over`() {
        val rendering = Renderer.render(draft, profile(Register.BLUNT))
        assertFalse(rendering.headlineOverLimit)
        assertFalse(rendering.bodyOverLimit)
        assertEquals(0, rendering.headlineOverBy)
    }

    @Test
    fun `exactly at the limit is not over`() {
        val rendering = Renderer.render(draft, profile(Register.BLUNT, headlineLimit = 23))
        assertFalse(rendering.headlineOverLimit)
    }

    @Test
    fun `the same source renders differently per platform`() {
        val short = Renderer.render(draft, profile(Register.SHORT_SCANNABLE))
        val long = Renderer.render(draft, profile(Register.LONG_FORM))
        val blunt = Renderer.render(draft, profile(Register.BLUNT))
        assertEquals(3, setOf(short.body, long.body, blunt.body).size)
    }

    // ---------------------------------------------------------- source views

    @Test
    fun `the headline source view keeps raw text so offsets stay valid`() {
        val messy = draft.copy(headlineCategory = "  Evenings  ")
        assertEquals("Dana |   Evenings   | North", Renderer.headlineSource(messy).text)
    }

    @Test
    fun `the body source view joins fields as paragraphs in order`() {
        assertEquals(
            listOf(FieldId.BODY_WHO_YOU_ARE, FieldId.BODY_HOW_IT_WORKS, FieldId.BODY_CONTACT),
            Renderer.bodySource(draft).fields()
        )
    }
}
