package com.calenpen.ui.calendar

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.calenpen.R
import com.calenpen.data.database.entities.CalendarEntry
import com.calenpen.utils.DateUtils

/**
 * Populates a 7-column calendar grid for a given month.
 *
 * Each cell represents a day.  Empty cells (before the 1st and after the last day) are
 * shown blank so the grid stays aligned to the correct day-of-week column.
 */
class CalendarAdapter(
    private val onDayClick: (dateKey: String) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    /** Data model for a single calendar cell. */
    data class CalendarCell(
        val dateKey: String?,       // null for blank leading / trailing cells
        val dayNumber: Int,         // 1-31, 0 for blank
        val isToday: Boolean = false,
        val isSelected: Boolean = false,
        val hasNotes: Boolean = false,
        val noteCount: Int = 0,
        val previewText: String = "",
        val colourLabel: String? = null,
        val isHighlighted: Boolean = false,
        val emoji: String? = null
    )

    private val cells = mutableListOf<CalendarCell>()

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Rebuilds the cell list for [monthKey] (format "YYYY-MM") using the provided
     * calendar entry data.
     */
    fun submitMonth(
        monthKey: String,
        entries: Map<String, CalendarEntry>,
        selectedDateKey: String?
    ) {
        cells.clear()
        val todayKey = DateUtils.todayKey()
        val daysInMonth = DateUtils.daysInMonth(monthKey)
        val firstDow = DateUtils.firstDayOfWeekInMonth(monthKey) // 1=Sun … 7=Sat

        // Leading blank cells
        val leadingBlanks = firstDow - 1
        repeat(leadingBlanks) { cells.add(CalendarCell(dateKey = null, dayNumber = 0)) }

        // Day cells
        val parts = monthKey.split("-")
        for (day in 1..daysInMonth) {
            val dateKey = DateUtils.buildDateKey(parts[0].toInt(), parts[1].toInt(), day)
            val entry = entries[dateKey]
            cells.add(
                CalendarCell(
                    dateKey = dateKey,
                    dayNumber = day,
                    isToday = dateKey == todayKey,
                    isSelected = dateKey == selectedDateKey,
                    hasNotes = (entry?.noteCount ?: 0) > 0,
                    noteCount = entry?.noteCount ?: 0,
                    previewText = entry?.previewText ?: "",
                    colourLabel = entry?.colourLabel,
                    isHighlighted = entry?.isHighlighted ?: false,
                    emoji = entry?.emoji
                )
            )
        }

        notifyDataSetChanged()
    }

    // ─── RecyclerView overrides ───────────────────────────────────────────────

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(cells[position])
    }

    override fun getItemCount(): Int = cells.size

    // ─── ViewHolder ───────────────────────────────────────────────────────────

    inner class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvDay: TextView = view.findViewById(R.id.tv_day_number)
        private val tvEmoji: TextView = view.findViewById(R.id.tv_day_emoji)
        private val dotIndicator: View = view.findViewById(R.id.view_note_dot)
        private val tvNoteCount: TextView = view.findViewById(R.id.tv_note_count)

        fun bind(cell: CalendarCell) {
            if (cell.dateKey == null) {
                itemView.isClickable = false
                tvDay.text = ""
                tvEmoji.isVisible = false
                dotIndicator.isVisible = false
                tvNoteCount.isVisible = false
                itemView.setBackgroundColor(Color.TRANSPARENT)
                return
            }

            itemView.isClickable = true
            tvDay.text = cell.dayNumber.toString()

            // Today indicator
            if (cell.isToday) {
                tvDay.setBackgroundResource(R.drawable.bg_today_circle)
                tvDay.setTextColor(Color.WHITE)
            } else {
                tvDay.setBackgroundColor(Color.TRANSPARENT)
                tvDay.setTextColor(
                    if (cell.isSelected) Color.parseColor("#1565C0")
                    else Color.BLACK
                )
            }

            itemView.setBackgroundResource(
                when {
                    cell.isHighlighted -> R.drawable.bg_highlighted_day
                    cell.isSelected -> R.drawable.bg_selected_day
                    else -> 0
                }
            )

            // Note dot / count
            dotIndicator.isVisible = cell.hasNotes
            tvNoteCount.isVisible = cell.noteCount > 1
            tvNoteCount.text = cell.noteCount.toString()

            // Emoji
            tvEmoji.isVisible = cell.emoji != null
            tvEmoji.text = cell.emoji ?: ""

            itemView.setOnClickListener {
                cell.dateKey?.let { key -> onDayClick(key) }
            }
        }
    }
}
