package com.drafts.compose.core

/**
 * Severity of a finding. BLOCK means "do not post this as written"; WARN means
 * "look at this before you post". Nothing in the app enforces either — the app
 * reports, the operator decides.
 */
enum class Severity { BLOCK, WARN }

/** Which pass produced a finding. */
enum class FindingSource { CONSISTENCY, LINT }

/**
 * The six editable source fields. Order is display order; findings sort by it.
 */
enum class FieldId(val label: String, val scope: Scope) {
    HEADLINE_NAME("Headline · name", Scope.HEADLINE),
    HEADLINE_CATEGORY("Headline · category", Scope.HEADLINE),
    HEADLINE_FILTER("Headline · filter", Scope.HEADLINE),
    BODY_WHO_YOU_ARE("Body · who you are", Scope.BODY),
    BODY_HOW_IT_WORKS("Body · how it works", Scope.BODY),
    BODY_CONTACT("Body · contact", Scope.BODY);
}

/** Whether a rule looks at the headline segments, the body fields, or both. */
enum class Scope { HEADLINE, BODY, ANY }

/**
 * A span within a single source field. An empty [range] means "this finding is
 * about the field as a whole" (e.g. a required value is absent entirely).
 */
data class FieldSpan(val field: FieldId, val range: IntRange) {
    val isWholeField: Boolean get() = range.isEmpty()
}

/**
 * One reported problem. [excerpt] is the exact offending substring, so the UI can
 * highlight it without re-deriving it, and [span] is what a tap jumps to.
 */
data class Finding(
    val ruleId: String,
    val source: FindingSource,
    val severity: Severity,
    val message: String,
    val span: FieldSpan,
    val excerpt: String
) {
    val field: FieldId get() = span.field
    val range: IntRange get() = span.range
}

/**
 * BLOCK before WARN, then source-field order, then position in the field.
 * Stable so the CHECK list does not shuffle between runs.
 */
fun List<Finding>.sortedForDisplay(): List<Finding> = sortedWith(
    compareBy(
        { it.severity.ordinal },
        { it.field.ordinal },
        { if (it.range.isEmpty()) -1 else it.range.first },
        { it.ruleId }
    )
)
