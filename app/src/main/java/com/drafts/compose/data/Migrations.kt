package com.drafts.compose.data

import androidx.room.migration.Migration

/**
 * Migration scaffolding.
 *
 * The rule for this database is **additive only**. Adding a nullable column, or a
 * column with a default, is safe and is what every migration here should be. Do
 * not drop a column, do not rename one, do not narrow a type: the drafts in this
 * database are the only copy of work that exists, and a destructive migration
 * loses it silently.
 *
 * To add version N+1:
 *  1. bump [AppDatabase.VERSION]
 *  2. add the entity field with a default
 *  3. append a Migration below and add it to [ALL]
 *
 * Template:
 *
 * ```
 * private val MIGRATION_1_2 = object : Migration(1, 2) {
 *     override fun migrate(db: SupportSQLiteDatabase) {
 *         db.execSQL("ALTER TABLE listing ADD COLUMN newField TEXT NOT NULL DEFAULT ''")
 *     }
 * }
 * ```
 *
 * `fallbackToDestructiveMigration` is deliberately never called anywhere in this
 * project. A missing migration should crash in testing, not wipe data in the field.
 */
object Migrations {
    val ALL: Array<Migration> = arrayOf()
}
