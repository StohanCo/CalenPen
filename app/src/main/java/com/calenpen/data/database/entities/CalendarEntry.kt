package com.calenpen.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Lightweight summary of each calendar day.
 *
 * A [CalendarEntry] is created / updated whenever a [Note] is assigned to or removed from
 * a date.  It caches the number of associated notes and a preview snippet so the calendar
 * grid can be rendered without querying all notes.
 */
@Entity(tableName = "calendar_entries")
data class CalendarEntry(
    /** Calendar date in "YYYY-MM-DD" format – also serves as the primary key. */
    @PrimaryKey
    val dateKey: String,

    /** Total number of notes linked to this date. */
    val noteCount: Int = 0,

    /** Short preview of the first note on this day (used in calendar cell tooltips). */
    val previewText: String = "",

    /** Colour label for quick visual categorisation (hex string, e.g. "#FF6B6B"). */
    val colourLabel: String? = null,

    /** Whether the user has marked this date as special (birthday, holiday, etc.). */
    val isHighlighted: Boolean = false,

    /** Optional emoji or icon code associated with the day. */
    val emoji: String? = null,

    /** Last modified timestamp (epoch millis). */
    val updatedAt: Long = System.currentTimeMillis()
)
