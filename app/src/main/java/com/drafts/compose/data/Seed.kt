package com.drafts.compose.data

import androidx.sqlite.db.SupportSQLiteDatabase
import com.drafts.compose.data.entity.CanonicalValues

/**
 * First-run rows.
 *
 * Everything seeded here is structural — platform names, limit placeholders, empty
 * canonical values, and six empty-labelled script slots. No seeded row contains
 * listing copy, and none should ever be added: the app formats what the operator
 * writes, it does not supply anything to say.
 *
 * The script bodies below are neutral scaffolding with braces to fill in. They
 * describe handling a message, not what is being sold.
 */
object Seed {

    /**
     * Character limits are placeholders. They are editable in the app because only
     * the operator knows what each platform currently enforces.
     */
    private val PLATFORMS = listOf(
        Triple("STG", 60 to 1500, "SHORT_SCANNABLE"),
        Triple("TRYST", 90 to 4000, "LONG_FORM"),
        Triple("LISTCRAWLER", 50 to 1000, "BLUNT")
    )

    private val SCRIPTS = listOf(
        "FIRST_REPLY" to "Thanks for reaching out. Here is what I need to move forward: {details}.",
        "PROBE" to "Before I can answer that, tell me {what you need to know}.",
        "REFUSAL" to "That isn't something I do. Best of luck finding what you're after.",
        "REDIRECT" to "I'd rather keep this to {channel}. Send it there and I'll pick it up.",
        "TRANSPORT" to "Send the address and I'll confirm once I'm on my way.",
        "CONFIRMATION" to "Confirming {day} at {time}. Reply to confirm, or tell me if it's changed."
    )

    fun onCreate(db: SupportSQLiteDatabase) {
        PLATFORMS.forEach { (name, limits, register) ->
            val (headline, body) = limits
            db.execSQL(
                "INSERT INTO platform_profile (name, headlineCharLimit, bodyCharLimit, register) " +
                    "VALUES (?, ?, ?, ?)",
                arrayOf(name, headline, body, register)
            )
        }

        SCRIPTS.forEach { (label, body) ->
            db.execSQL("INSERT INTO script (label, body) VALUES (?, ?)", arrayOf(label, body))
        }

        db.execSQL(
            "INSERT INTO canonical_values (id, rateQv, rateHh, rateHour, bioDescriptor, " +
                "contactHandle, contactInstruction) VALUES (?, 0, 0, 0, '', '', '')",
            arrayOf(CanonicalValues.ROW_ID)
        )

        db.execSQL(
            "INSERT INTO listing (name, headlineName, headlineCategory, headlineFilter, " +
                "bodyWhoYouAre, bodyHowItWorks, bodyContact, lastEdited) " +
                "VALUES ('Draft 1', '', '', '', '', '', '', ?)",
            arrayOf(System.currentTimeMillis())
        )
    }
}
