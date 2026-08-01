package com.drafts.compose.ui.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.drafts.compose.R
import com.drafts.compose.core.FieldId
import com.drafts.compose.core.render.Rendering
import com.drafts.compose.core.render.Renderer
import com.drafts.compose.data.entity.Listing
import com.drafts.compose.data.entity.PlatformProfile
import com.drafts.compose.databinding.DialogPlatformBinding
import com.drafts.compose.databinding.DialogSingleFieldBinding
import com.drafts.compose.databinding.FragmentComposeBinding
import com.drafts.compose.ui.JumpTarget
import com.drafts.compose.ui.MainViewModel
import com.drafts.compose.ui.common.Clip
import com.google.android.material.chip.Chip

/**
 * The editing surface. Six source fields at the top, one platform's rendering of
 * them at the bottom, and three copy buttons that are the whole point of the app.
 */
class ComposeFragment : Fragment() {

    private var _binding: FragmentComposeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    /** Guards the text watchers while fields are being populated from the model. */
    private var populating = false

    /** The listing currently shown in the fields, so we repopulate only on a swap. */
    private var shownListingId: Long? = null

    private var listingIds: List<Long> = emptyList()
    private var suppressSpinner = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComposeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        wireFields()
        wireListingControls()
        wireCopyButtons()

        viewModel.draft.observe(viewLifecycleOwner) { listing ->
            if (listing == null) return@observe
            if (shownListingId != listing.id) {
                shownListingId = listing.id
                populate(listing)
                syncSpinnerSelection(listing.id)
            }
            binding.headlinePreview.text = Renderer.headline(listing)
        }

        viewModel.listings.observe(viewLifecycleOwner) { listings -> bindListings(listings) }

        viewModel.platforms.observe(viewLifecycleOwner) { platforms -> bindPlatformChips(platforms) }

        viewModel.selectedPlatform.observe(viewLifecycleOwner) { platform ->
            binding.editPlatform.isEnabled = platform != null
            checkChipFor(platform)
        }

        viewModel.rendering.observe(viewLifecycleOwner) { rendering -> bindRendering(rendering) }

