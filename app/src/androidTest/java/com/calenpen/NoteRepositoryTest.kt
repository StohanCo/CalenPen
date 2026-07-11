package com.calenpen

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.calenpen.data.database.AppDatabase
import com.calenpen.data.database.entities.CalendarEntry
import com.calenpen.data.database.entities.Note
import com.calenpen.data.database.entities.Template
import com.calenpen.data.repository.NoteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: NoteRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NoteRepository(db.noteDao(), db.calendarEntryDao(), db.templateDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndRetrieveNote() = runBlocking {
        val note = Note(title = "Test note", dateKey = "2024-03-01")
        val id = repository.saveNote(note)
        assertTrue(id > 0)

        val retrieved = repository.getNoteById(id)
        assertNotNull(retrieved)
        assertEquals("Test note", retrieved!!.title)
        assertEquals("2024-03-01", retrieved.dateKey)
    }

    @Test
    fun searchNotesByTitle() = runBlocking {
        repository.saveNote(Note(title = "Shopping list", dateKey = "2024-03-01"))
        repository.saveNote(Note(title = "Meeting notes", dateKey = "2024-03-02"))
        repository.saveNote(Note(title = "Shopping ideas"))

        val results = repository.searchNotes("Shopping").first()
        assertEquals(2, results.size)
        assertTrue(results.all { it.title.contains("Shopping") })
    }

    @Test
    fun searchNotesByDateKey() = runBlocking {
        repository.saveNote(Note(title = "Note 1", dateKey = "2024-03-15"))
        repository.saveNote(Note(title = "Note 2", dateKey = "2024-03-16"))
        repository.saveNote(Note(title = "Note 3", dateKey = "2024-04-01"))

        val results = repository.searchNotes("2024-03").first()
        assertEquals(2, results.size)
    }

    @Test
    fun deleteNoteRemovesFromDatabase() = runBlocking {
        val note = Note(title = "To delete")
        val id = repository.saveNote(note)
        val saved = repository.getNoteById(id)!!
        repository.deleteNote(saved)

        val deleted = repository.getNoteById(id)
        assertNull(deleted)
    }

    @Test
    fun calendarEntryIsRefreshedAfterSave() = runBlocking {
        val dateKey = "2024-06-10"
        repository.saveNote(Note(title = "Note A", dateKey = dateKey))
        repository.saveNote(Note(title = "Note B", dateKey = dateKey))

        val entry = repository.getCalendarEntry(dateKey)
        assertNotNull(entry)
        assertEquals(2, entry!!.noteCount)
    }

    @Test
    fun updateRecognisedText() = runBlocking {
        val note = Note(title = "Handwritten")
        val id = repository.saveNote(note)
        repository.updateRecognisedText(id, "recognised handwriting text")

        val retrieved = repository.getNoteById(id)
        assertEquals("recognised handwriting text", retrieved!!.recognisedText)
    }

    @Test
    fun observeAllNotesReturnsFlow() = runBlocking {
        repository.saveNote(Note(title = "Flow note 1"))
        repository.saveNote(Note(title = "Flow note 2"))

        val notes = repository.observeAllNotes().first()
        assertTrue(notes.size >= 2)
    }

    @Test
    fun saveTemplateAndRetrieve() = runBlocking {
        val template = Template(name = "Custom template", type = "custom", isBuiltIn = false)
        val id = repository.saveTemplate(template)
        assertTrue(id > 0)

        val retrieved = repository.getTemplateById(id)
        assertNotNull(retrieved)
        assertEquals("Custom template", retrieved!!.name)
    }
}
