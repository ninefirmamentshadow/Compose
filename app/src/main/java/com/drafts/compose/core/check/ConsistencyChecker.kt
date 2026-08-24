package com.drafts.compose.core.check

import com.drafts.compose.core.FieldId
import com.drafts.compose.core.FieldSpan
import com.drafts.compose.core.Finding
import com.drafts.compose.core.FindingSource
import com.drafts.compose.core.ScopedText
import com.drafts.compose.core.Severity
import com.drafts.compose.core.TextScan
import com.drafts.compose.core.sortedForDisplay
import com.drafts.compose.data.entity.CanonicalValues

/**
 * Compares what a listing says against [CanonicalValues] — the one row that holds
 * what the numbers and strings are supposed to be.
 *
 * The checker is deliberately literal. It does not normalise, guess intent, or
 * accept "close enough": drift between platforms is exactly the thing it exists to
 * catch, and a checker that silently tolerates a variant is worse than no checker.
 */
class ConsistencyChecker {

    /** Rate words that make a bare number read as money rather than as a number. */
    private val rateContext = Regex(
        "\\b(?:qv|hh|half[-\\s]?hour|hr|hrs|hour|hours|rate|rates|donation|roses|deposit|" +
            "dollars?|usd|price|priced)\\b",
        RegexOption.IGNORE_CASE
    )

    // \d+ (not \d{1,4}): the old cap silently truncated a longer amount to its
    // first four digits and judged that truncated value instead of the real one
    // ("$12005" read as "$1200"). Capturing the whole run means a canonical rate
    // is compared against the complete stated amount and a wrong price is
    // reported as a mismatch instead of going unevaluated or getting a lucky
    // truncated match.
    private val explicitMoney = Regex("[\\$£€]\\s?(\\d+)(?:\\.\\d{2})?")

    /**
     * A standalone 2–4 digit number. The lookarounds keep it off the fractional part
     * of a decimal and off numbers already carrying a currency symbol, while still
     * matching a number that ends a sentence ("Rate is 175.").
     */
    private val bareNumber = Regex("(?<![\\w\\$£€])(?<!\\d\\.)(\\d{2,4})(?!\\d)(?!\\.\\d)")

    fun run(headline: ScopedText, body: ScopedText, canonical: CanonicalValues): List<Finding> {
        val docs = listOf(headline, body).filter { it.text.isNotEmpty() }
        val out = mutableListOf<Finding>()
        docs.forEach { out += checkRates(it, canonical) }
        docs.forEach { out += checkBioDescriptor(it, canonical) }
        out += checkContact(
            docs = docs,
            canonicalValue = canonical.contactHandle,
            missingRuleId = "CONTACT_HANDLE_MISSING",
            missingMessage = "Canonical contact handle does not appear in this listing.",
            driftRuleId = "CONTACT_HANDLE_DRIFT",
            driftMessage = "Contact handle differs from canonical.",
            severity = Severity.BLOCK
        )
        out += checkContact(
            docs = docs,
            canonicalValue = canonical.contactInstruction,
            missingRuleId = "CONTACT_INSTRUCTION_MISSING",
            missingMessage = "Canonical contact instruction does not appear in this listing.",
            driftRuleId = "CONTACT_INSTRUCTION_DRIFT",
            driftMessage = "Contact instruction differs from canonical.",
            severity = Severity.WARN
        )
        return out.sortedForDisplay()
    }

    // ----------------------------------------------------------------- rates

    private fun checkRates(doc: ScopedText, canonical: CanonicalValues): List<Finding> {
        val known = listOf(canonical.rateQv, canonical.rateHh, canonical.rateHour).filter { it > 0 }
        if (known.isEmpty()) return emptyList()

        val out = mutableListOf<Finding>()
        val claimed = mutableListOf<IntRange>()

        explicitMoney.findAll(doc.text).forEach { match ->
            claimed.add(match.range)
            val value = match.groupValues[1].toIntOrNull() ?: return@forEach
            if (value !in known) out += finding(doc, match.range, canonical, value)
        }

        // A bare number only reads as a rate if its own sentence talks about rates.
        TextScan.sentences(doc.text).forEach { sentence ->
            val text = doc.text.substring(sentence.first, sentence.last + 1)
            if (!rateContext.containsMatchIn(text)) return@forEach
            bareNumber.findAll(text).forEach { match ->
                val absolute = (sentence.first + match.range.first)..(sentence.first + match.range.last)
                // Skip digits already reported as part of an explicit money token
                // ("$ 175" would otherwise be counted twice).
                if (claimed.any { absolute.first in it }) return@forEach
                val value = match.groupValues[1].toIntOrNull() ?: return@forEach
                if (value !in known) out += finding(doc, absolute, canonical, value)
            }
        }
        return out
    }