        viewModel.jump.observe(viewLifecycleOwner) { target -> target?.let(::applyJump) }
    }

    override fun onPause() {
        super.onPause()
        viewModel.flush()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ----------------------------------------------------------------- fields

    private fun editorFor(field: FieldId): EditText = when (field) {
        FieldId.HEADLINE_NAME -> binding.editHeadlineName
        FieldId.HEADLINE_CATEGORY -> binding.editHeadlineCategory
        FieldId.HEADLINE_FILTER -> binding.editHeadlineFilter
        FieldId.BODY_WHO_YOU_ARE -> binding.editBodyWho
        FieldId.BODY_HOW_IT_WORKS -> binding.editBodyHow
        FieldId.BODY_CONTACT -> binding.editBodyContact
    }

    private fun wireFields() {
        FieldId.entries.forEach { field ->
            editorFor(field).doAfterTextChanged { text ->
                if (!populating) viewModel.editField(field, text?.toString().orEmpty())
            }
        }
    }

    private fun populate(listing: Listing) {
        populating = true
        binding.editHeadlineName.setText(listing.headlineName)
        binding.editHeadlineCategory.setText(listing.headlineCategory)
        binding.editHeadlineFilter.setText(listing.headlineFilter)
        binding.editBodyWho.setText(listing.bodyWhoYouAre)
        binding.editBodyHow.setText(listing.bodyHowItWorks)
        binding.editBodyContact.setText(listing.bodyContact)
        populating = false
    }

    // --------------------------------------------------------------- listings

    private fun wireListingControls() {
        binding.addListing.setOnClickListener { promptNewListing() }
        binding.listingMenu.setOnClickListener { showListingMenu() }
        binding.listingSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (suppressSpinner) return
                listingIds.getOrNull(position)?.let { viewModel.select(it) }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun bindListings(listings: List<Listing>) {
        listingIds = listings.map { it.id }
        val names = listings.map { it.name.ifBlank { "Untitled" } }
        suppressSpinner = true
        binding.listingSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            names
        )
        shownListingId?.let { syncSpinnerSelection(it) }
        suppressSpinner = false
    }

    private fun syncSpinnerSelection(id: Long) {
        val index = listingIds.indexOf(id)
        if (index >= 0 && binding.listingSpinner.selectedItemPosition != index) {
            suppressSpinner = true
            binding.listingSpinner.setSelection(index)
            suppressSpinner = false
        }
    }

    private fun promptNewListing() {
        textPrompt(getString(R.string.new_listing), "") { viewModel.newListing(it) }
    }

    private fun showListingMenu() {
        val options = arrayOf(getString(R.string.rename_listing), getString(R.string.delete_listing))
        AlertDialog.Builder(requireContext())
            .setItems(options) { _, which ->
                when (which) {
                    0 -> textPrompt(
                        getString(R.string.rename_listing),
                        viewModel.draft.value?.name.orEmpty()
                    ) { viewModel.renameActive(it) }

                    1 -> confirmDeleteListing()
                }
            }
            .show()
    }

    private fun confirmDeleteListing() {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.delete_listing_confirm)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteActive { shownListingId = null }
            }
            .show()
    }

    private fun textPrompt(title: String, initial: String, onSave: (String) -> Unit) {
        val dialogBinding = DialogSingleFieldBinding.inflate(layoutInflater)
        dialogBinding.wrapper.hint = getString(R.string.listing_name)
        dialogBinding.value.setText(initial)
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                onSave(dialogBinding.value.text?.toString().orEmpty().trim())
            }
            .show()
    }

    // -------------------------------------------------------------- platforms

    private fun bindPlatformChips(platforms: List<PlatformProfile>) {
        binding.platformChips.removeAllViews()
        platforms.forEach { platform ->
            val chip = Chip(requireContext()).apply {
                text = platform.name
                isCheckable = true
                tag = platform.id
                setOnClickListener { viewModel.selectPlatform(platform.id) }
            }
            binding.platformChips.addView(chip)
        }
        checkChipFor(viewModel.selectedPlatform.value)
        binding.editPlatform.setOnClickListener {
            viewModel.selectedPlatform.value?.let(::editPlatform)
        }
    }

    private fun checkChipFor(platform: PlatformProfile?) {
        val id = platform?.id ?: return
        for (index in 0 until binding.platformChips.childCount) {
            val chip = binding.platformChips.getChildAt(index) as? Chip ?: continue
            chip.isChecked = chip.tag == id
        }
    }

    private fun editPlatform(platform: PlatformProfile) {
        val dialogBinding = DialogPlatformBinding.inflate(layoutInflater)
        val registers = com.drafts.compose.data.entity.Register.entries
        dialogBinding.name.setText(platform.name)
        dialogBinding.headlineLimit.setText(platform.headlineCharLimit.toString())
        dialogBinding.bodyLimit.setText(platform.bodyCharLimit.toString())
        dialogBinding.register.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            registers.map { it.name }
        )
        dialogBinding.register.setSelection(registers.indexOf(platform.register))

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.edit_platform)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                viewModel.savePlatform(
                    platform.copy(
                        name = dialogBinding.name.text?.toString()?.trim().orEmpty()
                            .ifBlank { platform.name },
                        headlineCharLimit = dialogBinding.headlineLimit.text?.toString()
                            ?.toIntOrNull() ?: platform.headlineCharLimit,
                        bodyCharLimit = dialogBinding.bodyLimit.text?.toString()
                            ?.toIntOrNull() ?: platform.bodyCharLimit,
                        register = registers.getOrElse(dialogBinding.register.selectedItemPosition) {
                            platform.register
                        }
                    )
                )
            }
            .show()
    }

    // -------------------------------------------------------------- rendering

    private fun bindRendering(rendering: Rendering?) {
        if (rendering == null) {
            binding.renderedHeadline.text = ""
            binding.renderedBody.text = ""
            binding.headlineCount.text = ""
            binding.bodyCount.text = ""
            return
        }

        binding.renderedHeadline.text = rendering.headline
        binding.renderedBody.text = rendering.body

        bindCount(
            view = binding.headlineCount,
            prefix = "H",
            count = rendering.headlineCount,
            limit = rendering.headlineCharLimit,
            overBy = rendering.headlineOverBy
        )
        bindCount(
            view = binding.bodyCount,
            prefix = "B",
            count = rendering.bodyCount,
            limit = rendering.bodyCharLimit,
            overBy = rendering.bodyOverBy
        )
    }

    private fun bindCount(
        view: android.widget.TextView,
        prefix: String,
        count: Int,
        limit: Int,
        overBy: Int
    ) {
        val body = if (limit > 0) {
            getString(R.string.count_of_limit, count, limit)
        } else {
            getString(R.string.count_no_limit, count)
        }
        val suffix = if (overBy > 0) "  " + getString(R.string.over_by, overBy) else ""
        view.text = "$prefix $body$suffix"
        view.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (overBy > 0) R.color.block else R.color.on_surface_muted
            )
        )
    }

    // ------------------------------------------------------------------- copy

    private fun wireCopyButtons() {
        binding.copyHeadline.setOnClickListener {
            copy(getString(R.string.section_headline)) { it.headline }
        }
        binding.copyBody.setOnClickListener {
            copy(getString(R.string.section_body)) { it.body }
        }
        binding.copyFull.setOnClickListener {
            copy(getString(R.string.copy_all)) { it.full }
        }
    }

    private fun copy(label: String, select: (Rendering) -> String) {
        val rendering = viewModel.rendering.value ?: return
        Clip.copy(requireContext(), label, select(rendering))
    }

    // ------------------------------------------------------------------- jump

    private fun applyJump(target: JumpTarget) {
        val editor = editorFor(target.field)
        val length = editor.text?.length ?: 0
        editor.requestFocus()
        if (!target.range.isEmpty()) {
            val start = target.range.first.coerceIn(0, length)
            val end = (target.range.last + 1).coerceIn(start, length)
            editor.setSelection(start, end)
        } else {
            editor.setSelection(length)
        }
        binding.scroll.post { binding.scroll.smoothScrollTo(0, topWithinScroll(editor)) }
        viewModel.consumeJump()
    }

    private fun topWithinScroll(view: View): Int {
        var top = 0
        var current: View? = view
        while (current != null && current != binding.scroll) {
            top += current.top
            current = current.parent as? View
        }
        return (top - 48).coerceAtLeast(0)
    }
}
