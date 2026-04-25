package com.example.lifelogger.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lifelogger.data.model.LogEntry
import com.example.lifelogger.databinding.ItemLogEntryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MEMBER 2 RESPONSIBILITY: UI Layer - RecyclerView Adapter
 *
 * Adapter displays a list of LogEntries in a RecyclerView
 * RecyclerView efficiently displays large lists by recycling views
 *
 * ListAdapter + DiffUtil automatically:
 * - Detects what changed
 * - Animates only changed items
 * - Improves performance
 *
 * onItemClick: callback when user taps an entry
 */
class LogEntryAdapter(
    private val onItemClick: (LogEntry) -> Unit
) : ListAdapter<LogEntry, LogEntryAdapter.EntryViewHolder>(DiffCallback()) {

    /**
     * ViewHolder holds references to UI views for each list item
     */
    class EntryViewHolder(
        private val binding: ItemLogEntryBinding,
        private val onItemClick: (LogEntry) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: LogEntry) {
            binding.apply {
                // Set title
                titleText.text = entry.title.ifEmpty { "Untitled" }

                // Set preview of content
                contentPreview.text = if (entry.content.length > 100) {
                    entry.content.substring(0, 100) + "..."
                } else {
                    entry.content
                }

                // Format and display timestamp
                val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                dateText.text = formatter.format(Date(entry.timestamp))

                // Show category
                categoryText.text = entry.category.uppercase()

                // Click listener for the entire item
                root.setOnClickListener { onItemClick(entry) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val binding = ItemLogEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EntryViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * DiffUtil compares old and new lists to detect changes
     * Only refreshes items that changed (more efficient)
     */
    class DiffCallback : DiffUtil.ItemCallback<LogEntry>() {
        override fun areItemsTheSame(oldItem: LogEntry, newItem: LogEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LogEntry, newItem: LogEntry): Boolean {
            return oldItem == newItem
        }
    }
}

