package com.drafts.compose.core

import org.junit.Assert.assertEquals
import org.junit.Test

class FindingsTest {

    private fun finding(
        severity: Severity = Severity.WARN,
        field: FieldId = FieldId.HEADLINE_NAME,
        range: IntRange = IntRange.EMPTY,
        ruleId: String = "rule",
        message: String = ""
    ) = Finding(
        ruleId = ruleId,
        source = FindingSource.LINT,
        severity = severity,
        message = message,
        span = FieldSpan(field, range),
        excerpt = ""
    )

    @Test
    fun `sorts block before warn`() {
        val warn = finding(severity = Severity.WARN)
        val block = finding(severity = Severity.BLOCK)

        val list = listOf(warn, block)
        val sorted = list.sortedForDisplay()

        assertEquals(listOf(block, warn), sorted)
    }

    @Test
    fun `sorts by field ordinal when severities are equal`() {
        val body = finding(field = FieldId.BODY_WHO_YOU_ARE)
        val headline = finding(field = FieldId.HEADLINE_NAME)

        val list = listOf(body, headline)
        val sorted = list.sortedForDisplay()

        assertEquals(listOf(headline, body), sorted)
    }

    @Test
    fun `sorts whole field errors before specific ranges`() {
        val specific = finding(range = 5..10)
        val whole = finding(range = IntRange.EMPTY)

        val list = listOf(specific, whole)
        val sorted = list.sortedForDisplay()

        assertEquals(listOf(whole, specific), sorted)
    }

    @Test
    fun `sorts by range start position`() {
        val later = finding(range = 10..15)
        val earlier = finding(range = 2..5)

        val list = listOf(later, earlier)
        val sorted = list.sortedForDisplay()

        assertEquals(listOf(earlier, later), sorted)
    }

    @Test
    fun `preserves insertion order when primary properties are equal`() {
        // Same severity, field, range, and ruleId
        val f1 = finding(message = "first")
        val f2 = finding(message = "second")

        val list = listOf(f2, f1)
        val sorted = list.sortedForDisplay()

        // Should preserve the order they were in the list
        assertEquals(listOf(f2, f1), sorted)
    }

    @Test
    fun `full sort verification`() {
        val f1 = finding(severity = Severity.WARN, field = FieldId.BODY_WHO_YOU_ARE, range = 5..10, ruleId = "a")
        val f2 = finding(severity = Severity.BLOCK, field = FieldId.HEADLINE_NAME, range = IntRange.EMPTY, ruleId = "a")
        val f3 = finding(severity = Severity.WARN, field = FieldId.HEADLINE_NAME, range = 0..5, ruleId = "a")
        val f4 = finding(severity = Severity.WARN, field = FieldId.BODY_WHO_YOU_ARE, range = 2..8, ruleId = "a")
        val f5 = finding(severity = Severity.BLOCK, field = FieldId.HEADLINE_CATEGORY, range = IntRange.EMPTY, ruleId = "a")
        val f6 = finding(severity = Severity.BLOCK, field = FieldId.HEADLINE_NAME, range = IntRange.EMPTY, ruleId = "a", message = "f6")

        val list = listOf(f1, f2, f3, f4, f5, f6)

        // Expected order:
        // 1. BLOCKs first: f2, f6, f5
        //    f2 (HEADLINE_NAME, EMPTY) vs f6 (HEADLINE_NAME, EMPTY) -> preserve insertion order -> f2 then f6
        //    f5 (HEADLINE_CATEGORY) -> comes after HEADLINE_NAME
        //    So BLOCKs: f2, f6, f5
        // 2. WARNs next: f1, f3, f4
        //    f3 (HEADLINE_NAME) comes first
        //    f4 (BODY_WHO_YOU_ARE, 2..8) comes before f1 (BODY_WHO_YOU_ARE, 5..10)
        //    So WARNs: f3, f4, f1
        // Total expected: f2, f6, f5, f3, f4, f1

        val expected = listOf(f2, f6, f5, f3, f4, f1)
        assertEquals(expected, list.sortedForDisplay())
    }
}
