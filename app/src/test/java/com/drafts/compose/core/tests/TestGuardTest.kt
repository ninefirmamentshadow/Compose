package com.drafts.compose.core.tests

import com.drafts.compose.data.entity.HeadlineTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TestGuardTest {

    private fun test(
        id: Long,
        platform: String = "STG",
        name: String = "Dana",
        category: String = "Evenings",
        filter: String = "North",
        started: Long = 1_000L,
        inquiries: Int = 0
    ) = HeadlineTest(
        id = id,
        headlineName = name,
        headlineCategory = category,
        headlineFilter = filter,
        platform = platform,
        dateStarted = started,
        dateEnded = null,
        inquiryCount = inquiries
    )

    private val segments = TestGuard.Segments("Dana", "Evenings", "North")

    // ------------------------------------------------------ previous test

    @Test
    fun `the first test on a platform is never guarded`() {
        val decision = TestGuard.evaluate(emptyList(), "STG", TestGuard.Segments("A", "B", "C"))
        assertNull(decision.previous)
        assertEquals(0, decision.changedCount)
        assertFalse(decision.requiresConfirmation)
    }

    @Test
    fun `the previous test is the most recently started on that platform`() {
        val tests = listOf(
            test(id = 1, started = 100),
            test(id = 2, started = 300),
            test(id = 3, started = 200)
        )
        assertEquals(2L, TestGuard.mostRecentOn(tests, "STG")?.id)
    }

    @Test
    fun `same-day tests break the tie on the higher id`() {
        val tests = listOf(test(id = 1, started = 100), test(id = 2, started = 100))
        assertEquals(2L, TestGuard.mostRecentOn(tests, "STG")?.id)
    }

    @Test
    fun `tests on other platforms are ignored`() {
        val tests = listOf(
            test(id = 1, platform = "STG", started = 100),
            test(id = 2, platform = "TRYST", started = 900, name = "Other")
        )
        assertEquals(1L, TestGuard.mostRecentOn(tests, "STG")?.id)
        assertNull(TestGuard.mostRecentOn(tests, "LISTCRAWLER"))
    }

    @Test
    fun `platform matching tolerates case and padding`() {
        val tests = listOf(test(id = 1, platform = " stg "))
        assertEquals(1L, TestGuard.mostRecentOn(tests, "STG")?.id)
    }

    // --------------------------------------------------------- the guard

    @Test
    fun `changing nothing needs no confirmation`() {
        val decision = TestGuard.evaluate(listOf(test(id = 1)), "STG", segments)
        assertEquals(0, decision.changedCount)
        assertFalse(decision.requiresConfirmation)
    }

    @Test
    fun `changing one segment needs no confirmation`() {
        listOf(
            segments.copy(headlineName = "Other"),
            segments.copy(headlineCategory = "Other"),
            segments.copy(headlineFilter = "Other")
        ).forEach { next ->
            val decision = TestGuard.evaluate(listOf(test(id = 1)), "STG", next)
            assertEquals(1, decision.changedCount)
            assertFalse("one variable is a valid test", decision.requiresConfirmation)
        }
    }

    @Test
    fun `changing two segments requires confirmation`() {
        val next = segments.copy(headlineName = "Other", headlineFilter = "Elsewhere")
        val decision = TestGuard.evaluate(listOf(test(id = 1)), "STG", next)
        assertEquals(2, decision.changedCount)
        assertTrue(decision.requiresConfirmation)
        assertEquals("name, filter", decision.changedLabels)
    }

    @Test
    fun `changing all three segments requires confirmation`() {
        val next = TestGuard.Segments("A", "B", "C")
        val decision = TestGuard.evaluate(listOf(test(id = 1)), "STG", next)
        assertEquals(3, decision.changedCount)
        assertTrue(decision.requiresConfirmation)
        assertEquals("name, category, filter", decision.changedLabels)
    }

    @Test
    fun `the guard compares against the previous test on the same platform only`() {
        val tests = listOf(
            test(id = 1, platform = "STG", started = 100),
            test(id = 2, platform = "TRYST", started = 900, name = "X", category = "Y", filter = "Z")
        )
        val decision = TestGuard.evaluate(tests, "STG", segments)
        assertEquals(1L, decision.previous?.id)
        assertFalse(decision.requiresConfirmation)
    }

    @Test
    fun `surrounding whitespace is not a variable`() {
        val next = TestGuard.Segments("  Dana  ", "Evenings\n", " North")
        assertEquals(0, TestGuard.evaluate(listOf(test(id = 1)), "STG", next).changedCount)
    }

    @Test
    fun `a case change is a variable because the posted headline changes`() {
        val next = segments.copy(headlineName = "DANA", headlineCategory = "EVENINGS")
        val decision = TestGuard.evaluate(listOf(test(id = 1)), "STG", next)
        assertEquals(2, decision.changedCount)
        assertTrue(decision.requiresConfirmation)
    }

    @Test
    fun `emptying a segment counts as changing it`() {
        val next = segments.copy(headlineCategory = "", headlineFilter = "")
        assertTrue(TestGuard.evaluate(listOf(test(id = 1)), "STG", next).requiresConfirmation)
    }

    @Test
    fun `the dialog message is the one specified`() {
        assertEquals("two variables changed, this test won't tell you anything", TestGuard.MESSAGE)
    }

    @Test
    fun `segments can be read off an existing test`() {
        assertEquals(segments, TestGuard.Segments.of(test(id = 1)))
    }
}
