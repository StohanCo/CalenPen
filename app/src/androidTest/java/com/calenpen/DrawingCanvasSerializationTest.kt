package com.calenpen

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.calenpen.ui.editor.DrawingCanvas
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented tests for serialisable stroke data (requires Android runtime for Path/Paint). */
@RunWith(AndroidJUnit4::class)
class DrawingCanvasSerializationTest {

    @Test
    fun `SerialisedStroke round-trips through toStrokeData without crashing`() {
        val serialised = DrawingCanvas.SerialisedStroke(
            points = listOf(listOf(10f, 20f, 0f), listOf(30f, 40f, 100f)),
            colour = -16777216, // Color.BLACK
            widthDp = 3f,
            penType = "PEN"
        )
        val data = serialised.toStrokeData()
        assertNotNull(data)
    }

    @Test
    fun `SerialisedStroke with HIGHLIGHTER penType does not throw`() {
        val serialised = DrawingCanvas.SerialisedStroke(
            points = listOf(listOf(5f, 5f, 0f), listOf(50f, 50f, 200f)),
            colour = -256, // Color.YELLOW
            widthDp = 12f,
            penType = "HIGHLIGHTER"
        )
        assertDoesNotThrow { serialised.toStrokeData() }
    }

    @Test
    fun `SerialisedStroke with unknown penType does not throw`() {
        val serialised = DrawingCanvas.SerialisedStroke(
            points = listOf(listOf(1f, 1f, 0f)),
            colour = -16777216,
            widthDp = 2f,
            penType = "UNKNOWN_TYPE"
        )
        // toStrokeData() should fall back to PEN rather than throwing
        assertDoesNotThrow { serialised.toStrokeData() }
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            fail("Expected no exception but got: ${e.message}")
        }
    }
}
