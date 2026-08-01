package com.drafts.compose.data

import androidx.room.TypeConverter
import com.drafts.compose.data.entity.Register
import com.drafts.compose.data.entity.ScriptLabel

/**
 * Enums are stored as their names, not their ordinals, so reordering an enum can
 * never silently reinterpret existing rows.
 */
class Converters {
    @TypeConverter
    fun fromRegister(value: Register): String = value.name

    @TypeConverter
    fun toRegister(value: String): Register =
        runCatching { Register.valueOf(value) }.getOrDefault(Register.SHORT_SCANNABLE)

    @TypeConverter
    fun fromScriptLabel(value: ScriptLabel): String = value.name

    @TypeConverter
    fun toScriptLabel(value: String): ScriptLabel =
        runCatching { ScriptLabel.valueOf(value) }.getOrDefault(ScriptLabel.FIRST_REPLY)
}
