package com.drafts.compose.ui.scripts

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.drafts.compose.R
import com.drafts.compose.data.entity.Script
import com.drafts.compose.databinding.DialogSingleFieldBinding
import com.drafts.compose.databinding.FragmentScriptsBinding
import com.drafts.compose.databinding.ItemScriptBinding
import com.drafts.compose.ui.MainViewModel
import com.drafts.compose.ui.common.Clip

/** Six labelled replies. Tap to copy, edit to change the wording. Nothing else. */
class ScriptsFragment : Fragment() {

    private var _binding: FragmentScriptsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var adapter: ScriptAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScriptsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ScriptAdapter(
            onCopy = { script -> Clip.copy(requireContext(), script.label.name, script.body) },
            onEdit = ::promptEdit
        )
        binding.scripts.adapter = adapter
        viewModel.scripts.observe(viewLifecycleOwner) { adapter.submit(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun promptEdit(script: Script) {
        val dialogBinding = DialogSingleFieldBinding.inflate(layoutInflater)
        dialogBinding.wrapper.hint = getString(R.string.script_body)
        dialogBinding.value.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        dialogBinding.value.minLines = 4
        dialogBinding.value.setText(script.body)

        AlertDialog.Builder(requireContext())
            .setTitle(script.label.name)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                viewModel.saveScript(
                    script.copy(body = dialogBinding.value.text?.toString().orEmpty())
                )
            }
            .show()
    }
}

class ScriptAdapter(
    private val onCopy: (Script) -> Unit,
    private val onEdit: (Script) -> Unit
) : RecyclerView.Adapter<ScriptAdapter.Holder>() {

    private var items: List<Script> = emptyList()

    fun submit(scripts: List<Script>) {
        items = scripts
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemScriptBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemScriptBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val script = items[position]
        holder.binding.label.text = script.label.name.replace('_', ' ')
        holder.binding.body.text = script.body
        holder.binding.copy.setOnClickListener { onCopy(script) }
        holder.binding.edit.setOnClickListener { onEdit(script) }
    }
}
