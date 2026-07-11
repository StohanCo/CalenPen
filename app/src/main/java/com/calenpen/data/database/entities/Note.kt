package com.calenpen.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.calenpen.data.database.Converters

/**
 * Represents a single note / diary page.
 *
 * A note can be:
 *  - A free-form handwritten page (strokes stored as JSON in [strokesJson])
 *  - A typed text page ([typedText])
 *  - A PDF-imported page (reference in [pdfUri])
 *  - Any combination of the above layered together
 *
 * A note is linked to a calendar date via [dateKey] (format "YYYY-MM-DD").
 * If [dateKey] is null the note is a standalone, undated page.
 */
@Entity(tableName = "notes")
@TypeConverters(Converters::class)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Human-readable title shown in lists and search results. */
    val title: String = "",

    /** Calendar date this note belongs to, in "YYYY-MM-DD" format. Nullable for undated notes. */
    val dateKey: String? = null,

    /** Template type used when creating the note. See [TemplateType]. */
    val templateType: String = TemplateType.BLANK,

    /**
     * Serialised list of [com.calenpen.ui.editor.Stroke] objects (JSON).
     * Null / empty means the note has no handwritten content yet.
     */
    val strokesJson: String? = null,

    /** Plain-text content for typed notes. */
    val typedText: String = "",

    /**
     * OCR-recognised text derived from [strokesJson].
     * Updated asynchronously by [com.calenpen.utils.HandwritingRecognizer].
     */
    val recognisedText: String = "",

    /** URI string pointing to an imported PDF file (content:// or file://). */
    val pdfUri: String? = null,

    /** Page number inside a multi-page PDF (0-based). */
    val pdfPage: Int = 0,

    /** Background paper style (e.g. "blank", "lined", "dotted", "grid"). */
    val paperStyle: String = PaperStyle.BLANK,

    /** Creation timestamp (epoch millis). */
    val createdAt: Long = System.currentTimeMillis(),

    /** Last modification timestamp (epoch millis). */
    val updatedAt: Long = System.currentTimeMillis(),

    /** Whether the note is pinned / favourite. */
    val isPinned: Boolean = false,

    /** Tags associated with this note, stored as comma-separated values. */
    val tags: String = ""
)

/** Pre-defined template types for new notes. */
object TemplateType {
    const val BLANK = "blank"
    const val DAILY = "daily"
    const val WEEKLY = "weekly"
    const val MONTHLY = "monthly"
    const val HABIT_TRACKER = "habit_tracker"
    const val MOOD_JOURNAL = "mood_journal"
    const val BULLET_JOURNAL = "bullet_journal"
    const val MEETING_NOTES = "meeting_notes"
    const val TRAVEL_LOG = "travel_log"
    const val BOOK_NOTES = "book_notes"
}

/** Paper background styles. */
object PaperStyle {
    const val BLANK = "blank"
    const val LINED = "lined"
    const val DOTTED = "dotted"
    const val GRID = "grid"
    const val CORNELL = "cornell"
}
