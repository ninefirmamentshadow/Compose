package com.drafts.compose.ui.check

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.drafts.compose.R
import com.drafts.compose.data.entity.CanonicalValues
import com.drafts.compose.databinding.DialogCanonicalBinding
import com.drafts.compose.databinding.FragmentCheckBinding
import com.drafts.compose.ui.MainViewModel

/**
 * Both passes over the current draft, re-run on every keystroke. Tapping a finding
 * jumps to the field and selects the offending text.
 */
class CheckFragment : Fragment() {

    private var _binding: FragmentCheckBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var adapter: FindingAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = FindingAdapter { finding -> viewModel.requestJump(finding) }
        binding.findings.adapter = adapter

        viewModel.report.observe(viewLifecycleOwner) { report ->
            adapter.submit(report.findings)
            binding.summary.text = getString(R.string.check_summary, report.blocks, report.warns)
            binding.empty.visibility = if (report.isClean) View.VISIBLE else View.GONE
        }

        binding.openCanonical.setOnClickListener { editCanonical() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun editCanonical() {
        val current = viewModel.canonical.value ?: CanonicalValues()
        val dialogBinding = DialogCanonicalBinding.inflate(layoutInflater)
        dialogBinding.rateQv.setText(current.rateQv.takeIf { it > 0 }?.toString().orEmpty())
        dialogBinding.rateHh.setText(current.rateHh.takeIf { it > 0 }?.toString().orEmpty())
        dialogBinding.rateHour.setText(current.rateHour.takeIf { it > 0 }?.toString().orEmpty())
        dialogBinding.bioDescriptor.setText(current.bioDescriptor)
        dialogBinding.contactHandle.setText(current.contactHandle)
        dialogBinding.contactInstruction.setText(current.contactInstruction)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.canonical_values)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                viewModel.saveCanonical(
                    current.copy(
                        rateQv = dialogBinding.rateQv.text?.toString()?.toIntOrNull() ?: 0,
                        rateHh = dialogBinding.rateHh.text?.toString()?.toIntOrNull() ?: 0,
                        rateHour = dialogBinding.rateHour.text?.toString()?.toIntOrNull() ?: 0,
                        bioDescriptor = dialogBinding.bioDescriptor.text?.toString().orEmpty(),
                        contactHandle = dialogBinding.contactHandle.text?.toString().orEmpty(),
                        contactInstruction = dialogBinding.contactInstruction.text?.toString().orEmpty()
                    )
                )
            }
            .show()
    }
}
