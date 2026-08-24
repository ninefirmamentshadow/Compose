package com.drafts.compose.core.check

import com.drafts.compose.core.FieldId
import com.drafts.compose.core.FindingSource
import com.drafts.compose.core.Fixtures
import com.drafts.compose.core.Severity
import com.drafts.compose.core.forRule
import com.drafts.compose.core.hasRule
import com.drafts.compose.data.entity.CanonicalValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsistencyCheckerTest {

    private val ratesOnly = Fixtures.canonicalRatesOnly
    private val full = Fixtures.canonicalFull

    // ------------------------------------------------------------- baseline

    @Test
    fun `a listing matching canonical values produces no findings`() {
        val listing = Fixtures.listing(
            who = "independent and established",
            how = "Rates are 80, 150 and 250.",
            contact = "@placeholder.handle — text before noon."
        )
        assertEquals(emptyList<String>(), Fixtures.check(listing, full).map { it.ruleId })
    }

    @Test
    fun `an empty listing is only checked against values that are set`() {
        assertTrue(Fixtures.check(Fixtures.listing(), ratesOnly).isEmpty())
    }

    // ---------------------------------------------------------------- rates

    @Test
    fun `canonical rates in rate context are accepted`() {
        val listing = Fixtures.listing(who = "Rates: 80, 150, 250.")
        assertTrue(Fixtures.check(listing, ratesOnly).isEmpty())
    }

    @Test
    fun `a rate-shaped number that matches nothing canonical is blocked`() {
        val findings = Fixtures.check(Fixtures.listing(who = "Rate is 175."), ratesOnly)
            .forRule("RATE_MISMATCH")
        assertEquals(1, findings.size)
        assertEquals(Severity.BLOCK, findings.first().severity)
        assertEquals("175", findings.first().excerpt)
    }

    @Test
    fun `a mismatched rate names the canonical values in its message`() {
        val message = Fixtures.check(Fixtures.listing(who = "Rate is 175."), ratesOnly)
            .forRule("RATE_MISMATCH").single().message
        assertTrue(message, message.contains("qv 80"))
        assertTrue(message, message.contains("hh 150"))
        assertTrue(message, message.contains("hour 250"))
        assertTrue(message, message.contains("175"))
    }

    @Test
    fun `an explicit currency amount is checked with or without rate context`() {
        val findings = Fixtures.check(Fixtures.listing(who = "Total is \$175."), ratesOnly)
            .forRule("RATE_MISMATCH")
        assertEquals(1, findings.size)
        assertEquals("\$175", findings.first().excerpt)
    }

    @Test
    fun `an explicit currency amount matching canonical is accepted`() {
        assertTrue(Fixtures.check(Fixtures.listing(who = "Total is \$150."), ratesOnly).isEmpty())
    }

    @Test
    fun `a currency amount with decimals is read at its whole-dollar value`() {
        assertTrue(Fixtures.check(Fixtures.listing(who = "Total is \$150.00."), ratesOnly).isEmpty())
        assertTrue(
            Fixtures.check(Fixtures.listing(who = "Total is \$175.00."), ratesOnly)
                .hasRule("RATE_MISMATCH")
        )
    }

    @Test
    fun `a bare number outside rate context is left alone`() {
        listOf(
            "I have done this for 12 years.",
            "Reply within 30 of reading this.",
            "Post 2024 was a quiet year."
        ).forEach { text ->
            assertFalse(
                "false positive on: $text",
                Fixtures.check(Fixtures.listing(who = text), ratesOnly).hasRule("RATE_MISMATCH")
            )
        }
    }

    @Test
    fun `rate context is judged sentence by sentence`() {
        // "175" sits in its own sentence with no rate word, so it is not a rate.
        val listing = Fixtures.listing(who = "Rates are 150. The room number is 175.")
        assertTrue(Fixtures.check(listing, ratesOnly).isEmpty())
    }

    @Test
    fun `a spaced currency amount is reported once, not twice`() {
        val findings = Fixtures.check(Fixtures.listing(who = "Rate is \$ 175."), ratesOnly)
            .forRule("RATE_MISMATCH")
        assertEquals(1, findings.size)
    }

    @Test
    fun `a five-or-more-digit dollar amount is captured whole and flagged as a mismatch`() {
        // Regression: explicitMoney's capture group used to cap at 4 digits with
        // no upper bound, so "$12005" matched only "$1200" and was judged against
        // that truncated value instead of the real one. It must capture the
        // complete amount and report it against real canonical rates.
        val findings = Fixtures.check(Fixtures.listing(who = "Total due is \$12005."), ratesOnly)
            .forRule("RATE_MISMATCH")
        assertEquals(1, findings.size)
        assertEquals("\$12005", findings.first().excerpt)
    }

    @Test
    fun `capturing the complete amount prevents a truncated false accept`() {
        // The concrete failure mode of the old bug: if a canonical rate happened
        // to equal a longer amount's first four digits, the truncated match made
        // "$12005" against a canonical 1200 read as consistent. It must not.
        val truncationRisk = ratesOnly.copy(rateQv = 1200)
        val findings = Fixtures.check(
            Fixtures.listing(who = "Total due is \$12005."),
            truncationRisk
        ).forRule("RATE_MISMATCH")
        assertEquals(1, findings.size)
    }

    @Test
    fun `rates are not checked when no canonical rate is set`() {
        val none = CanonicalValues()
        assertTrue(Fixtures.check(Fixtures.listing(who = "Rate is 175 or \$999."), none).isEmpty())
    }

    @Test
    fun `an unset canonical rate does not silently validate a number`() {
        val partial = ratesOnly.copy(rateQv = 0)
        assertTrue(
            Fixtures.check(Fixtures.listing(who = "Rate is 80."), partial).hasRule("RATE_MISMATCH")
        )
    }

    @Test
    fun `a mismatched rate points at the field it appears in`() {
        val listing = Fixtures.listing(who = "Nothing here.", how = "Rate is 175.")
        val finding = Fixtures.check(listing, ratesOnly).forRule("RATE_MISMATCH").single()
        assertEquals(FieldId.BODY_HOW_IT_WORKS, finding.field)
        assertEquals("175", listing.bodyHowItWorks.substring(finding.range.first, finding.range.last + 1))
    }

    @Test
    fun `rates in the headline are checked too`() {
        val listing = Fixtures.listing(name = "Dana", category = "Rate 175")
        val finding = Fixtures.check(listing, ratesOnly).forRule("RATE_MISMATCH").single()
        assertEquals(FieldId.HEADLINE_CATEGORY, finding.field)
    }

    // ----------------------------------------------------------- descriptor

    @Test
    fun `a byte-identical descriptor is accepted`() {
        val listing = Fixtures.listing(who = "independent and established")
        assertFalse(Fixtures.check(listing, full.copy(contactHandle = "", contactInstruction = ""))
            .hasRule("BIO_DESCRIPTOR_DRIFT"))
    }

    @Test
    fun `a descriptor differing only in case is flagged`() {
        val listing = Fixtures.listing(who = "Independent And Established")
        val findings = Fixtures.check(listing, full.copy(contactHandle = "", contactInstruction = ""))
            .forRule("BIO_DESCRIPTOR_DRIFT")
        assertEquals(1, findings.size)
        assertEquals(Severity.WARN, findings.first().severity)
        assertEquals("Independent And Established", findings.first().excerpt)
    }

    @Test
    fun `a descriptor differing only in spacing is flagged`() {
        val listing = Fixtures.listing(who = "independent  and  established")
        assertTrue(
            Fixtures.check(listing, full.copy(contactHandle = "", contactInstruction = ""))
                .hasRule("BIO_DESCRIPTOR_DRIFT")
        )
    }

    @Test
    fun `a descriptor that does not appear at all is not flagged`() {
        // The rule is about drift, not about mandating the descriptor everywhere.
        val listing = Fixtures.listing(who = "Something else entirely.")
        assertFalse(
            Fixtures.check(listing, full.copy(contactHandle = "", contactInstruction = ""))
                .hasRule("BIO_DESCRIPTOR_DRIFT")
        )
    }

    @Test
    fun `no descriptor set means no descriptor checking`() {
        val listing = Fixtures.listing(who = "Independent And Established")
        assertFalse(Fixtures.check(listing, ratesOnly).hasRule("BIO_DESCRIPTOR_DRIFT"))
    }

    // -------------------------------------------------------------- contact

    private val handleOnly = CanonicalValues(contactHandle = "@placeholder.handle")

    @Test
    fun `an exact contact handle is accepted`() {
        val listing = Fixtures.listing(contact = "Write to @placeholder.handle first.")
        assertTrue(Fixtures.check(listing, handleOnly).isEmpty())
    }

    @Test
    fun `a missing contact handle is blocked against the contact field`() {
        val listing = Fixtures.listing(contact = "Write to me first.")
        val finding = Fixtures.check(listing, handleOnly).forRule("CONTACT_HANDLE_MISSING").single()
        assertEquals(Severity.BLOCK, finding.severity)
        assertEquals(FieldId.BODY_CONTACT, finding.field)
        assertTrue("a whole-field finding carries no span", finding.span.isWholeField)
        assertEquals("", finding.excerpt)
        assertTrue(finding.message.contains("@placeholder.handle"))
    }

    @Test
    fun `an absent handle is blocked even when the listing is otherwise empty`() {
        assertTrue(Fixtures.check(Fixtures.listing(), handleOnly).hasRule("CONTACT_HANDLE_MISSING"))
    }

    @Test
    fun `a handle differing in case is drift, not absence`() {
        val listing = Fixtures.listing(contact = "Write to @Placeholder.Handle first.")
        val finding = Fixtures.check(listing, handleOnly).forRule("CONTACT_HANDLE_DRIFT").single()
        assertEquals(Severity.BLOCK, finding.severity)
        assertEquals("@Placeholder.Handle", finding.excerpt)
        assertEquals(FieldId.BODY_CONTACT, finding.field)
    }

    @Test
    fun `a handle missing its leading marker is drift`() {
        val listing = Fixtures.listing(contact = "Write to placeholder.handle first.")
        val finding = Fixtures.check(listing, handleOnly).forRule("CONTACT_HANDLE_DRIFT").single()
        assertEquals("placeholder.handle", finding.excerpt)
    }

    @Test
    fun `a handle in the headline counts as present`() {
        val listing = Fixtures.listing(filter = "@placeholder.handle")
        assertTrue(Fixtures.check(listing, handleOnly).isEmpty())
    }

    @Test
    fun `a handle does not match as a substring inside an unrelated word`() {
        // Regression: looseRegex had no word-boundary anchors, so canonical
        // "@shop" matched the "shop" inside "bookshop" and was reported as
        // CONTACT_HANDLE_DRIFT instead of correctly falling through to missing.
        val listing = Fixtures.listing(contact = "Ask about the bookshop hours.")
        val finding = Fixtures.check(listing, CanonicalValues(contactHandle = "@shop"))
            .forRule("CONTACT_HANDLE_MISSING").single()
        assertEquals(Severity.BLOCK, finding.severity)
    }

    @Test
    fun `no canonical handle set means no handle checking`() {
        assertFalse(
            Fixtures.check(Fixtures.listing(contact = "Nothing."), ratesOnly)
                .hasRule("CONTACT_HANDLE_MISSING")
        )
    }

    // --------------------------------------------------------- instruction

    private val instructionOnly = CanonicalValues(contactInstruction = "text before noon")

    @Test
    fun `an exact contact instruction is accepted`() {
        val listing = Fixtures.listing(contact = "Please text before noon.")
        assertTrue(Fixtures.check(listing, instructionOnly).isEmpty())
    }

    @Test
    fun `a missing contact instruction warns rather than blocks`() {
        val listing = Fixtures.listing(contact = "Please write.")
        val finding = Fixtures.check(listing, instructionOnly)
            .forRule("CONTACT_INSTRUCTION_MISSING").single()
        assertEquals(Severity.WARN, finding.severity)
    }

    @Test
    fun `a drifted contact instruction warns`() {
        val listing = Fixtures.listing(contact = "Please Text Before Noon.")
        val finding = Fixtures.check(listing, instructionOnly)
            .forRule("CONTACT_INSTRUCTION_DRIFT").single()
        assertEquals(Severity.WARN, finding.severity)
        assertEquals("Text Before Noon", finding.excerpt)
    }

    // ----------------------------------------------------------- provenance

    @Test
    fun `every finding is tagged as coming from the consistency pass`() {
        val listing = Fixtures.listing(who = "Rate is 175.", contact = "Nothing.")
        val findings = Fixtures.check(listing, full)
        assertTrue(findings.isNotEmpty())
        assertTrue(findings.all { it.source == FindingSource.CONSISTENCY })
    }

    @Test
    fun `blocks sort ahead of warns`() {
        val listing = Fixtures.listing(
            who = "Independent And Established",
            how = "Rate is 175.",
            contact = "@placeholder.handle text before noon"
        )
        val findings = Fixtures.check(listing, full)
        assertEquals(Severity.BLOCK, findings.first().severity)
        assertEquals(Severity.WARN, findings.last().severity)
    }
}
