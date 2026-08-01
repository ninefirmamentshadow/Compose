package com.drafts.compose.core.lint

import com.drafts.compose.core.FieldId
import com.drafts.compose.core.Fixtures
import com.drafts.compose.core.Scope
import com.drafts.compose.core.Severity
import com.drafts.compose.core.forRule
import com.drafts.compose.core.hasRule
import com.drafts.compose.core.render.Renderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LintEngineTest {

    // ------------------------------------------------------------- baseline

    @Test
    fun `clean draft produces no findings`() {
        assertEquals(emptyList<String>(), Fixtures.lint(Fixtures.cleanListing()).map { it.ruleId })
    }

    @Test
    fun `empty draft produces no findings`() {
        assertTrue(Fixtures.lint(Fixtures.listing()).isEmpty())
    }

    // ------------------------------------------------- BLOCK  incall phrasing

    @Test
    fun `incall phrasing is blocked`() {
        val phrases = listOf(
            "Incall only for regulars." to "Incall",
            "This is an in-call arrangement." to "in-call",
            "That is an in call arrangement." to "in call",
            "We can meet at my place." to "my place",
            "You can come to me instead." to "come to me",
            "I host during the week." to "host",
            "I am hosting this week." to "hosting"
        )
        phrases.forEach { (text, expected) ->
            val findings = Fixtures.lint(Fixtures.listing(who = text)).forRule("INCALL_PHRASING")
            assertEquals("expected a finding in: $text", 1, findings.size)
            assertTrue(
                "expected excerpt containing '$expected' in: $text",
                findings.first().excerpt.contains(expected, ignoreCase = true)
            )
            assertEquals(Severity.BLOCK, findings.first().severity)
        }
    }

    @Test
    fun `words merely containing host are not flagged`() {
        listOf("I stayed at a hostel.", "A ghost of a chance.", "The hostess was polite.")
            .forEach { text ->
                assertFalse(
                    "false positive on: $text",
                    Fixtures.lint(Fixtures.listing(who = text)).hasRule("INCALL_PHRASING")
                )
            }
    }

    // --------------------------------------------- BLOCK  real-time location

    @Test
    fun `real-time location phrasing is blocked`() {
        listOf(
            "I am here now.",
            "In town at the weekend.",
            "Currently at the north end.",
            "Room 214 is where to ask for.",
            "See room #7 for details."
        ).forEach { text ->
            val findings = Fixtures.lint(Fixtures.listing(who = text)).forRule("REALTIME_LOCATION")
            assertEquals("expected a finding in: $text", 1, findings.size)
            assertEquals(Severity.BLOCK, findings.first().severity)
        }
    }

    @Test
    fun `location phrasing without the real-time marker is not flagged`() {
        listOf("I have a room prepared.", "In town on Thursdays.", "Here is the schedule.")
            .forEach { text ->
                assertFalse(
                    "false positive on: $text",
                    Fixtures.lint(Fixtures.listing(who = text)).hasRule("REALTIME_LOCATION")
                )
            }
    }

    // -------------------------------------------- BLOCK  price near an offer

    @Test
    fun `price in the same sentence as an offer term is blocked`() {
        val findings = Fixtures.lint(Fixtures.listing(who = "One hour is \$200."))
            .forRule("PRICE_NEAR_OFFER")
        assertEquals(1, findings.size)
        assertEquals(Severity.BLOCK, findings.first().severity)
        assertEquals("the whole sentence is highlighted", "One hour is \$200.", findings.first().excerpt)
    }

    @Test
    fun `price and offer term in different sentences are not flagged`() {
        val listing = Fixtures.listing(who = "Total is \$200. One hour is the usual length.")
        assertFalse(Fixtures.lint(listing).hasRule("PRICE_NEAR_OFFER"))
    }

    @Test
    fun `a price alone is not flagged by the lint pass`() {
        assertFalse(Fixtures.lint(Fixtures.listing(who = "Total is \$200.")).hasRule("PRICE_NEAR_OFFER"))
    }

    @Test
    fun `an offer term alone is not flagged`() {
        assertFalse(
            Fixtures.lint(Fixtures.listing(who = "An hour is the usual booking.")).hasRule("PRICE_NEAR_OFFER")
        )
    }

    @Test
    fun `spelled-out money counts as a price`() {
        assertTrue(
            Fixtures.lint(Fixtures.listing(who = "One hour is 200 dollars.")).hasRule("PRICE_NEAR_OFFER")
        )
    }

    // ------------------------------------------------ WARN  defensive tone

    @Test
    fun `defensive phrasing warns`() {
        listOf(
            "No timewasters.",
            "No time wasters please.",
            "Serious inquiries only.",
            "Serious enquiries only.",
            "Don't waste my time.",
            "I'm not free."
        ).forEach { text ->
            val findings = Fixtures.lint(Fixtures.listing(who = text)).forRule("DEFENSIVE_PHRASING")
            assertEquals("expected a finding in: $text", 1, findings.size)
            assertEquals(Severity.WARN, findings.first().severity)
        }
    }

    @Test
    fun `ordinary refusals are not defensive phrasing`() {
        assertFalse(
            Fixtures.lint(Fixtures.listing(who = "I reply to complete messages only."))
                .hasRule("DEFENSIVE_PHRASING")
        )
    }

    // ------------------------------------------------------ WARN  emoji budget

    @Test
    fun `more than two emoji in the headline warns`() {
        val findings = Fixtures.lint(Fixtures.listing(name = "Dana ✅🔥⭐")).forRule("EMOJI_HEADLINE")
        assertEquals(1, findings.size)
        assertEquals(Severity.WARN, findings.first().severity)
        assertTrue(findings.first().message.contains("Found 3"))
    }

    @Test
    fun `exactly two emoji in the headline is fine`() {
        assertFalse(Fixtures.lint(Fixtures.listing(name = "Dana ✅🔥")).hasRule("EMOJI_HEADLINE"))
    }

    @Test
    fun `emoji budget counts across all three headline segments`() {
        val listing = Fixtures.listing(name = "Dana ✅", category = "Evenings 🔥", filter = "North ⭐")
        assertTrue(Fixtures.lint(listing).hasRule("EMOJI_HEADLINE"))
    }

    @Test
    fun `more than five emoji in the body warns`() {
        val findings = Fixtures.lint(Fixtures.listing(who = "Hello ✅🔥⭐✅🔥⭐")).forRule("EMOJI_BODY")
        assertEquals(1, findings.size)
        assertTrue(findings.first().message.contains("Found 6"))
    }

    @Test
    fun `five emoji in the body is fine`() {
        assertFalse(Fixtures.lint(Fixtures.listing(who = "Hello ✅🔥⭐✅🔥")).hasRule("EMOJI_BODY"))
    }

    @Test
    fun `headline emoji budget does not apply to the body and vice versa`() {
        val headlineHeavy = Fixtures.lint(Fixtures.listing(name = "A ✅🔥⭐"))
        assertTrue(headlineHeavy.hasRule("EMOJI_HEADLINE"))
        assertFalse(headlineHeavy.hasRule("EMOJI_BODY"))

        val bodyHeavy = Fixtures.lint(Fixtures.listing(who = "A ✅🔥⭐"))
        assertFalse(bodyHeavy.hasRule("EMOJI_HEADLINE"))
        assertFalse(bodyHeavy.hasRule("EMOJI_BODY"))
    }

    // -------------------------------------------------- WARN  paragraph budget

    @Test
    fun `body past three paragraphs warns`() {
        val listing = Fixtures.listing(
            who = "One.\n\nTwo.",
            how = "Three.",
            contact = "Four."
        )
        val findings = Fixtures.lint(listing).forRule("BODY_PARAGRAPHS")
        assertEquals(1, findings.size)
        assertEquals(Severity.WARN, findings.first().severity)
        assertTrue(findings.first().message.contains("Found 4"))
        assertEquals("the overflow is attributed to its own field", FieldId.BODY_CONTACT, findings.first().field)
    }

    @Test
    fun `three body paragraphs is fine`() {
        val listing = Fixtures.listing(who = "One.", how = "Two.", contact = "Three.")
        assertFalse(Fixtures.lint(listing).hasRule("BODY_PARAGRAPHS"))
    }

    @Test
    fun `single line breaks do not start a new paragraph`() {
        val listing = Fixtures.listing(who = "One.\nStill one.", how = "Two.", contact = "Three.")
        assertFalse(Fixtures.lint(listing).hasRule("BODY_PARAGRAPHS"))
    }

    // ---------------------------------------------- WARN  boundary as question

    @Test
    fun `a boundary phrased as a question warns`() {
        listOf(
            "I don't reply to blocked numbers?",
            "Screening is required, ok?",
            "No exceptions?"
        ).forEach { text ->
            val findings = Fixtures.lint(Fixtures.listing(who = text)).forRule("BOUNDARY_AS_QUESTION")
            assertEquals("expected a finding in: $text", 1, findings.size)
            assertEquals(Severity.WARN, findings.first().severity)
        }
    }

    @Test
    fun `the same boundary stated flatly does not warn`() {
        assertFalse(
            Fixtures.lint(Fixtures.listing(who = "I don't reply to blocked numbers."))
                .hasRule("BOUNDARY_AS_QUESTION")
        )
    }

    @Test
    fun `an ordinary question does not warn`() {
        assertFalse(
            Fixtures.lint(Fixtures.listing(who = "What time works for you?")).hasRule("BOUNDARY_AS_QUESTION")
        )
    }

    @Test
    fun `a question mark in a different sentence does not warn`() {
        assertFalse(
            Fixtures.lint(Fixtures.listing(who = "I don't reply to blocked numbers. What time works?"))
                .hasRule("BOUNDARY_AS_QUESTION")
        )
    }

    // ----------------------------------------------------- findings point back

    @Test
    fun `a finding points at the exact offset inside its own field`() {
        val listing = Fixtures.listing(
            who = "Alpha beta gamma.",
            how = "I host during the week."
        )
        val finding = Fixtures.lint(listing).forRule("INCALL_PHRASING").single()

        assertEquals(FieldId.BODY_HOW_IT_WORKS, finding.field)
        assertEquals(
            "the range must be usable as-is against the field's own text",
            "host",
            listing.bodyHowItWorks.substring(finding.range.first, finding.range.last + 1)
        )
        assertEquals("host", finding.excerpt)
    }

    @Test
    fun `a finding in the third headline segment maps to that segment`() {
        val listing = Fixtures.listing(name = "Dana", category = "Evenings", filter = "Here now")
        val finding = Fixtures.lint(listing).forRule("REALTIME_LOCATION").single()

        assertEquals(FieldId.HEADLINE_FILTER, finding.field)
        assertEquals(
            "Here now",
            listing.headlineFilter.substring(finding.range.first, finding.range.last + 1)
        )
    }

    @Test
    fun `a blank middle segment does not shift offsets in later segments`() {
        val listing = Fixtures.listing(name = "Dana", category = "", filter = "Here now")
        val finding = Fixtures.lint(listing).forRule("REALTIME_LOCATION").single()

        assertEquals(FieldId.HEADLINE_FILTER, finding.field)
        assertEquals(0, finding.range.first)
    }

    // ----------------------------------------------------------------- order

    @Test
    fun `blocks sort ahead of warns`() {
        val listing = Fixtures.listing(
            who = "No timewasters.",
            how = "I host during the week."
        )
        val findings = Fixtures.lint(listing)
        assertEquals(Severity.BLOCK, findings.first().severity)
        assertEquals("INCALL_PHRASING", findings.first().ruleId)
        assertTrue(findings.last().severity == Severity.WARN)
    }

    @Test
    fun `every finding is tagged as coming from the lint pass`() {
        val listing = Fixtures.listing(who = "I host. No timewasters.")
        assertTrue(Fixtures.lint(listing).all { it.source.name == "LINT" })
    }

    // --------------------------------------------------------- extensibility

    @Test
    fun `the engine runs whatever rule list it is handed`() {
        val custom = listOf(
            PatternRule(
                id = "CUSTOM",
                pattern = Regex("widget", RegexOption.IGNORE_CASE),
                severity = Severity.BLOCK,
                message = "no widgets"
            )
        )
        val listing = Fixtures.listing(who = "A widget and I host.")
        val findings = Fixtures.lint(listing, custom)

        assertEquals(listOf("CUSTOM"), findings.map { it.ruleId })
        assertEquals("widget", findings.single().excerpt)
    }

    @Test
    fun `a rule scoped to the headline never fires on the body`() {
        val custom = listOf(
            PatternRule(
                id = "HEADLINE_ONLY",
                pattern = Regex("widget"),
                severity = Severity.WARN,
                message = "headline only",
                scope = Scope.HEADLINE
            )
        )
        assertTrue(Fixtures.lint(Fixtures.listing(who = "widget"), custom).isEmpty())
        assertEquals(1, Fixtures.lint(Fixtures.listing(name = "widget"), custom).size)
    }

    @Test
    fun `an empty rule list produces no findings`() {
        assertTrue(Fixtures.lint(Fixtures.listing(who = "I host. No timewasters."), emptyList()).isEmpty())
    }

    @Test
    fun `every seeded rule has a unique id`() {
        val ids = LintRules.DEFAULT.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `the seeded rules cover the eight documented checks`() {
        assertEquals(
            setOf(
                "INCALL_PHRASING",
                "REALTIME_LOCATION",
                "PRICE_NEAR_OFFER",
                "DEFENSIVE_PHRASING",
                "EMOJI_HEADLINE",
                "EMOJI_BODY",
                "BODY_PARAGRAPHS",
                "BOUNDARY_AS_QUESTION"
            ),
            LintRules.DEFAULT.map { it.id }.toSet()
        )
    }

    @Test
    fun `rules run against source text not rendered text`() {
        // The renderer collapses whitespace; the lint pass must not, or offsets
        // would no longer line up with what is on screen.
        val listing = Fixtures.listing(who = "I    host    here.")
        val finding = Fixtures.lint(listing).forRule("INCALL_PHRASING").single()
        assertEquals("host", listing.bodyWhoYouAre.substring(finding.range.first, finding.range.last + 1))
        assertTrue(Renderer.body(listing, com.drafts.compose.data.entity.Register.BLUNT).contains("I host here."))
    }
}
