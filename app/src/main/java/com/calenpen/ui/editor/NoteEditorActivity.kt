package com.calenpen.ui.editor

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.calenpen.CalenPenApplication
import com.calenpen.R
import com.calenpen.data.database.entities.PaperStyle
import com.calenpen.databinding.ActivityNoteEditorBinding
import com.calenpen.utils.DateUtils

/**
 * Full-screen note editor with:
 *  - Infinite-scroll handwriting canvas ([DrawingCanvas])
 *  - Pen / pencil / highlighter / eraser tool switcher
 *  - Colour picker
 *  - Undo / redo
 *  - Paper style switcher
 *  - Title input
 *  - Date assignment
 *  - Handwriting-to-text OCR trigger
 */
class NoteEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteEditorBinding
    private val viewModel: NoteEditorViewModel by viewModels {
        NoteEditorViewModel.Factory(
            application,
            (application as CalenPenApplication).repository
        )
    }

    private var selectedDateKey: String? = null
    private var currentPaperStyle: String = PaperStyle.BLANK

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        selectedDateKey = intent.getStringExtra(EXTRA_DATE_KEY)

        if (noteId > 0) {
            viewModel.loadNote(noteId)
        } else {
            val templateType = intent.getStringExtra(EXTRA_TEMPLATE_TYPE)
                ?: com.calenpen.data.database.entities.TemplateType.BLANK
            val paperStyle = intent.getStringExtra(EXTRA_PAPER_STYLE) ?: PaperStyle.BLANK
            viewModel.newNote(dateKey = selectedDateKey, templateType = templateType, paperStyle = paperStyle)
        }

        setupCanvas()
        setupToolbar()
        setupToolPalette()
        observeViewModel()

        // Handle back navigation using the modern OnBackPressedDispatcher (API 33+ compatible)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                saveAndFinish()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_note_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { saveAndFinish(); true }
            R.id.action_save -> { saveAndFinish(); true }
            R.id.action_recognise -> { triggerHandwritingRecognition(); true }
            R.id.action_paper_style -> { showPaperStylePicker(); true }
            R.id.action_undo -> { binding.drawingCanvas.undo(); true }
            R.id.action_redo -> { binding.drawingCanvas.redo(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ─── Setup ────────────────────────────────────────────────────────────────

    private fun setupCanvas() {
        binding.drawingCanvas.onStrokeCompleted = {
            // Auto-save strokes after each stroke
            autoSave()
        }
    }

    private fun setupToolbar() {
        binding.dateChip.setOnClickListener { showDatePicker() }
        selectedDateKey?.let { binding.dateChip.text = DateUtils.toShortLabel(it) }
            ?: run { binding.dateChip.text = getString(R.string.no_date) }
    }

    private fun setupToolPalette() {
        // Pen type buttons
        binding.btnPen.setOnClickListener {
            binding.drawingCanvas.penType = DrawingCanvas.PenType.PEN
            highlightActiveTool(binding.btnPen)
        }
        binding.btnPencil.setOnClickListener {
            binding.drawingCanvas.penType = DrawingCanvas.PenType.PENCIL
            highlightActiveTool(binding.btnPencil)
        }
        binding.btnHighlighter.setOnClickListener {
            binding.drawingCanvas.penType = DrawingCanvas.PenType.HIGHLIGHTER
            highlightActiveTool(binding.btnHighlighter)
        }
        binding.btnEraser.setOnClickListener {
            binding.drawingCanvas.penType = DrawingCanvas.PenType.ERASER
            highlightActiveTool(binding.btnEraser)
        }

        // Colour buttons
        val colourMap = mapOf(
            binding.colourBlack to Color.BLACK,
            binding.colourBlue to Color.parseColor("#1565C0"),
            binding.colourRed to Color.parseColor("#B71C1C"),
            binding.colourGreen to Color.parseColor("#2E7D32"),
            binding.colourOrange to Color.parseColor("#E65100"),
            binding.colourPurple to Color.parseColor("#6A1B9A")
        )
        colourMap.forEach { (btn, colour) ->
            btn.setOnClickListener {
                binding.drawingCanvas.strokeColour = colour
                colourMap.keys.forEach { b -> b.alpha = 0.5f }
                btn.alpha = 1f
            }
        }

        // Stroke width slider
        binding.strokeWidthSlider.addOnChangeListener { _, value, _ ->
            binding.drawingCanvas.strokeWidthDp = value
        }
    }

    private fun highlightActiveTool(activeBtn: View) {
        listOf(binding.btnPen, binding.btnPencil, binding.btnHighlighter, binding.btnEraser)
            .forEach { it.alpha = 0.5f }
        activeBtn.alpha = 1f
    }

    // ─── ViewModel observation ────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.note.observe(this) { note ->
            if (!binding.editTitle.isFocused && binding.editTitle.text.toString() != note.title) {
                binding.editTitle.setText(note.title)
            }
            binding.drawingCanvas.fromJson(note.strokesJson)
            currentPaperStyle = note.paperStyle
            binding.drawingCanvas.paperBackground = when (note.paperStyle) {
                PaperStyle.LINED -> DrawingCanvas.PaperBackground.LINED
                PaperStyle.DOTTED -> DrawingCanvas.PaperBackground.DOTTED
                PaperStyle.GRID -> DrawingCanvas.PaperBackground.GRID
                PaperStyle.CORNELL -> DrawingCanvas.PaperBackground.CORNELL
                else -> DrawingCanvas.PaperBackground.BLANK
            }
            note.dateKey?.let {
                selectedDateKey = it
                binding.dateChip.text = DateUtils.toShortLabel(it)
            }
        }

        viewModel.recognisedText.observe(this) { text ->
            if (text.isNotBlank()) {
                binding.recognisedTextCard.isVisible = true
                binding.tvRecognisedText.text = text
            }
        }

        viewModel.isSaving.observe(this) { saving ->
            binding.savingIndicator.isVisible = saving
        }

        viewModel.saveResult.observe(this) { id ->
            if (id != null) {
                setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_NOTE_ID, id))
            }
        }
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    private fun autoSave() {
        viewModel.saveNote(
            title = binding.editTitle.text.toString(),
            strokesJson = binding.drawingCanvas.toJson(),
            typedText = "",
            dateKey = selectedDateKey,
            paperStyle = currentPaperStyle
        )
    }

    private fun saveAndFinish() {
        viewModel.saveNote(
            title = binding.editTitle.text.toString(),
            strokesJson = binding.drawingCanvas.toJson(),
            typedText = "",
            dateKey = selectedDateKey,
            paperStyle = currentPaperStyle
        )
        finish()
    }

    private fun triggerHandwritingRecognition() {
        Toast.makeText(this, R.string.recognising_handwriting, Toast.LENGTH_SHORT).show()
        viewModel.recogniseHandwriting(binding.drawingCanvas)
    }

    private fun showPaperStylePicker() {
        val styles = arrayOf(
            getString(R.string.paper_blank),
            getString(R.string.paper_lined),
            getString(R.string.paper_dotted),
            getString(R.string.paper_grid),
            getString(R.string.paper_cornell)
        )
        val styleKeys = arrayOf(
            PaperStyle.BLANK, PaperStyle.LINED, PaperStyle.DOTTED,
            PaperStyle.GRID, PaperStyle.CORNELL
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.choose_paper_style)
            .setItems(styles) { _, which ->
                currentPaperStyle = styleKeys[which]
                binding.drawingCanvas.paperBackground = when (styleKeys[which]) {
                    PaperStyle.LINED -> DrawingCanvas.PaperBackground.LINED
                    PaperStyle.DOTTED -> DrawingCanvas.PaperBackground.DOTTED
                    PaperStyle.GRID -> DrawingCanvas.PaperBackground.GRID
                    PaperStyle.CORNELL -> DrawingCanvas.PaperBackground.CORNELL
                    else -> DrawingCanvas.PaperBackground.BLANK
                }
            }
            .show()
    }

    private fun showDatePicker() {
        val cal = java.util.Calendar.getInstance()
        android.app.DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDateKey = DateUtils.buildDateKey(year, month + 1, day)
                binding.dateChip.text = DateUtils.toShortLabel(selectedDateKey!!)
            },
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH),
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    companion object {
        const val EXTRA_NOTE_ID = "extra_note_id"
        const val EXTRA_DATE_KEY = "extra_date_key"
        const val EXTRA_TEMPLATE_TYPE = "extra_template_type"
        const val EXTRA_PAPER_STYLE = "extra_paper_style"
    }
}
