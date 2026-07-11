package com.calenpen.ui.templates

import android.app.Application
import androidx.lifecycle.*
import com.calenpen.data.database.entities.Template
import com.calenpen.data.repository.NoteRepository
import kotlinx.coroutines.launch

class TemplatesViewModel(
    application: Application,
    private val repository: NoteRepository
) : AndroidViewModel(application) {

    val allTemplates: LiveData<List<Template>> = repository.observeAllTemplates().asLiveData()

    fun deleteTemplate(template: Template) {
        if (template.isBuiltIn) return // never delete built-ins
        viewModelScope.launch { repository.deleteTemplate(template) }
    }

    class Factory(
        private val application: Application,
        private val repository: NoteRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TemplatesViewModel::class.java)) {
                return TemplatesViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
