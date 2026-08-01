package com.drafts.compose.core

import com.drafts.compose.core.check.ConsistencyChecker
import com.drafts.compose.core.lint.LintEngine
import com.drafts.compose.core.lint.LintRule
import com.drafts.compose.core.lint.LintRules
import com.drafts.compose.core.render.Renderer
import com.drafts.compose.data.entity.CanonicalValues
import com.drafts.compose.data.entity.Listing

/**
 * Neutral fixtures. Nothing in this file, or anywhere in the test suite, contains
 * example listing content beyond filler — the same constraint the shipped app is
 * held to.
 */
object Fixtures {

    fun listing(
        name: String = "",
        category: String = "",
        filter: String = "",
        who: String = "",
        how: String = "",
        contact: String = ""
    ) = Listing(
        id = 1L,
        name = "fixture",
        headlineName = name,
        headlineCategory = category,
        headlineFilter = filter,
        bodyWhoYouAre = who,
        bodyHowItWorks = how,
        bodyContact = contact,
        lastEdited = 0L
    )

    /** A draft that trips no seeded rule — the baseline every rule test contrasts with. */
    fun cleanListing() = listing(
        name = "Dana",
        category = "Evenings",
        filter = "Daytime",
        who = "Independent, established, and reachable during posted times.",
        how = "Message first with your details. Screening takes a few minutes.",
        contact = "Reach me at @placeholder.handle before noon."
    )

    fun lint(listing: Listing, rules: List<LintRule> = LintRules.DEFAULT): List<Finding> =
        LintEngine(rules).run(Renderer.headlineSource(listing), Renderer.bodySource(listing))

    fun check(listing: Listing, canonical: CanonicalValues): List<Finding> =
        ConsistencyChecker().run(
            Renderer.headlineSource(listing),
            Renderer.bodySource(listing),
            canonical
        )

    val canonicalRatesOnly = CanonicalValues(
        rateQv = 80,
        rateHh = 150,
        rateHour = 250,
        bioDescriptor = "",
        contactHandle = "",
        contactInstruction = ""
    )

    val canonicalFull = CanonicalValues(
        rateQv = 80,
        rateHh = 150,
        rateHour = 250,
        bioDescriptor = "independent and established",
        contactHandle = "@placeholder.handle",
        contactInstruction = "text before noon"
    )
}

/** Assertion helpers — findings are compared by rule id, never by list order. */
fun List<Finding>.ids(): List<String> = map { it.ruleId }

fun List<Finding>.forRule(id: String): List<Finding> = filter { it.ruleId == id }

fun List<Finding>.hasRule(id: String): Boolean = any { it.ruleId == id }
