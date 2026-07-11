package com.calenpen.data.repository

import com.calenpen.data.database.dao.CalendarEntryDao
import com.calenpen.data.database.dao.NoteDao
import com.calenpen.data.database.dao.TemplateDao
import com.calenpen.data.database.entities.CalendarEntry
import com.calenpen.data.database.entities.Note
import com.calenpen.data.database.entities.Template
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for all CalenPen data.
 *
 * All UI-layer classes interact with the data through this repository so that
 * the data sources (Room DAOs) can be swapped or extended without touching the UI.
 */
class NoteRepository(
    private val noteDao: NoteDao,
    private val calendarEntryDao: CalendarEntryDao,
    private val templateDao: TemplateDao
) {

    // ─── Notes ────────────────────────────────────────────────────────────────

    fun observeAllNotes(): Flow<List<Note>> = noteDao.observeAllNotes()

    fun observeUndatedNotes(): Flow<List<Note>> = noteDao.observeUndatedNotes()

    fun observePinnedNotes(): Flow<List<Note>> = noteDao.observePinnedNotes()

    fun observeNoteById(id: Long): Flow<Note?> = noteDao.observeNoteById(id)

    suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)

    fun observeNotesForDate(dateKey: String): Flow<List<Note>> =
        noteDao.observeNotesForDate(dateKey)

    fun observeNotesForMonth(yearMonth: String): Flow<List<Note>> =
        noteDao.observeNotesForMonth(yearMonth)

    /**
     * Saves (insert or update) a note and refreshes the [CalendarEntry] for its date.
     *
     * @return the row id of the saved note.
     */
    suspend fun saveNote(note: Note): Long {
        val id = if (note.id == 0L) {
            noteDao.insertNote(note)
        } else {
            noteDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
            note.id
        }
        note.dateKey?.let { refreshCalendarEntry(it) }
        return id
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
        note.dateKey?.let { refreshCalendarEntry(it) }
    }

    suspend fun updateRecognisedText(noteId: Long, text: String) {
        noteDao.updateRecognisedText(noteId, text)
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    fun searchNotesByDateRange(startDate: String, endDate: String): Flow<List<Note>> =
        noteDao.searchNotesByDateRange(startDate, endDate)

    // ─── Calendar Entries ─────────────────────────────────────────────────────

    fun observeCalendarEntriesForMonth(yearMonth: String): Flow<List<CalendarEntry>> =
        calendarEntryDao.observeEntriesForMonth(yearMonth)

    fun observeCalendarEntry(dateKey: String): Flow<CalendarEntry?> =
        calendarEntryDao.observeEntry(dateKey)

    suspend fun getCalendarEntry(dateKey: String): CalendarEntry? =
        calendarEntryDao.getEntry(dateKey)

    suspend fun saveCalendarEntry(entry: CalendarEntry) =
        calendarEntryDao.insertOrReplace(entry)

    /**
     * Re-computes the note count and preview text for a given date and persists it.
     */
    suspend fun refreshCalendarEntry(dateKey: String) {
        val count = noteDao.countNotesForDate(dateKey)
        val preview = noteDao.getFirstNoteTitleForDate(dateKey) ?: ""
        if (count > 0) {
            val existing = calendarEntryDao.getEntry(dateKey)
            calendarEntryDao.insertOrReplace(
                (existing ?: CalendarEntry(dateKey = dateKey)).copy(
                    noteCount = count,
                    previewText = preview,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            calendarEntryDao.getEntry(dateKey)?.let {
                calendarEntryDao.insertOrReplace(
                    it.copy(noteCount = 0, previewText = "", updatedAt = System.currentTimeMillis())
                )
            }
        }
    }

    // ─── Templates ────────────────────────────────────────────────────────────

    fun observeAllTemplates(): Flow<List<Template>> = templateDao.observeAllTemplates()

    fun observeBuiltInTemplates(): Flow<List<Template>> = templateDao.observeBuiltInTemplates()

    fun observeCustomTemplates(): Flow<List<Template>> = templateDao.observeCustomTemplates()

    suspend fun getTemplateById(id: Long): Template? = templateDao.getTemplateById(id)

    suspend fun saveTemplate(template: Template): Long = templateDao.insertTemplate(template)

    suspend fun deleteTemplate(template: Template) = templateDao.deleteTemplate(template)
}
