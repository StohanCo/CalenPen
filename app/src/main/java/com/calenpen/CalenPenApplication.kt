package com.calenpen

import android.app.Application
import com.calenpen.data.database.AppDatabase
import com.calenpen.data.repository.NoteRepository

/**
 * Application entry point.
 *
 * Holds the single [NoteRepository] instance used by all ViewModels so that
 * the database is only created once per process.
 */
class CalenPenApplication : Application() {

    val repository: NoteRepository by lazy {
        val db = AppDatabase.getInstance(this)
        NoteRepository(
            noteDao = db.noteDao(),
            calendarEntryDao = db.calendarEntryDao(),
            templateDao = db.templateDao()
        )
    }
}
