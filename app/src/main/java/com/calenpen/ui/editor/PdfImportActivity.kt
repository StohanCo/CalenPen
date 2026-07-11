package com.calenpen.ui.editor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.calenpen.CalenPenApplication
import com.calenpen.R
import com.calenpen.data.database.entities.Note
import com.calenpen.data.database.entities.PaperStyle
import com.calenpen.databinding.ActivityPdfImportBinding
import com.calenpen.utils.PdfImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity that lets the user:
 * 1. Pick a PDF file from storage.
 * 2. Preview all pages as rendered bitmaps.
 * 3. Select individual pages to import as new [com.calenpen.data.database.entities.Note]s.
 */
class PdfImportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfImportBinding
    private val repository by lazy { (application as CalenPenApplication).repository }

    private lateinit var pdfImporter: PdfImporter
    private var selectedPdfUri: Uri? = null
    private var totalPages = 0
    private val selectedPages = mutableSetOf<Int>()

    private val pickPdfLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { loadPdf(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfImportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.import_pdf)

        pdfImporter = PdfImporter(this)

        binding.btnPickPdf.setOnClickListener { pickPdfLauncher.launch("application/pdf") }
        binding.btnImportSelected.setOnClickListener { importSelectedPages() }
        binding.btnImportAll.setOnClickListener { importAllPages() }

        binding.rvPdfPages.layoutManager = LinearLayoutManager(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadPdf(uri: Uri) {
        selectedPdfUri = uri
        selectedPages.clear()
        binding.progressBar.visibility = View.VISIBLE
        binding.btnImportSelected.isEnabled = false
        binding.btnImportAll.isEnabled = false

        lifecycleScope.launch {
            totalPages = pdfImporter.getPageCount(uri)
            if (totalPages == 0) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this@PdfImportActivity,
                    R.string.pdf_load_failed,
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            val bitmaps = withContext(Dispatchers.IO) {
                pdfImporter.renderAllPages(uri, targetWidthPx = 800)
            }

            val adapter = PdfPageAdapter(bitmaps) { pageIndex, selected ->
                if (selected) selectedPages.add(pageIndex) else selectedPages.remove(pageIndex)
                binding.btnImportSelected.isEnabled = selectedPages.isNotEmpty()
            }
            binding.rvPdfPages.adapter = adapter
            binding.progressBar.visibility = View.GONE
            binding.btnImportAll.isEnabled = true
            binding.tvPageCount.text = getString(R.string.pdf_page_count, totalPages)
        }
    }

    private fun importSelectedPages() {
        val uri = selectedPdfUri ?: return
        val dateKey = intent.getStringExtra(NoteEditorActivity.EXTRA_DATE_KEY)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                selectedPages.sorted().forEach { page ->
                    repository.saveNote(
                        Note(
                            title = "PDF Import – Page ${page + 1}",
                            typedText = "",
                            dateKey = dateKey,
                            paperStyle = PaperStyle.BLANK,
                            pdfUri = uri.toString(),
                            pdfPage = page
                        )
                    )
                }
            }
            Toast.makeText(this@PdfImportActivity, R.string.pdf_imported, Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun importAllPages() {
        selectedPages.clear()
        for (i in 0 until totalPages) selectedPages.add(i)
        importSelectedPages()
    }
}
