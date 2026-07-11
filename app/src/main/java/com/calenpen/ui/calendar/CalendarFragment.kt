package com.calenpen.ui.calendar

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.calenpen.CalenPenApplication
import com.calenpen.R
import com.calenpen.databinding.FragmentCalendarBinding
import com.calenpen.ui.editor.NoteEditorActivity
import com.calenpen.ui.notes.NotesAdapter
import com.calenpen.utils.DateUtils

/**
 * Main calendar screen.
 *
 * Layout: a month grid at the top + a vertical list of notes for the selected day below.
 * Users navigate months via prev/next arrow buttons and tap a day to view / create notes.
 */
class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CalendarViewModel by viewModels {
        CalendarViewModel.Factory(
            requireActivity().application,
            (requireActivity().application as CalenPenApplication).repository
        )
    }

    private val calendarAdapter = CalendarAdapter { dateKey ->
        viewModel.selectDate(dateKey)
    }

    private val notesAdapter = NotesAdapter(
        onNoteClick = { note ->
            startActivity(
                Intent(requireContext(), NoteEditorActivity::class.java)
                    .putExtra(NoteEditorActivity.EXTRA_NOTE_ID, note.id)
            )
        },
        onNoteDelete = { note -> viewModel.deleteNote(note) }
    )

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCalendarGrid()
        setupNotesList()
        setupControls()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ─── Setup ────────────────────────────────────────────────────────────────

    private fun setupCalendarGrid() {
        binding.rvCalendar.apply {
            layoutManager = GridLayoutManager(requireContext(), 7)
            adapter = calendarAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupNotesList() {
        binding.rvDayNotes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notesAdapter
        }
    }

    private fun setupControls() {
        binding.btnPrevMonth.setOnClickListener { viewModel.navigateToPreviousMonth() }
        binding.btnNextMonth.setOnClickListener { viewModel.navigateToNextMonth() }
        binding.btnToday.setOnClickListener { viewModel.navigateToToday() }
        binding.fabNewNote.setOnClickListener { openNewNoteForSelectedDate() }
    }

    // ─── ViewModel ────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.currentMonthKeyLive.observe(viewLifecycleOwner) { monthKey ->
            binding.tvMonthYear.text = DateUtils.toMonthYearLabel(monthKey)
        }

        viewModel.calendarEntries.observe(viewLifecycleOwner) { entries ->
            val entriesMap = entries.associateBy { it.dateKey }
            calendarAdapter.submitMonth(
                monthKey = viewModel.currentMonthKey.value,
                entries = entriesMap,
                selectedDateKey = viewModel.selectedDateKey.value
            )
        }

        viewModel.selectedDateKey.observe(viewLifecycleOwner) { dateKey ->
            binding.tvSelectedDate.text = dateKey?.let { DateUtils.toShortLabel(it) }
                ?: getString(R.string.no_date_selected)
            // Re-bind adapter so the selected day highlight updates
            viewModel.calendarEntries.value?.let { entries ->
                calendarAdapter.submitMonth(
                    monthKey = viewModel.currentMonthKey.value,
                    entries = entries.associateBy { it.dateKey },
                    selectedDateKey = dateKey
                )
            }
        }

        viewModel.notesForSelectedDate.observe(viewLifecycleOwner) { notes ->
            notesAdapter.submitList(notes)
            binding.tvEmptyDayNotes.visibility =
                if (notes.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    private fun openNewNoteForSelectedDate() {
        val dateKey = viewModel.selectedDateKey.value ?: DateUtils.todayKey()
        startActivity(
            Intent(requireContext(), NoteEditorActivity::class.java)
                .putExtra(NoteEditorActivity.EXTRA_DATE_KEY, dateKey)
        )
    }
}
