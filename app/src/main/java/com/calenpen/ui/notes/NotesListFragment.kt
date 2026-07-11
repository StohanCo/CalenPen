package com.calenpen.ui.notes

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.calenpen.CalenPenApplication
import com.calenpen.R
import com.calenpen.databinding.FragmentNotesListBinding
import com.calenpen.ui.editor.NoteEditorActivity

/** Displays all notes in reverse-chronological order. */
class NotesListFragment : Fragment() {

    private var _binding: FragmentNotesListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotesViewModel by viewModels {
        NotesViewModel.Factory(
            requireActivity().application,
            (requireActivity().application as CalenPenApplication).repository
        )
    }

    private val adapter = NotesAdapter(
        onNoteClick = { note ->
            startActivity(
                Intent(requireContext(), NoteEditorActivity::class.java)
                    .putExtra(NoteEditorActivity.EXTRA_NOTE_ID, note.id)
            )
        },
        onNoteDelete = { note -> viewModel.deleteNote(note) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvNotes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@NotesListFragment.adapter
        }

        binding.fabNewNote.setOnClickListener {
            startActivity(Intent(requireContext(), NoteEditorActivity::class.java))
        }

        viewModel.allNotes.observe(viewLifecycleOwner) { notes ->
            adapter.submitList(notes)
            binding.tvEmpty.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
