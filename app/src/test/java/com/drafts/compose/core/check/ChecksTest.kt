package com.drafts.compose.core.check

import com.drafts.compose.core.FindingSource
import com.drafts.compose.core.Fixtures
import com.drafts.compose.core.Severity
import com.drafts.compose.core.hasRule
import com.drafts.compose.core.lint.LintRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The two passes as the CHECK tab runs them: together, over one draft. */
class ChecksTest {

    @Test
    fun `a clean draft against matching canonical values reports nothing`() {
        val listing = Fixtures.listing(
            name = "Dana",
            category = "Evenings",
            who = "independent and established",
            how = "Rates are 80, 150 and 250.",
            contact = "@placeholder.handle — text before noon."
        )
        val report = Checks.run(listing, Fixtures.canonicalFull)
        assertTrue(report.findings.map { it.ruleId }.toString(), report.isClean)
        assertEquals(0, report.blocks)
        assertEquals(0, report.warns)
    }

    @Test
    fun `both passes contribute to one report`() {
        val listing = Fixtures.listing(
            who = "I host during the week.",
            how = "Rate is 175.",
            contact = "No handle here."
        )
        val report = Checks.run(listing, Fixtures.canonicalFull)

        assertTrue("lint pass ran", report.findings.hasRule("INCALL_PHRASING"))
        assertTrue("consistency pass ran", report.findings.hasRule("RATE_MISMATCH"))
        assertTrue(report.findings.any { it.source == FindingSource.LINT })
        assertTrue(report.findings.any { it.source == FindingSource.CONSISTENCY })
    }

    @Test
    fun `blocks are counted separately from warns and sort first`() {
        val listing = Fixtures.listing(
            who = "I host during the week.",
            how = "No timewasters.",
            contact = "@placeholder.handle — text before noon."
        )
        val report = Checks.run(listing, Fixtures.canonicalFull)

        assertTrue(report.blocks >= 1)
        assertTrue(report.warns >= 1)
        assertEquals(report.blocks + report.warns, report.findings.size)
        assertEquals(Severity.BLOCK, report.findings.first().severity)
        assertEquals(Severity.WARN, report.findings.last().severity)
    }

    @Test
    fun `an alternative rule set replaces the seeded lint rules only`() {
        val listing = Fixtures.listing(who = "I host during the week. Rate is 175.")
        val report = Checks.run(listing, Fixtures.canonicalRatesOnly, rules = emptyList())

        assertFalse("lint rules were replaced", report.findings.hasRule("INCALL_PHRASING"))
        assertTrue("the consistency pass is not configurable away", report.findings.hasRule("RATE_MISMATCH"))
    }

    @Test
    fun `an empty draft with unset canonical values is clean`() {
        assertTrue(Checks.run(Fixtures.listing(), Fixtures.canonicalRatesOnly).isClean)
    }

    @Test
    fun `the shipped rule set is what CHECK runs by default`() {
        val listing = Fixtures.listing(who = "I host during the week.")
        val default = Checks.run(listing, Fixtures.canonicalRatesOnly)
        val explicit = Checks.run(listing, Fixtures.canonicalRatesOnly, LintRules.DEFAULT)
        assertEquals(default.findings, explicit.findings)
    }
}
