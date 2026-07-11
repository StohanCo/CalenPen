package com.calenpen.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper for importing PDF files into CalenPen.
 *
 * Each page of a PDF is rendered into a [Bitmap] at a chosen DPI and returned as a list.
 * The caller can then store the URI + page index in the [com.calenpen.data.database.entities.Note]
 * and use [renderPage] to display individual pages on demand.
 */
class PdfImporter(private val context: Context) {

    /**
     * Returns the total number of pages in the PDF at [uri].
     */
    suspend fun getPageCount(uri: Uri): Int = withContext(Dispatchers.IO) {
        openRenderer(uri)?.use { renderer -> renderer.pageCount } ?: 0
    }

    /**
     * Renders a single PDF page to a [Bitmap].
     *
     * @param uri     Content or file URI of the PDF.
     * @param pageIndex 0-based page index.
     * @param targetWidthPx  Desired bitmap width in pixels. Height is calculated proportionally.
     * @return Rendered [Bitmap], or null if the page cannot be rendered.
     */
    suspend fun renderPage(
        uri: Uri,
        pageIndex: Int,
        targetWidthPx: Int = DEFAULT_WIDTH_PX
    ): Bitmap? = withContext(Dispatchers.IO) {
        openRenderer(uri)?.use { renderer ->
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null
            renderer.openPage(pageIndex)?.use { page ->
                val ratio = targetWidthPx.toFloat() / page.width.toFloat()
                val bitmapWidth = targetWidthPx
                val bitmapHeight = (page.height * ratio).toInt()
                val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                // Fill with white background (PDFs are transparent by default)
                Canvas(bitmap).drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        }
    }

    /**
     * Renders all pages of a PDF and returns them as a list of [Bitmap]s.
     *
     * Prefer [renderPage] for lazy on-demand rendering in paginated views.
     */
    suspend fun renderAllPages(
        uri: Uri,
        targetWidthPx: Int = DEFAULT_WIDTH_PX
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        openRenderer(uri)?.use { renderer ->
            for (i in 0 until renderer.pageCount) {
                renderer.openPage(i)?.use { page ->
                    val ratio = targetWidthPx.toFloat() / page.width.toFloat()
                    val bitmapWidth = targetWidthPx
                    val bitmapHeight = (page.height * ratio).toInt()
                    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                    Canvas(bitmap).drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps.add(bitmap)
                }
            }
        }
        bitmaps
    }

    private fun openRenderer(uri: Uri): PdfRenderer? {
        return try {
            val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                ?: return null
            PdfRenderer(parcelFileDescriptor)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        /** Default render width — roughly A4 at 150 dpi. */
        const val DEFAULT_WIDTH_PX = 1240
    }
}
