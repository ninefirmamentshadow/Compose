package com.drafts.compose.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.drafts.compose.data.entity.CanonicalValues
import com.drafts.compose.data.entity.HeadlineTest
import com.drafts.compose.data.entity.Listing
import com.drafts.compose.data.entity.PlatformProfile
import com.drafts.compose.data.entity.Script
import kotlinx.coroutines.flow.Flow

@Dao
interface ListingDao {
    @Query("SELECT * FROM listing ORDER BY lastEdited DESC, id DESC")
    fun observeAll(): Flow<List<Listing>>

    @Query("SELECT * FROM listing WHERE id = :id")
    fun observe(id: Long): Flow<Listing?>

    @Query("SELECT * FROM listing WHERE id = :id")
    suspend fun byId(id: Long): Listing?

    @Query("SELECT id FROM listing ORDER BY lastEdited DESC, id DESC LIMIT 1")
    suspend fun mostRecentId(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(listing: Listing): Long

    @Update
    suspend fun update(listing: Listing)

    @Delete
    suspend fun delete(listing: Listing)

    @Query("SELECT COUNT(*) FROM listing")
    suspend fun count(): Int
}

@Dao
interface PlatformProfileDao {
    @Query("SELECT * FROM platform_profile ORDER BY id")
    fun observeAll(): Flow<List<PlatformProfile>>

    @Query("SELECT * FROM platform_profile ORDER BY id")
    suspend fun all(): List<PlatformProfile>

    @Update
    suspend fun update(profile: PlatformProfile)
}

@Dao
interface HeadlineTestDao {
    @Query("SELECT * FROM headline_test ORDER BY dateStarted DESC, id DESC")
    fun observeByDate(): Flow<List<HeadlineTest>>

    @Query("SELECT * FROM headline_test ORDER BY inquiryCount DESC, dateStarted DESC")
    fun observeByInquiryCount(): Flow<List<HeadlineTest>>

    @Query("SELECT * FROM headline_test")
    suspend fun all(): List<HeadlineTest>

    @Insert
    suspend fun insert(test: HeadlineTest): Long

    @Update
    suspend fun update(test: HeadlineTest)

    @Delete
    suspend fun delete(test: HeadlineTest)
}

@Dao
interface CanonicalValuesDao {
    @Query("SELECT * FROM canonical_values WHERE id = ${CanonicalValues.ROW_ID}")
    fun observe(): Flow<CanonicalValues?>

    @Upsert
    suspend fun upsert(values: CanonicalValues)
}

@Dao
interface ScriptDao {
    @Query("SELECT * FROM script ORDER BY id")
    fun observeAll(): Flow<List<Script>>

    @Update
    suspend fun update(script: Script)
}
