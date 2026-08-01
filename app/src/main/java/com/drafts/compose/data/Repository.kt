package com.drafts.compose.data

import android.content.Context
import com.drafts.compose.data.entity.CanonicalValues
import com.drafts.compose.data.entity.HeadlineTest
import com.drafts.compose.data.entity.Listing
import com.drafts.compose.data.entity.PlatformProfile
import com.drafts.compose.data.entity.Script
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Thin wrapper over the DAOs. There is no network layer, no sync, and no export
 * path: everything this class touches stays in one SQLite file on the device.
 */
class Repository(private val db: AppDatabase) {

    val listings: Flow<List<Listing>> = db.listings().observeAll()
    val platforms: Flow<List<PlatformProfile>> = db.platforms().observeAll()
    val scripts: Flow<List<Script>> = db.scripts().observeAll()

    val canonical: Flow<CanonicalValues> =
        db.canonical().observe().map { it ?: CanonicalValues() }

    fun listing(id: Long): Flow<Listing?> = db.listings().observe(id)

    fun testsByDate(): Flow<List<HeadlineTest>> = db.headlineTests().observeByDate()

    fun testsByInquiryCount(): Flow<List<HeadlineTest>> = db.headlineTests().observeByInquiryCount()

    suspend fun mostRecentListingId(): Long? = db.listings().mostRecentId()

    suspend fun listingById(id: Long): Listing? = db.listings().byId(id)

    suspend fun saveListing(listing: Listing) {
        db.listings().update(listing.copy(lastEdited = System.currentTimeMillis()))
    }

    suspend fun createListing(name: String): Long =
        db.listings().insert(Listing(name = name, lastEdited = System.currentTimeMillis()))

    suspend fun deleteListing(listing: Listing) = db.listings().delete(listing)

    suspend fun listingCount(): Int = db.listings().count()

    suspend fun savePlatform(profile: PlatformProfile) = db.platforms().update(profile)

    suspend fun allPlatforms(): List<PlatformProfile> = db.platforms().all()

    suspend fun saveCanonical(values: CanonicalValues) =
        db.canonical().upsert(values.copy(id = CanonicalValues.ROW_ID))

    suspend fun saveScript(script: Script) = db.scripts().update(script)

    suspend fun allTests(): List<HeadlineTest> = db.headlineTests().all()

    suspend fun addTest(test: HeadlineTest): Long = db.headlineTests().insert(test)

    suspend fun updateTest(test: HeadlineTest) = db.headlineTests().update(test)

    suspend fun deleteTest(test: HeadlineTest) = db.headlineTests().delete(test)

    companion object {
        fun from(context: Context) = Repository(AppDatabase.get(context))
    }
}