    private fun finding(
        doc: ScopedText,
        range: IntRange,
        canonical: CanonicalValues,
        value: Int
    ): Finding = Finding(
        ruleId = "RATE_MISMATCH",
        source = FindingSource.CONSISTENCY,
        severity = Severity.BLOCK,
        message = "Reads as a rate but matches no canonical rate (${canonicalRateList(canonical)}). Found $value.",
        span = doc.locate(range),
        excerpt = doc.excerpt(range)
    )

    private fun canonicalRateList(canonical: CanonicalValues): String =
        listOfNotNull(
            canonical.rateQv.takeIf { it > 0 }?.let { "qv $it" },
            canonical.rateHh.takeIf { it > 0 }?.let { "hh $it" },
            canonical.rateHour.takeIf { it > 0 }?.let { "hour $it" }
        ).joinToString(", ").ifEmpty { "none set" }

    // ------------------------------------------------------------ descriptor

    private fun checkBioDescriptor(doc: ScopedText, canonical: CanonicalValues): List<Finding> {
        val canonicalValue = canonical.bioDescriptor
        if (canonicalValue.isBlank()) return emptyList()
        val loose = looseRegex(canonicalValue) ?: return emptyList()

        return loose.findAll(doc.text)
            .filter { it.value != canonicalValue }
            .map { match ->
                Finding(
                    ruleId = "BIO_DESCRIPTOR_DRIFT",
                    source = FindingSource.CONSISTENCY,
                    severity = Severity.WARN,
                    message = "Descriptor is not byte-identical to canonical (\"$canonicalValue\").",
                    span = doc.locate(match.range),
                    excerpt = doc.excerpt(match.range)
                )
            }
            .toList()
    }

    // --------------------------------------------------------------- contact

    private fun checkContact(
        docs: List<ScopedText>,
        canonicalValue: String,
        missingRuleId: String,
        missingMessage: String,
        driftRuleId: String,
        driftMessage: String,
        severity: Severity
    ): List<Finding> {
        if (canonicalValue.isBlank()) return emptyList()
        if (docs.any { it.text.contains(canonicalValue) }) return emptyList()

        val loose = looseRegex(canonicalValue, allowMissingAt = true)
        if (loose != null) {
            for (doc in docs) {
                val match = loose.find(doc.text)
                if (match != null) {
                    return listOf(
                        Finding(
                            ruleId = driftRuleId,
                            source = FindingSource.CONSISTENCY,
                            severity = severity,
                            message = "$driftMessage Canonical is \"$canonicalValue\".",
                            span = doc.locate(match.range),
                            excerpt = doc.excerpt(match.range)
                        )
                    )
                }
            }
        }

        return listOf(
            Finding(
                ruleId = missingRuleId,
                source = FindingSource.CONSISTENCY,
                severity = severity,
                message = "$missingMessage Canonical is \"$canonicalValue\".",
                span = FieldSpan(FieldId.BODY_CONTACT, IntRange.EMPTY),
                excerpt = ""
            )
        )
    }

    /**
     * A case-insensitive, whitespace-flexible pattern for [literal] — so "Jo Smith"
     * still matches "jo  smith" and gets reported as drift rather than slipping
     * past as absent.
     */
    private fun looseRegex(literal: String, allowMissingAt: Boolean = false): Regex? {
        val tokens = literal.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null
        val body = tokens.joinToString("\\s+") { Regex.escape(it) }
        val pattern = if (allowMissingAt && literal.trimStart().startsWith("@")) {
            "@?" + tokens.joinToString("\\s+") { Regex.escape(it.removePrefix("@")) }
        } else {
            body
        }
        // Bounded so the literal can't match as a substring of a longer, unrelated
        // word ("@shop" inside "bookshop") and get reported as drift instead of
        // correctly falling through to missing.
        return Regex("(?<![\\w@])$pattern(?!\\w)", RegexOption.IGNORE_CASE)
    }
}
