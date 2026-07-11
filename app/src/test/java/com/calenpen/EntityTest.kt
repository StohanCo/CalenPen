package com.calenpen

import com.calenpen.data.database.entities.*
import org.junit.Assert.*
import org.junit.Test

class EntityTest {

    @Test
    fun `Note has sensible defaults`() {
        val note = Note()
        assertEquals(0L, note.id)
        assertEquals("", note.title)
        assertNull(note.dateKey)
        assertEquals(TemplateType.BLANK, note.templateType)
        assertNull(note.strokesJson)
        assertEquals("", note.typedText)
        assertEquals("", note.recognisedText)
        assertNull(note.pdfUri)
        assertEquals(0, note.pdfPage)
        assertEquals(PaperStyle.BLANK, note.paperStyle)
        assertFalse(note.isPinned)
        assertEquals("", note.tags)
    }

    @Test
    fun `Note copy changes only the specified fields`() {
        val base = Note(id = 1, title = "Original", dateKey = "2024-03-01")
        val updated = base.copy(title = "Updated", updatedAt = 9999L)
        assertEquals(1L, updated.id)
        assertEquals("Updated", updated.title)
        assertEquals("2024-03-01", updated.dateKey)
        assertEquals(9999L, updated.updatedAt)
    }

    @Test
    fun `CalendarEntry has sensible defaults`() {
        val entry = CalendarEntry(dateKey = "2024-01-01")
        assertEquals("2024-01-01", entry.dateKey)
        assertEquals(0, entry.noteCount)
        assertEquals("", entry.previewText)
        assertNull(entry.colourLabel)
        assertFalse(entry.isHighlighted)
        assertNull(entry.emoji)
    }

    @Test
    fun `Template built-in flag defaults to false`() {
        val template = Template(name = "My template")
        assertFalse(template.isBuiltIn)
    }

    @Test
    fun `TemplateType constants are unique strings`() {
        val types = listOf(
            TemplateType.BLANK, TemplateType.DAILY, TemplateType.WEEKLY,
            TemplateType.MONTHLY, TemplateType.HABIT_TRACKER, TemplateType.MOOD_JOURNAL,
            TemplateType.BULLET_JOURNAL, TemplateType.MEETING_NOTES,
            TemplateType.TRAVEL_LOG, TemplateType.BOOK_NOTES
        )
        assertEquals(types.size, types.toSet().size)
    }

    @Test
    fun `PaperStyle constants are unique strings`() {
        val styles = listOf(
            PaperStyle.BLANK, PaperStyle.LINED, PaperStyle.DOTTED,
            PaperStyle.GRID, PaperStyle.CORNELL
        )
        assertEquals(styles.size, styles.toSet().size)
    }
}
