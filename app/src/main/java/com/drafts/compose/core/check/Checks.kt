package com.drafts.compose.core.check

import com.drafts.compose.core.Finding
import com.drafts.compose.core.Severity
import com.drafts.compose.core.lint.LintEngine
import com.drafts.compose.core.lint.LintRule
import com.drafts.compose.core.lint.LintRules
import com.drafts.compose.core.render.Renderer
import com.drafts.compose.core.sortedForDisplay
import com.drafts.compose.data.entity.CanonicalValues
import com.drafts.compose.data.entity.Listing

/** Both passes over one listing, in the order the CHECK tab shows them. */
data class CheckReport(val findings: List<Finding>) {
    val blocks: Int = findings.count { it.severity == Severity.BLOCK }
    val warns: Int = findings.count { it.severity == Severity.WARN }
    val isClean: Boolean = findings.isEmpty()
}

object Checks {

    fun run(
        listing: Listing,
        canonical: CanonicalValues,
        rules: List<LintRule> = LintRules.DEFAULT
    ): CheckReport {
        val headline = Renderer.headlineSource(listing)
        val body = Renderer.bodySource(listing)
        val findings = ConsistencyChecker().run(headline, body, canonical) +
            LintEngine(rules).run(headline, body)
        return CheckReport(findings.sortedForDisplay())
    }
}
