package com.drafts.compose.core.lint

import com.drafts.compose.core.Finding
import com.drafts.compose.core.FindingSource
import com.drafts.compose.core.ScopedText
import com.drafts.compose.core.TextScan
import com.drafts.compose.core.sortedForDisplay

/**
 * Runs a list of [LintRule] over the headline and body source views.
 *
 * The engine knows the four rule shapes and nothing else — no rule's subject
 * matter is encoded here. Swap [rules] to lint against a different set entirely;
 * that is what the tests do.
 */
class LintEngine(private val rules: List<LintRule> = LintRules.DEFAULT) {

    fun run(headline: ScopedText, body: ScopedText): List<Finding> {
        val out = mutableListOf<Finding>()
        for (doc in listOf(headline, body)) {
            if (doc.text.isEmpty()) continue
            for (rule in rules) {
                if (!rule.appliesTo(doc.scope)) continue
                out += apply(rule, doc)
            }
        }
        return out.sortedForDisplay()
    }

    private fun apply(rule: LintRule, doc: ScopedText): List<Finding> = when (rule) {
        is PatternRule -> pattern(rule, doc)
        is SentencePairRule -> sentencePair(rule, doc)
        is EmojiCeilingRule -> emojiCeiling(rule, doc)
        is ParagraphCeilingRule -> paragraphCeiling(rule, doc)
    }

    private fun pattern(rule: PatternRule, doc: ScopedText): List<Finding> =
        rule.pattern.findAll(doc.text)
            .filter { it.value.isNotEmpty() }
            .map { finding(rule, doc, it.range) }
            .toList()

    private fun sentencePair(rule: SentencePairRule, doc: ScopedText): List<Finding> =
        TextScan.sentences(doc.text)
            .filter { range ->
                val sentence = doc.text.substring(range.first, range.last + 1)
                rule.first.containsMatchIn(sentence) && rule.second.containsMatchIn(sentence)
            }
            .map { finding(rule, doc, it) }

    private fun emojiCeiling(rule: EmojiCeilingRule, doc: ScopedText): List<Finding> {
        val found = TextScan.emoji(doc.text)
        if (found.size <= rule.max) return emptyList()
        val firstExcess = found[rule.max]
        val span = firstExcess.first..found.last().last
        return listOf(finding(rule, doc, span, "${rule.message} Found ${found.size}."))
    }

    private fun paragraphCeiling(rule: ParagraphCeilingRule, doc: ScopedText): List<Finding> {
        val found = TextScan.paragraphs(doc.text)
        if (found.size <= rule.max) return emptyList()
        val span = found[rule.max].first..found.last().last
        return listOf(finding(rule, doc, span, "${rule.message} Found ${found.size}."))
    }

    private fun finding(
        rule: LintRule,
        doc: ScopedText,
        range: IntRange,
        message: String = rule.message
    ): Finding = Finding(
        ruleId = rule.id,
        source = FindingSource.LINT,
        severity = rule.severity,
        message = message,
        span = doc.locate(range),
        excerpt = doc.excerpt(range)
    )
}
