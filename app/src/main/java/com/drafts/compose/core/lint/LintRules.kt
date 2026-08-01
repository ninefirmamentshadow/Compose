package com.drafts.compose.core.lint

import com.drafts.compose.core.Scope
import com.drafts.compose.core.Severity

/**
 * THE RULES FILE.
 *
 * Every lint rule the app ships with lives in [LintRules.DEFAULT] below. To change
 * what the app flags, edit that list — add an entry, delete an entry, change a
 * regex, change a severity, change a message. Nothing in [LintEngine] knows what
 * any individual rule is about, so the engine never needs to be touched.
 *
 * Four rule shapes are available:
 *  - [PatternRule]          flag every match of one regex
 *  - [SentencePairRule]     flag a sentence where two regexes both match
 *  - [EmojiCeilingRule]     flag emoji past a per-scope budget
 *  - [ParagraphCeilingRule] flag paragraphs past a budget
 *
 * If a new rule needs a shape that does not exist yet, add a new `LintRule`
 * subtype and one `when` branch in the engine. That is the only coupling.
 *
 * Regexes are matched case-insensitively unless a rule opts out.
 */
sealed interface LintRule {
    val id: String
    val severity: Severity
    val message: String
    val scope: Scope

    fun appliesTo(other: Scope): Boolean = scope == Scope.ANY || scope == other
}

/** Flags every match of [pattern]. The match itself is the highlighted span. */
data class PatternRule(
    override val id: String,
    val pattern: Regex,
    override val severity: Severity,
    override val message: String,
    override val scope: Scope = Scope.ANY
) : LintRule

/**
 * Flags a sentence in which [first] and [second] both match. The whole sentence is
 * the highlighted span, because the problem is the pairing, not either half.
 */
data class SentencePairRule(
    override val id: String,
    val first: Regex,
    val second: Regex,
    override val severity: Severity,
    override val message: String,
    override val scope: Scope = Scope.ANY
) : LintRule

/** Flags the first emoji past [max] within a scope. */
data class EmojiCeilingRule(
    override val id: String,
    val max: Int,
    override val severity: Severity,
    override val message: String,
    override val scope: Scope
) : LintRule

/** Flags everything past the [max]th paragraph within a scope. */
data class ParagraphCeilingRule(
    override val id: String,
    val max: Int,
    override val severity: Severity,
    override val message: String,
    override val scope: Scope
) : LintRule

object LintRules {

    private val CI = setOf(RegexOption.IGNORE_CASE)

    // ------------------------------------------------------------------------
    // Shared fragments. These are ordinary English vocabulary — durations, money
    // words, commerce verbs. There is deliberately no vocabulary anywhere in this
    // file describing what is being sold, and none should be added: this app does
    // not carry an act list, and a lint rule is not an exception to that.
    // ------------------------------------------------------------------------

    /** A number that reads as money. */
    val PRICE = Regex(
        "(?:[\\$£€]\\s?\\d{1,4}(?:\\.\\d{2})?|\\b\\d{2,4}\\s?(?:usd|dollars?|bucks|roses)\\b)",
        CI
    )

    /** Duration and commerce language — "what you get for it", generically. */
    val OFFER_TERM = Regex(
        "\\b(?:qv|hh|half[-\\s]?hour|hour|hours|hr|hrs|min|mins|minute|minutes|" +
            "session|visit|appointment|booking|meeting|date|time|rate|rates|deposit|donation|" +
            "includes?|included|including|offers?|offering|provides?|provided|menu|list|" +
            "extra|extras|add[-\\s]?on|upgrade|special|deal|package|covers?)\\b",
        CI
    )

    /** Language that states a limit or a requirement. */
    val BOUNDARY_TERM = Regex(
        "\\b(?:i\\s+don'?t|i\\s+do\\s+not|i\\s+won'?t|i\\s+will\\s+not|i\\s+can'?t|i\\s+cannot|" +
            "no\\s+\\w+|not\\s+available|never|require[sd]?|required|must\\s+\\w+|only\\s+if|" +
            "deposit|screening|screened|verif(?:y|ied|ication)|non[-\\s]?negotiable)\\b",
        CI
    )

    private val QUESTION_MARK = Regex("\\?")

    // ------------------------------------------------------------------------
    // The seeded rules.
    // ------------------------------------------------------------------------

    val DEFAULT: List<LintRule> = listOf(

        PatternRule(
            id = "INCALL_PHRASING",
            pattern = Regex(
                "\\b(?:incall|in-?\\s?call|my\\s+place|come\\s+to\\s+(?:me|mine|my\\b[\\w\\s]{0,12})|" +
                    "host(?:s|ing|ed)?)\\b",
                CI
            ),
            severity = Severity.BLOCK,
            message = "Reads as incall / hosting."
        ),

        PatternRule(
            id = "REALTIME_LOCATION",
            pattern = Regex(
                "\\b(?:here\\s+now|in\\s+town\\s+at|currently\\s+at|room\\s*#?\\s*\\d+)\\b",
                CI
            ),
            severity = Severity.BLOCK,
            message = "States where you are, right now."
        ),

        SentencePairRule(
            id = "PRICE_NEAR_OFFER",
            first = PRICE,
            second = OFFER_TERM,
            severity = Severity.BLOCK,
            message = "A price sits in the same sentence as what it buys."
        ),

        PatternRule(
            id = "DEFENSIVE_PHRASING",
            pattern = Regex(
                "\\b(?:no\\s+time\\s?wasters?|serious\\s+(?:inquir(?:y|ies)|enquir(?:y|ies))\\s+only|" +
                    "serious\\s+only|don'?t\\s+waste\\s+my\\s+time|stop\\s+wasting\\s+my\\s+time|" +
                    "i'?m\\s+not\\s+free|i\\s+am\\s+not\\s+free)\\b",
                CI
            ),
            severity = Severity.WARN,
            message = "Defensive phrasing — reads as braced for a fight."
        ),

        EmojiCeilingRule(
            id = "EMOJI_HEADLINE",
            max = 2,
            severity = Severity.WARN,
            message = "More than 2 emoji in the headline.",
            scope = Scope.HEADLINE
        ),

        EmojiCeilingRule(
            id = "EMOJI_BODY",
            max = 5,
            severity = Severity.WARN,
            message = "More than 5 emoji in the body.",
            scope = Scope.BODY
        ),

        ParagraphCeilingRule(
            id = "BODY_PARAGRAPHS",
            max = 3,
            severity = Severity.WARN,
            message = "Body runs past three paragraphs.",
            scope = Scope.BODY
        ),

        SentencePairRule(
            id = "BOUNDARY_AS_QUESTION",
            first = BOUNDARY_TERM,
            second = QUESTION_MARK,
            severity = Severity.WARN,
            message = "A boundary is phrased as a question."
        )
    )
}
