package com.drafts.compose.ui.tests

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.drafts.compose.R
import com.drafts.compose.core.render.Renderer
import com.drafts.compose.data.entity.HeadlineTest
import com.drafts.compose.databinding.ItemTestBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TestAdapter(
    private val onSetCount: (HeadlineTest) -> Unit,
    private val onEnd: (HeadlineTest) -> Unit,
    private val onDelete: (HeadlineTest) -> Unit
) : RecyclerView.Adapter<TestAdapter.Holder>() {

    private var items: List<HeadlineTest> = emptyList()
    private val dates = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun submit(tests: List<HeadlineTest>) {
        items = tests
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemTestBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemTestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val test = items[position]
        val context = holder.binding.root.context

        holder.binding.headline.text = listOf(
            test.headlineName,
            test.headlineCategory,
            test.headlineFilter
        ).filter { it.isNotBlank() }.joinToString(Renderer.SEGMENT_SEPARATOR)

        val ended = test.dateEnded?.let { dates.format(Date(it)) } ?: context.getString(R.string.running)
        holder.binding.meta.text = "${test.platform} · ${dates.format(Date(test.dateStarted))} → $ended"
        holder.binding.inquiries.text = test.inquiryCount.toString()

        holder.binding.setCount.setOnClickListener { onSetCount(test) }
        holder.binding.endTest.setOnClickListener { onEnd(test) }
        holder.binding.endTest.isEnabled = test.dateEnded == null
        holder.binding.deleteTest.setOnClickListener { onDelete(test) }
    }
}
