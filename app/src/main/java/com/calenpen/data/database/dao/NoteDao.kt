package com.calenpen.data.database.dao

import androidx.room.*
import com.calenpen.data.database.entities.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    // ─── Insert / Update / Delete ────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    // ─── Single note queries ──────────────────────────────────────────────────

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeNoteById(id: Long): Flow<Note?>

    // ─── Date-based queries ───────────────────────────────────────────────────

    @Query("SELECT * FROM notes WHERE dateKey = :dateKey ORDER BY updatedAt DESC")
    fun observeNotesForDate(dateKey: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE dateKey = :dateKey ORDER BY updatedAt DESC")
    suspend fun getNotesForDate(dateKey: String): List<Note>

    @Query("SELECT * FROM notes WHERE dateKey LIKE :monthPrefix || '%' ORDER BY dateKey ASC, updatedAt DESC")
    fun observeNotesForMonth(monthPrefix: String): Flow<List<Note>>

    // ─── All notes ────────────────────────────────────────────────────────────

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE dateKey IS NULL ORDER BY updatedAt DESC")
    fun observeUndatedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isPinned = 1 ORDER BY updatedAt DESC")
    fun observePinnedNotes(): Flow<List<Note>>

    // ─── Search ───────────────────────────────────────────────────────────────

    /**
     * Full-text search across title, typed text, and OCR-recognised handwriting.
     * Also matches date keys so users can search "2024-01" to find all Jan 2024 notes.
     */
    @Query("""
        SELECT * FROM notes
        WHERE title LIKE '%' || :query || '%'
           OR typedText LIKE '%' || :query || '%'
           OR recognisedText LIKE '%' || :query || '%'
           OR dateKey LIKE '%' || :query || '%'
           OR tags LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
    """)
    fun searchNotes(query: String): Flow<List<Note>>

    @Query("""
        SELECT * FROM notes
        WHERE dateKey BETWEEN :startDate AND :endDate
        ORDER BY dateKey ASC, updatedAt DESC
    """)
    fun searchNotesByDateRange(startDate: String, endDate: String): Flow<List<Note>>

    // ─── Stats helpers ────────────────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM notes WHERE dateKey = :dateKey")
    suspend fun countNotesForDate(dateKey: String): Int

    @Query("SELECT title FROM notes WHERE dateKey = :dateKey ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getFirstNoteTitleForDate(dateKey: String): String?

    // ─── OCR update ───────────────────────────────────────────────────────────

    @Query("UPDATE notes SET recognisedText = :text, updatedAt = :now WHERE id = :id")
    suspend fun updateRecognisedText(id: Long, text: String, now: Long = System.currentTimeMillis())
}
