package com.calenpen.ui.notes

import android.app.Application
import androidx.lifecycle.*
import com.calenpen.data.database.entities.Note
import com.calenpen.data.repository.NoteRepository
import kotlinx.coroutines.launch

class NotesViewModel(
    application: Application,
    private val repository: NoteRepository
) : AndroidViewModel(application) {

    val allNotes: LiveData<List<Note>> = repository.observeAllNotes().asLiveData()
    val pinnedNotes: LiveData<List<Note>> = repository.observePinnedNotes().asLiveData()
    val undatedNotes: LiveData<List<Note>> = repository.observeUndatedNotes().asLiveData()

    fun deleteNote(note: Note) {
        viewModelScope.launch { repository.deleteNote(note) }
    }

    class Factory(
        private val application: Application,
        private val repository: NoteRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
                return NotesViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
