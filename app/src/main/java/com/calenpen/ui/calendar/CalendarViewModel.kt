package com.calenpen.ui.calendar

import android.app.Application
import androidx.lifecycle.*
import com.calenpen.data.database.entities.CalendarEntry
import com.calenpen.data.database.entities.Note
import com.calenpen.data.repository.NoteRepository
import com.calenpen.utils.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    application: Application,
    private val repository: NoteRepository
) : AndroidViewModel(application) {

    /** Currently displayed month, e.g. "2024-03". */
    private val _currentMonthKey = MutableStateFlow(DateUtils.currentMonthKey())
    val currentMonthKey: StateFlow<String> = _currentMonthKey.asStateFlow()

    /** LiveData wrapper for fragment observers. */
    val currentMonthKeyLive: LiveData<String> = _currentMonthKey.asLiveData()

    /** Calendar entries for the currently displayed month. */
    val calendarEntries: LiveData<List<CalendarEntry>> =
        _currentMonthKey
            .flatMapLatest { monthKey -> repository.observeCalendarEntriesForMonth(monthKey) }
            .asLiveData()

    /** The selected date (for the day-detail panel). */
    private val _selectedDateKey = MutableLiveData<String?>(DateUtils.todayKey())
    val selectedDateKey: LiveData<String?> = _selectedDateKey

    /** Notes for the selected date. */
    val notesForSelectedDate: LiveData<List<Note>> =
        _selectedDateKey.switchMap { dateKey ->
            if (dateKey != null) {
                repository.observeNotesForDate(dateKey).asLiveData()
            } else {
                MutableLiveData(emptyList())
            }
        }

    // ─── Navigation ───────────────────────────────────────────────────────────

    fun navigateToPreviousMonth() {
        val parts = _currentMonthKey.value.split("-")
        val cal = java.util.Calendar.getInstance()
        cal.set(parts[0].toInt(), parts[1].toInt() - 1, 1)
        cal.add(java.util.Calendar.MONTH, -1)
        _currentMonthKey.value = DateUtils.toDateKey(cal.time).take(7)
    }

    fun navigateToNextMonth() {
        val parts = _currentMonthKey.value.split("-")
        val cal = java.util.Calendar.getInstance()
        cal.set(parts[0].toInt(), parts[1].toInt() - 1, 1)
        cal.add(java.util.Calendar.MONTH, 1)
        _currentMonthKey.value = DateUtils.toDateKey(cal.time).take(7)
    }

    fun navigateToToday() {
        _currentMonthKey.value = DateUtils.currentMonthKey()
        _selectedDateKey.value = DateUtils.todayKey()
    }

    fun selectDate(dateKey: String) {
        _selectedDateKey.value = dateKey
        // Also switch month if the selected date is in a different month
        val month = DateUtils.monthKeyOf(dateKey)
        if (month != _currentMonthKey.value) {
            _currentMonthKey.value = month
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    // ─── Factory ─────────────────────────────────────────────────────────────

    class Factory(
        private val application: Application,
        private val repository: NoteRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
                return CalendarViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
