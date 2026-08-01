package com.drafts.compose.core.tests

import com.drafts.compose.data.entity.HeadlineTest

/**
 * The one-variable rule for the headline kill-file.
 *
 * If a new test changes more than one headline segment against the last test that
 * ran on the same platform, the result cannot be attributed to either change. The
 * guard says so and makes the operator confirm; it never blocks outright, because
 * sometimes you do want to throw the whole headline out and start again.
 */
object TestGuard {

    /** Shown verbatim in the blocking dialog. */
    const val MESSAGE = "two variables changed, this test won't tell you anything"

    /** The three headline segments of a test about to be started. */
    data class Segments(
        val headlineName: String,
        val headlineCategory: String,
        val headlineFilter: String
    ) {
        companion object {
            fun of(test: HeadlineTest) =
                Segments(test.headlineName, test.headlineCategory, test.headlineFilter)
        }
    }

    enum class SegmentField(val label: String) { NAME("name"), CATEGORY("category"), FILTER("filter") }

    data class Decision(
        val previous: HeadlineTest?,
        val changed: List<SegmentField>
    ) {
        val changedCount: Int get() = changed.size

        /** True when more than one segment moved against the previous test. */
        val requiresConfirmation: Boolean get() = changedCount > 1

        /** e.g. "name, filter" — for the dialog body. */
        val changedLabels: String get() = changed.joinToString(", ") { it.label }
    }

    /**
     * The most recent test on [platform] by start date. Ties break on the higher id,
     * so two tests started the same day still order deterministically.
     */
    fun mostRecentOn(tests: List<HeadlineTest>, platform: String): HeadlineTest? =
        tests.filter { it.platform.trim().equals(platform.trim(), ignoreCase = true) }
            .maxWithOrNull(compareBy({ it.dateStarted }, { it.id }))

    /**
     * Segments are compared trimmed but case-sensitively: a case change is a change
     * to the headline as posted, so it counts as a variable.
     */
    fun evaluate(tests: List<HeadlineTest>, platform: String, next: Segments): Decision {
        val previous = mostRecentOn(tests, platform) ?: return Decision(null, emptyList())
        val before = Segments.of(previous)
        val changed = buildList {
            if (before.headlineName.trim() != next.headlineName.trim()) add(SegmentField.NAME)
            if (before.headlineCategory.trim() != next.headlineCategory.trim()) add(SegmentField.CATEGORY)
            if (before.headlineFilter.trim() != next.headlineFilter.trim()) add(SegmentField.FILTER)
        }
        return Decision(previous, changed)
    }
}
