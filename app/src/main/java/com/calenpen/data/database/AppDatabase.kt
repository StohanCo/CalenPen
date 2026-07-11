package com.calenpen.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.calenpen.data.database.dao.CalendarEntryDao
import com.calenpen.data.database.dao.NoteDao
import com.calenpen.data.database.dao.TemplateDao
import com.calenpen.data.database.entities.CalendarEntry
import com.calenpen.data.database.entities.Note
import com.calenpen.data.database.entities.Template
import com.calenpen.data.database.entities.TemplateType
import com.calenpen.data.database.entities.PaperStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Note::class, CalendarEntry::class, Template::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun calendarEntryDao(): CalendarEntryDao
    abstract fun templateDao(): TemplateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calenpen_database"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    /** Populates the database with built-in templates on first creation. */
    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateBuiltInTemplates(database.templateDao())
                }
            }
        }

        private suspend fun populateBuiltInTemplates(dao: TemplateDao) {
            val builtIns = listOf(
                Template(
                    name = "Blank",
                    description = "A completely blank page.",
                    type = TemplateType.BLANK,
                    paperStyle = PaperStyle.BLANK,
                    isBuiltIn = true
                ),
                Template(
                    name = "Daily Planner",
                    description = "Hourly schedule, top priorities, and notes section for one day.",
                    type = TemplateType.DAILY,
                    paperStyle = PaperStyle.LINED,
                    isBuiltIn = true
                ),
                Template(
                    name = "Weekly Overview",
                    description = "Seven columns for a full week at a glance.",
                    type = TemplateType.WEEKLY,
                    paperStyle = PaperStyle.GRID,
                    isBuiltIn = true
                ),
                Template(
                    name = "Monthly Calendar",
                    description = "Full month grid with space for notes per day.",
                    type = TemplateType.MONTHLY,
                    paperStyle = PaperStyle.GRID,
                    isBuiltIn = true
                ),
                Template(
                    name = "Habit Tracker",
                    description = "Track daily habits throughout the month.",
                    type = TemplateType.HABIT_TRACKER,
                    paperStyle = PaperStyle.DOTTED,
                    isBuiltIn = true
                ),
                Template(
                    name = "Mood Journal",
                    description = "Log your mood and reflections each day.",
                    type = TemplateType.MOOD_JOURNAL,
                    paperStyle = PaperStyle.LINED,
                    isBuiltIn = true
                ),
                Template(
                    name = "Bullet Journal",
                    description = "Rapid-logging style with tasks, events, and notes.",
                    type = TemplateType.BULLET_JOURNAL,
                    paperStyle = PaperStyle.DOTTED,
                    isBuiltIn = true
                ),
                Template(
                    name = "Meeting Notes",
                    description = "Agenda, attendees, action items, and free notes.",
                    type = TemplateType.MEETING_NOTES,
                    paperStyle = PaperStyle.CORNELL,
                    isBuiltIn = true
                ),
                Template(
                    name = "Travel Log",
                    description = "Itinerary, highlights, and sketch space for trips.",
                    type = TemplateType.TRAVEL_LOG,
                    paperStyle = PaperStyle.BLANK,
                    isBuiltIn = true
                ),
                Template(
                    name = "Book Notes",
                    description = "Record key quotes, ideas, and chapter summaries.",
                    type = TemplateType.BOOK_NOTES,
                    paperStyle = PaperStyle.LINED,
                    isBuiltIn = true
                )
            )
            builtIns.forEach { dao.insertTemplate(it) }
        }
    }
}
