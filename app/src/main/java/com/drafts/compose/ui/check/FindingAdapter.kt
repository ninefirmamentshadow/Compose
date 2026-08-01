package com.drafts.compose.ui.check

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.drafts.compose.R
import com.drafts.compose.core.Finding
import com.drafts.compose.core.Severity
import com.drafts.compose.databinding.ItemFindingBinding

class FindingAdapter(
    private val onTap: (Finding) -> Unit
) : RecyclerView.Adapter<FindingAdapter.Holder>() {

    private var items: List<Finding> = emptyList()

    fun submit(findings: List<Finding>) {
        items = findings
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemFindingBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemFindingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val finding = items[position]
        val context = holder.binding.root.context
        val isBlock = finding.severity == Severity.BLOCK

        holder.binding.severity.apply {
            text = context.getString(if (isBlock) R.string.severity_block else R.string.severity_warn)
            setBackgroundResource(if (isBlock) R.drawable.bg_badge_block else R.drawable.bg_badge_warn)
            setTextColor(ContextCompat.getColor(context, if (isBlock) R.color.block else R.color.warn))
        }

        holder.binding.field.text = finding.field.label
        holder.binding.ruleId.text = finding.ruleId
        holder.binding.message.text = finding.message

        if (finding.excerpt.isBlank()) {
            holder.binding.excerpt.text = context.getString(R.string.whole_field)
            holder.binding.excerpt.setBackgroundResource(0)
        } else {
            holder.binding.excerpt.text = finding.excerpt
            holder.binding.excerpt.setBackgroundResource(R.drawable.bg_excerpt)
        }

        holder.binding.root.setOnClickListener { onTap(finding) }
    }
}
