package com.drafts.compose.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.drafts.compose.data.dao.CanonicalValuesDao
import com.drafts.compose.data.dao.HeadlineTestDao
import com.drafts.compose.data.dao.ListingDao
import com.drafts.compose.data.dao.PlatformProfileDao
import com.drafts.compose.data.dao.ScriptDao
import com.drafts.compose.data.entity.CanonicalValues
import com.drafts.compose.data.entity.HeadlineTest
import com.drafts.compose.data.entity.Listing
import com.drafts.compose.data.entity.PlatformProfile
import com.drafts.compose.data.entity.Script

@Database(
    entities = [
        Listing::class,
        PlatformProfile::class,
        HeadlineTest::class,
        CanonicalValues::class,
        Script::class
    ],
    version = AppDatabase.VERSION,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun listings(): ListingDao
    abstract fun platforms(): PlatformProfileDao
    abstract fun headlineTests(): HeadlineTestDao
    abstract fun canonical(): CanonicalValuesDao
    abstract fun scripts(): ScriptDao

    companion object {
        const val VERSION = 1
        private const val NAME = "drafts.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

        private fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                .addMigrations(*Migrations.ALL)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        Seed.onCreate(db)
                    }
                })
                .build()
    }
}
