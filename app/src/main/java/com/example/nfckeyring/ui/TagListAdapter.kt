package com.example.nfckeyring.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.nfckeyring.R
import com.example.nfckeyring.data.TagEntity

class TagListAdapter(
    private val onRename: (TagEntity) -> Unit,
    private val onEdit: (TagEntity) -> Unit,
    private val onDelete: (TagEntity) -> Unit
) : RecyclerView.Adapter<TagListAdapter.TagViewHolder>() {

    private var tags: List<TagEntity> = emptyList()

    fun submitList(list: List<TagEntity>) {
        tags = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.tag_item, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tag = tags[position]
        holder.label.text = tag.label
        holder.renameButton.setOnClickListener { onRename(tag) }
        holder.editButton.setOnClickListener { onEdit(tag) }
        holder.deleteButton.setOnClickListener { onDelete(tag) }
    }

    override fun getItemCount(): Int = tags.size

    inner class TagViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val label: TextView = view.findViewById(R.id.tagLabel)
        val renameButton: Button = view.findViewById(R.id.renameButton)
        val editButton: Button = view.findViewById(R.id.editButton)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
    }
}
