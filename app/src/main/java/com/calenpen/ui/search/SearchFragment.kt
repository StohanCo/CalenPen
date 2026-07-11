package com.calenpen.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.calenpen.CalenPenApplication
import com.calenpen.R
import com.calenpen.databinding.FragmentSearchBinding
import com.calenpen.ui.editor.NoteEditorActivity
import com.calenpen.ui.notes.NotesAdapter

/**
 * Search screen – supports full-text search over note titles, typed text,
 * OCR-recognised handwriting, and date keys.
 */
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels {
        SearchViewModel.Factory(
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
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvSearchResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SearchFragment.adapter
        }

        binding.searchView.apply {
            isIconifiedByDefault = false
            queryHint = getString(R.string.search_hint)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    viewModel.setQuery(query ?: "")
                    return true
                }
                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel.setQuery(newText ?: "")
                    return true
                }
            })
        }

        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            adapter.submitList(results)
            binding.tvEmpty.visibility =
                if (results.isEmpty() && viewModel.query.value.isNotBlank())
                    View.VISIBLE else View.GONE
            binding.tvPrompt.visibility =
                if (viewModel.query.value.isBlank()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
