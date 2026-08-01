package com.drafts.compose.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.drafts.compose.core.FieldId
import com.drafts.compose.core.Finding
import com.drafts.compose.core.check.CheckReport
import com.drafts.compose.core.check.Checks
import com.drafts.compose.core.render.Rendering
import com.drafts.compose.core.render.Renderer
import com.drafts.compose.core.tests.TestGuard
import com.drafts.compose.data.Repository
import com.drafts.compose.data.entity.CanonicalValues
import com.drafts.compose.data.entity.HeadlineTest
import com.drafts.compose.data.entity.Listing
import com.drafts.compose.data.entity.PlatformProfile
import com.drafts.compose.data.entity.Script
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Where a CHECK finding sends you when tapped. */
data class JumpTarget(val field: FieldId, val range: IntRange)

/**
 * One view model for the whole app, scoped to the activity, so the CHECK tab is
 * always looking at exactly what the COMPOSE tab has in its fields — including
 * edits that have not hit the database yet.
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository.from(app)

    private val draftFlow = MutableStateFlow<Listing?>(null)
    private val selectedPlatformIdFlow = MutableStateFlow<Long?>(null)

    val listings: LiveData<List<Listing>> = repo.listings.asLiveData()
    val platforms: LiveData<List<PlatformProfile>> = repo.platforms.asLiveData()
    val canonical: LiveData<CanonicalValues> = repo.canonical.asLiveData()
    val scripts: LiveData<List<Script>> = repo.scripts.asLiveData()
    val testsByDate: LiveData<List<HeadlineTest>> = repo.testsByDate().asLiveData()
    val testsByInquiryCount: LiveData<List<HeadlineTest>> = repo.testsByInquiryCount().asLiveData()

    val draft: LiveData<Listing?> = draftFlow.asLiveData()

    val selectedPlatform: LiveData<PlatformProfile?> =
        combine(repo.platforms, selectedPlatformIdFlow) { all, id ->
            all.firstOrNull { it.id == id } ?: all.firstOrNull()
        }.asLiveData()

    val rendering: LiveData<Rendering?> =
        combine(draftFlow, repo.platforms, selectedPlatformIdFlow) { listing, all, id ->
            val platform = all.firstOrNull { it.id == id } ?: all.firstOrNull()
            if (listing == null || platform == null) null else Renderer.render(listing, platform)
        }.asLiveData()

    val report: LiveData<CheckReport> =
        combine(draftFlow, repo.canonical) { listing, canonical ->
            if (listing == null) CheckReport(emptyList()) else Checks.run(listing, canonical)
        }.asLiveData()

    private val _jump = MutableLiveData<JumpTarget?>()
    val jump: LiveData<JumpTarget?> = _jump

    /** The listing as last written to the database, for change detection. */
    private var persisted: Listing? = null
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            val id = repo.mostRecentListingId() ?: repo.createListing("Draft 1")
            select(id)
        }
    }

    // ------------------------------------------------------------- listings

    fun select(id: Long) {
        viewModelScope.launch {
            val listing = repo.listingById(id) ?: return@launch
            saveJob?.cancel()
            persisted = listing
            draftFlow.value = listing
        }
    }

    fun newListing(name: String) {
        viewModelScope.launch { select(repo.createListing(name.ifBlank { "Untitled" })) }
    }

    fun renameActive(name: String) = edit { it.copy(name = name) }

    fun deleteActive(onDeleted: () -> Unit) {
        val current = draftFlow.value ?: return
        viewModelScope.launch {
            repo.deleteListing(current)
            val next = repo.mostRecentListingId() ?: repo.createListing("Draft 1")
            select(next)
            onDeleted()
        }
    }

    // ---------------------------------------------------------------- edits

    fun editField(field: FieldId, text: String) = edit { listing ->
        when (field) {
            FieldId.HEADLINE_NAME -> listing.copy(headlineName = text)
            FieldId.HEADLINE_CATEGORY -> listing.copy(headlineCategory = text)
            FieldId.HEADLINE_FILTER -> listing.copy(headlineFilter = text)
            FieldId.BODY_WHO_YOU_ARE -> listing.copy(bodyWhoYouAre = text)
            FieldId.BODY_HOW_IT_WORKS -> listing.copy(bodyHowItWorks = text)
            FieldId.BODY_CONTACT -> listing.copy(bodyContact = text)
        }
    }

    private fun edit(transform: (Listing) -> Listing) {
        val current = draftFlow.value ?: return
        val updated = transform(current)
        if (updated == current) return
        draftFlow.value = updated
        scheduleSave()
    }

    /**
     * Autosave, coalesced. Every keystroke reschedules; a write happens once the
     * typing pauses, and again on [flush] when the screen goes away.
     */
    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            persistNow()
        }
    }

    fun flush() {
        saveJob?.cancel()
        viewModelScope.launch { persistNow() }
    }

    private suspend fun persistNow() {
        val current = draftFlow.value ?: return
        if (sameContent(current, persisted)) return
        repo.saveListing(current)
        persisted = current
    }

    private fun sameContent(a: Listing, b: Listing?): Boolean =
        b != null && a.copy(lastEdited = 0L) == b.copy(lastEdited = 0L)

    // ------------------------------------------------------------ platforms

    fun selectPlatform(id: Long) {
        selectedPlatformIdFlow.value = id
    }

    fun savePlatform(profile: PlatformProfile) {
        viewModelScope.launch { repo.savePlatform(profile) }
    }

    // ------------------------------------------------------------ canonical

    fun saveCanonical(values: CanonicalValues) {
        viewModelScope.launch { repo.saveCanonical(values) }
    }

    // -------------------------------------------------------------- scripts

    fun saveScript(script: Script) {
        viewModelScope.launch { repo.saveScript(script) }
    }

    // -------------------------------------------------------- headline tests

    /**
     * Runs the one-variable guard for a test about to start. The caller decides
     * what to do with a [TestGuard.Decision] that requires confirmation.
     */
    fun evaluateGuard(
        platform: String,
        segments: TestGuard.Segments,
        onResult: (TestGuard.Decision) -> Unit
    ) {
        viewModelScope.launch { onResult(TestGuard.evaluate(repo.allTests(), platform, segments)) }
    }

    fun startTest(test: HeadlineTest) {
        viewModelScope.launch { repo.addTest(test) }
    }

    fun updateTest(test: HeadlineTest) {
        viewModelScope.launch { repo.updateTest(test) }
    }

    fun deleteTest(test: HeadlineTest) {
        viewModelScope.launch { repo.deleteTest(test) }
    }

    // ----------------------------------------------------------------- jump

    fun requestJump(finding: Finding) {
        _jump.value = JumpTarget(finding.field, finding.range)
    }

    fun consumeJump() {
        _jump.value = null
    }

    companion object {
        private const val SAVE_DEBOUNCE_MS = 400L
    }
}
