package com.drafts.compose.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * How a platform expects copy to read. This drives layout only — the renderer
 * reshapes whitespace and joins, it never adds, removes or substitutes words.
 */
enum class Register { SHORT_SCANNABLE, LONG_FORM, BLUNT }

/** The six labelled script slots. Bodies are the operator's own words. */
enum class ScriptLabel { REFUSAL, REDIRECT, PROBE, TRANSPORT, CONFIRMATION, FIRST_REPLY }

/**
 * One source draft. Headline is three segments so they can be varied independently
 * in the kill-file; body is three fields so the renderer can reshape them per
 * platform without guessing where the seams are.
 */
@Entity(tableName = "listing")
data class Listing(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String = "",
    val headlineName: String = "",
    val headlineCategory: String = "",
    val headlineFilter: String = "",
    val bodyWhoYouAre: String = "",
    val bodyHowItWorks: String = "",
    val bodyContact: String = "",
    val lastEdited: Long = 0L
)

/** Seeded with the three platforms, editable — limits and register both. */
@Entity(tableName = "platform_profile")
data class PlatformProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String = "",
    val headlineCharLimit: Int = 0,
    val bodyCharLimit: Int = 0,
    val register: Register = Register.SHORT_SCANNABLE
)

/**
 * A headline that ran on a platform for a date range, and how many inquiries came
 * in while it ran. [inquiryCount] is a count and nothing else — no free text field
 * exists on this table by design.
 */
@Entity(tableName = "headline_test")
data class HeadlineTest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val headlineName: String = "",
    val headlineCategory: String = "",
    val headlineFilter: String = "",
    val platform: String = "",
    val dateStarted: Long = 0L,
    val dateEnded: Long? = null,
    val inquiryCount: Int = 0
)

/**
 * The single row of values every listing is checked against. One row, id fixed
 * at [ROW_ID]; rates of 0 mean "not set" and are skipped by the checker.
 */
@Entity(tableName = "canonical_values")
data class CanonicalValues(
    @PrimaryKey val id: Int = ROW_ID,
    val rateQv: Int = 0,
    val rateHh: Int = 0,
    val rateHour: Int = 0,
    val bioDescriptor: String = "",
    val contactHandle: String = "",
    val contactInstruction: String = ""
) {
    companion object { const val ROW_ID = 1 }
}

/** A canned reply, tapped to copy. */
@Entity(tableName = "script")
data class Script(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val label: ScriptLabel = ScriptLabel.FIRST_REPLY,
    val body: String = ""
)
