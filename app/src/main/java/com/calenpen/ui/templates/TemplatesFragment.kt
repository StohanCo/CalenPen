package com.calenpen.ui.templates

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.calenpen.CalenPenApplication
import com.calenpen.R
import com.calenpen.databinding.FragmentTemplatesBinding
import com.calenpen.ui.editor.NoteEditorActivity

/** Displays all available templates. Tapping one opens the editor with that template pre-applied. */
class TemplatesFragment : Fragment() {

    private var _binding: FragmentTemplatesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TemplatesViewModel by viewModels {
        TemplatesViewModel.Factory(
            requireActivity().application,
            (requireActivity().application as CalenPenApplication).repository
        )
    }

    private val adapter = TemplatesAdapter(
        onTemplateClick = { template ->
            startActivity(
                Intent(requireContext(), NoteEditorActivity::class.java)
                    .putExtra(NoteEditorActivity.EXTRA_TEMPLATE_TYPE, template.type)
                    .putExtra(NoteEditorActivity.EXTRA_PAPER_STYLE, template.paperStyle)
            )
        },
        onTemplateDelete = { template -> viewModel.deleteTemplate(template) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTemplatesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvTemplates.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@TemplatesFragment.adapter
        }

        viewModel.allTemplates.observe(viewLifecycleOwner) { templates ->
            adapter.submitList(templates)
            binding.tvEmpty.visibility = if (templates.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
