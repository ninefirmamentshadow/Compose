package com.drafts.compose.ui.tests

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.drafts.compose.R
import com.drafts.compose.core.tests.TestGuard
import com.drafts.compose.data.entity.HeadlineTest
import com.drafts.compose.data.entity.PlatformProfile
import com.drafts.compose.databinding.DialogNewTestBinding
import com.drafts.compose.databinding.DialogSingleFieldBinding
import com.drafts.compose.databinding.FragmentTestsBinding
import com.drafts.compose.ui.MainViewModel

/**
 * The headline kill-file: what ran, where, for how long, and how many inquiries
 * came in. Starting a test that moves more than one segment is guarded.
 */
class TestsFragment : Fragment() {

    private var _binding: FragmentTestsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var adapter: TestAdapter
    private var sortByInquiries = false
    private var platforms: List<PlatformProfile> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = TestAdapter(
            onSetCount = ::promptInquiryCount,
            onEnd = ::endTest,
            onDelete = ::confirmDelete
        )
        binding.tests.adapter = adapter

        viewModel.testsByDate.observe(viewLifecycleOwner) { if (!sortByInquiries) render(it) }
        viewModel.testsByInquiryCount.observe(viewLifecycleOwner) { if (sortByInquiries) render(it) }
        viewModel.platforms.observe(viewLifecycleOwner) { platforms = it }

        binding.sortToggle.setOnClickListener {
            sortByInquiries = !sortByInquiries
            binding.sortToggle.setText(
                if (sortByInquiries) R.string.sort_by_inquiries else R.string.sort_by_date
            )
            val current = if (sortByInquiries) {
                viewModel.testsByInquiryCount.value
            } else {
                viewModel.testsByDate.value
            }
            render(current.orEmpty())
        }

        binding.addTest.setOnClickListener { promptNewTest() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun render(tests: List<HeadlineTest>) {
        adapter.submit(tests)
        binding.empty.visibility = if (tests.isEmpty()) View.VISIBLE else View.GONE
    }

    // ------------------------------------------------------------- new test

    private fun promptNewTest() {
        if (platforms.isEmpty()) return
        val draft = viewModel.draft.value
        val dialogBinding = DialogNewTestBinding.inflate(layoutInflater)
        dialogBinding.platform.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            platforms.map { it.name }
        )
        viewModel.selectedPlatform.value?.let { selected ->
            val index = platforms.indexOfFirst { it.id == selected.id }
            if (index >= 0) dialogBinding.platform.setSelection(index)
        }
        dialogBinding.headlineName.setText(draft?.headlineName.orEmpty())
        dialogBinding.headlineCategory.setText(draft?.headlineCategory.orEmpty())
        dialogBinding.headlineFilter.setText(draft?.headlineFilter.orEmpty())

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.new_test)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                val platform = platforms.getOrNull(dialogBinding.platform.selectedItemPosition)
                    ?: return@setPositiveButton
                val segments = TestGuard.Segments(
                    headlineName = dialogBinding.headlineName.text?.toString().orEmpty(),
                    headlineCategory = dialogBinding.headlineCategory.text?.toString().orEmpty(),
                    headlineFilter = dialogBinding.headlineFilter.text?.toString().orEmpty()
                )
                guardThenStart(platform.name, segments)
            }
            .show()
    }

    /**
     * The one-variable rule. If more than one segment moved against the last test
     * on this platform, say so and make the operator choose.
     */
    private fun guardThenStart(platform: String, segments: TestGuard.Segments) {
        viewModel.evaluateGuard(platform, segments) { decision ->
            if (!isAdded) return@evaluateGuard
            if (decision.requiresConfirmation) {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.guard_title)
                    .setMessage(
                        TestGuard.MESSAGE + "\n\n" +
                            getString(R.string.guard_detail, platform, decision.changedLabels)
                    )
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.guard_continue) { _, _ -> start(platform, segments) }
                    .show()
            } else {
                start(platform, segments)
            }
        }
    }

    private fun start(platform: String, segments: TestGuard.Segments) {
        viewModel.startTest(
            HeadlineTest(
                headlineName = segments.headlineName.trim(),
                headlineCategory = segments.headlineCategory.trim(),
                headlineFilter = segments.headlineFilter.trim(),
                platform = platform,
                dateStarted = System.currentTimeMillis(),
                dateEnded = null,
                inquiryCount = 0
            )
        )
    }

    // -------------------------------------------------------- existing tests

    private fun promptInquiryCount(test: HeadlineTest) {
        val dialogBinding = DialogSingleFieldBinding.inflate(layoutInflater)
        dialogBinding.wrapper.hint = getString(R.string.inquiry_count)
        dialogBinding.value.inputType = InputType.TYPE_CLASS_NUMBER
        dialogBinding.value.setText(test.inquiryCount.toString())

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.inquiry_count)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                val count = dialogBinding.value.text?.toString()?.toIntOrNull() ?: test.inquiryCount
                viewModel.updateTest(test.copy(inquiryCount = count.coerceAtLeast(0)))
            }
            .show()
    }

    private fun endTest(test: HeadlineTest) {
        if (test.dateEnded != null) return
        viewModel.updateTest(test.copy(dateEnded = System.currentTimeMillis()))
    }

    private fun confirmDelete(test: HeadlineTest) {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.delete_test_confirm)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteTest(test) }
            .show()
    }
}
