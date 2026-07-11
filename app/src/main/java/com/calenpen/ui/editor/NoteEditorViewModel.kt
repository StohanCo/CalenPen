package com.calenpen.ui.editor

import android.app.Application
import androidx.lifecycle.*
import com.calenpen.data.database.entities.Note
import com.calenpen.data.database.entities.PaperStyle
import com.calenpen.data.database.entities.TemplateType
import com.calenpen.data.repository.NoteRepository
import com.calenpen.utils.HandwritingRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NoteEditorViewModel(
    application: Application,
    private val repository: NoteRepository
) : AndroidViewModel(application) {

    private val recognizer = HandwritingRecognizer()

    private val _note = MutableLiveData<Note>()
    val note: LiveData<Note> = _note

    private val _isSaving = MutableLiveData(false)
    val isSaving: LiveData<Boolean> = _isSaving

    private val _recognisedText = MutableLiveData<String>()
    val recognisedText: LiveData<String> = _recognisedText

    private val _saveResult = MutableLiveData<Long?>()
    val saveResult: LiveData<Long?> = _saveResult

    // ─── Load ─────────────────────────────────────────────────────────────────

    fun loadNote(noteId: Long) {
        viewModelScope.launch {
            val loaded = repository.getNoteById(noteId)
            _note.value = loaded ?: Note()
        }
    }

    fun newNote(
        dateKey: String? = null,
        templateType: String = TemplateType.BLANK,
        paperStyle: String = PaperStyle.BLANK
    ) {
        _note.value = Note(
            dateKey = dateKey,
            templateType = templateType,
            paperStyle = paperStyle
        )
    }

    // ─── Save ─────────────────────────────────────────────────────────────────

    /**
     * Persists the note with updated strokes, typed text, and metadata.
     */
    fun saveNote(
        title: String,
        strokesJson: String?,
        typedText: String,
        dateKey: String?,
        paperStyle: String
    ) {
        val current = _note.value ?: Note()
        val updated = current.copy(
            title = title.ifBlank { getApplication<Application>().getString(com.calenpen.R.string.untitled_note) },
            strokesJson = strokesJson,
            typedText = typedText,
            dateKey = dateKey,
            paperStyle = paperStyle,
            updatedAt = System.currentTimeMillis()
        )
        _isSaving.value = true
        viewModelScope.launch {
            val id = repository.saveNote(updated)
            _note.value = updated.copy(id = id)
            _isSaving.value = false
            _saveResult.value = id
        }
    }

    // ─── Handwriting recognition ──────────────────────────────────────────────

    /**
     * Runs OCR on the current stroke data and persists the result.
     */
    fun recogniseHandwriting(canvas: DrawingCanvas) {
        val noteId = _note.value?.id ?: return
        viewModelScope.launch {
            val strokes = withContext(Dispatchers.Default) {
                canvas.toRecognitionStrokes()
            }
            if (strokes.isEmpty()) return@launch
            try {
                recognizer.downloadModelIfNeeded()
                val text = recognizer.recognise(strokes)
                _recognisedText.value = text
                if (noteId > 0 && text.isNotBlank()) {
                    repository.updateRecognisedText(noteId, text)
                }
            } catch (e: Exception) {
                // Recognition failed — leave existing recognised text unchanged
            }
        }
    }

    // ─── Factory ─────────────────────────────────────────────────────────────

    class Factory(
        private val application: Application,
        private val repository: NoteRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NoteEditorViewModel::class.java)) {
                return NoteEditorViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
