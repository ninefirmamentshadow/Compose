package com.drafts.compose.core.render

import com.drafts.compose.core.FieldId
import com.drafts.compose.core.Scope
import com.drafts.compose.core.ScopedText
import com.drafts.compose.core.TextScan
import com.drafts.compose.data.entity.Listing
import com.drafts.compose.data.entity.PlatformProfile
import com.drafts.compose.data.entity.Register

/**
 * One draft rendered for one platform, with the limits it is measured against.
 */
data class Rendering(
    val platformName: String,
    val headline: String,
    val body: String,
    val headlineCharLimit: Int,
    val bodyCharLimit: Int
) {
    val full: String = listOf(headline, body).filter { it.isNotBlank() }.joinToString("\n\n")

    val headlineCount: Int get() = headline.length
    val bodyCount: Int get() = body.length

    val headlineOverBy: Int get() = if (headlineCharLimit <= 0) 0 else (headlineCount - headlineCharLimit).coerceAtLeast(0)
    val bodyOverBy: Int get() = if (bodyCharLimit <= 0) 0 else (bodyCount - bodyCharLimit).coerceAtLeast(0)

    val headlineOverLimit: Boolean get() = headlineOverBy > 0
    val bodyOverLimit: Boolean get() = bodyOverBy > 0
}

/**
 * Reshapes one source draft into each platform's expected shape.
 *
 * This is a formatter, not a writer. Every character it emits came from a field the
 * operator typed. It joins, splits and normalises whitespace; it never adds words,
 * removes words, abbreviates, or substitutes one token for another.
 */
object Renderer {

    /** Fixed by the headline model: three segments, pipe-delimited. */
    const val SEGMENT_SEPARATOR = " | "

    private const val BODY_PARAGRAPH_SEPARATOR = "\n\n"

    fun headline(listing: Listing): String =
        listOf(listing.headlineName, listing.headlineCategory, listing.headlineFilter)
            .map { TextScan.collapseWhitespace(it) }
            .filter { it.isNotEmpty() }
            .joinToString(SEGMENT_SEPARATOR)

    /**
     * Body shape per register:
     *  - SHORT_SCANNABLE: each field collapsed to one line, one line per field.
     *  - LONG_FORM: each field keeps its own line breaks, blank line between fields.
     *  - BLUNT: everything collapsed into a single continuous paragraph.
     */
    fun body(listing: Listing, register: Register): String {
        val fields = listOf(listing.bodyWhoYouAre, listing.bodyHowItWorks, listing.bodyContact)
        return when (register) {
            Register.SHORT_SCANNABLE ->
                fields.map { TextScan.collapseWhitespace(it) }.filter { it.isNotEmpty() }.joinToString("\n")

            Register.LONG_FORM ->
                fields.map { TextScan.collapseInlineWhitespace(it) }.filter { it.isNotEmpty() }
                    .joinToString(BODY_PARAGRAPH_SEPARATOR)

            Register.BLUNT ->
                fields.map { TextScan.collapseWhitespace(it) }.filter { it.isNotEmpty() }.joinToString(" ")
        }
    }

    fun render(listing: Listing, platform: PlatformProfile): Rendering = Rendering(
        platformName = platform.name,
        headline = headline(listing),
        body = body(listing, platform.register),
        headlineCharLimit = platform.headlineCharLimit,
        bodyCharLimit = platform.bodyCharLimit
    )

    // ------------------------------------------------------------------------
    // Source views. Checks run against these — raw field text, joined but never
    // reshaped — so every offset in a finding is a valid offset into an EditText.
    // ------------------------------------------------------------------------

    fun headlineSource(listing: Listing): ScopedText = ScopedText.join(
        scope = Scope.HEADLINE,
        separator = SEGMENT_SEPARATOR,
        segments = listOf(
            FieldId.HEADLINE_NAME to listing.headlineName,
            FieldId.HEADLINE_CATEGORY to listing.headlineCategory,
            FieldId.HEADLINE_FILTER to listing.headlineFilter
        )
    )

    fun bodySource(listing: Listing): ScopedText = ScopedText.join(
        scope = Scope.BODY,
        separator = BODY_PARAGRAPH_SEPARATOR,
        segments = listOf(
            FieldId.BODY_WHO_YOU_ARE to listing.bodyWhoYouAre,
            FieldId.BODY_HOW_IT_WORKS to listing.bodyHowItWorks,
            FieldId.BODY_CONTACT to listing.bodyContact
        )
    )
}
