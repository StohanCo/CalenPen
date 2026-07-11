package com.calenpen.ui.notes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.calenpen.R
import com.calenpen.data.database.entities.Note
import com.calenpen.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Generic note-list adapter used on the calendar day detail, notes list screen, and search results.
 */
class NotesAdapter(
    private val onNoteClick: (Note) -> Unit,
    private val onNoteDelete: ((Note) -> Unit)? = null
) : ListAdapter<Note, NotesAdapter.NoteViewHolder>(NoteDiffCallback) {

    inner class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_note_title)
        val tvDate: TextView = view.findViewById(R.id.tv_note_date)
        val tvPreview: TextView = view.findViewById(R.id.tv_note_preview)
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete_note)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        holder.tvTitle.text = note.title.ifBlank {
            holder.itemView.context.getString(R.string.untitled_note)
        }
        holder.tvDate.text = note.dateKey?.let { DateUtils.toShortLabel(it) }
            ?: holder.itemView.context.getString(R.string.no_date)

        val preview = when {
            note.typedText.isNotBlank() -> note.typedText
            note.recognisedText.isNotBlank() -> note.recognisedText
            else -> ""
        }
        holder.tvPreview.text = preview.take(120)
        holder.tvPreview.visibility = if (preview.isBlank()) View.GONE else View.VISIBLE

        holder.itemView.setOnClickListener { onNoteClick(note) }

        if (onNoteDelete != null) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener { onNoteDelete.invoke(note) }
        } else {
            holder.btnDelete.visibility = View.GONE
        }
    }

    private object NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Note, newItem: Note) = oldItem == newItem
    }
}
