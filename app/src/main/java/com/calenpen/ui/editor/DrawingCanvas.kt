package com.calenpen.ui.editor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.calenpen.utils.RecognitionStroke
import com.calenpen.utils.StrokePoint
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.abs

/**
 * An infinite-page, pressure-sensitive drawing canvas optimised for e-ink stylus devices
 * (NXTPaper) but also functional on regular touch screens.
 *
 * Features:
 *  - Smooth Bézier-curve stroke rendering using [Path].
 *  - Multiple pen types: pen, pencil, highlighter, eraser.
 *  - Configurable stroke colour, width, and opacity.
 *  - Paper background styles: blank, lined, dotted, grid, Cornell.
 *  - Vertical infinite scroll (the canvas grows downward as the user writes).
 *  - Undo / redo stack.
 *  - Stroke serialisation to / from JSON for persistence in Room.
 */
class DrawingCanvas @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ─── Enums ────────────────────────────────────────────────────────────────

    enum class PenType { PEN, PENCIL, HIGHLIGHTER, ERASER }
    enum class PaperBackground { BLANK, LINED, DOTTED, GRID, CORNELL }

    // ─── Public state ─────────────────────────────────────────────────────────

    var penType: PenType = PenType.PEN
        set(value) { field = value; updatePaint() }

    var strokeColour: Int = Color.BLACK
        set(value) { field = value; updatePaint() }

    var strokeWidthDp: Float = 3f
        set(value) { field = value; updatePaint() }

    var paperBackground: PaperBackground = PaperBackground.BLANK
        set(value) { field = value; invalidate() }

    var paperColour: Int = Color.WHITE
        set(value) { field = value; invalidate() }

    /** Called whenever a stroke is completed (finger/pen lifted). */
    var onStrokeCompleted: (() -> Unit)? = null

    // ─── Internal data ────────────────────────────────────────────────────────

    private val strokes = mutableListOf<StrokeData>()
    private val undoStack = mutableListOf<StrokeData>()
    private val redoStack = mutableListOf<StrokeData>()

    private val currentPath = Path()
    private val currentPoints = mutableListOf<StrokePoint>()
    private var currentStrokeStartTime = 0L

    private var lastX = 0f
    private var lastY = 0f

    private val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DDDDDD")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private var canvasHeight = 0

    private val gson = Gson()

    // ─── Init ─────────────────────────────────────────────────────────────────

    init {
        updatePaint()
        setLayerType(LAYER_TYPE_HARDWARE, null) // GPU-accelerated for smooth inking
    }

    // ─── Measurement ─────────────────────────────────────────────────────────

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        // Canvas grows as strokes extend below the initial viewport
        val minH = MeasureSpec.getSize(heightMeasureSpec)
        val h = maxOf(minH, canvasHeight + CANVAS_EXTENSION_PX)
        setMeasuredDimension(w, h)
    }

    // ─── Drawing ─────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Background
        backgroundPaint.color = paperColour
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        // Guide lines
        drawBackground(canvas)

        // Committed strokes
        for (stroke in strokes) {
            canvas.drawPath(stroke.path, stroke.paint)
        }

        // In-progress stroke
        canvas.drawPath(currentPath, drawPaint)
    }

    private fun drawBackground(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        when (paperBackground) {
            PaperBackground.LINED -> {
                var y = LINE_SPACING_PX.toFloat()
                while (y < h) { canvas.drawLine(0f, y, w, y, guidePaint); y += LINE_SPACING_PX }
            }
            PaperBackground.DOTTED -> {
                var y = LINE_SPACING_PX.toFloat()
                while (y < h) {
                    var x = LINE_SPACING_PX.toFloat()
                    while (x < w) { canvas.drawCircle(x, y, 2f, guidePaint); x += LINE_SPACING_PX }
                    y += LINE_SPACING_PX
                }
            }
            PaperBackground.GRID -> {
                var y = LINE_SPACING_PX.toFloat()
                while (y < h) { canvas.drawLine(0f, y, w, y, guidePaint); y += LINE_SPACING_PX }
                var x = LINE_SPACING_PX.toFloat()
                while (x < w) { canvas.drawLine(x, 0f, x, h, guidePaint); x += LINE_SPACING_PX }
            }
            PaperBackground.CORNELL -> {
                // Vertical margin line
                val margin = (w * 0.25f)
                guidePaint.color = Color.parseColor("#FFAAAA")
                canvas.drawLine(margin, 0f, margin, h, guidePaint)
                guidePaint.color = Color.parseColor("#DDDDDD")
                // Horizontal lines
                var y = LINE_SPACING_PX.toFloat()
                while (y < h) { canvas.drawLine(0f, y, w, y, guidePaint); y += LINE_SPACING_PX }
                // Summary section at the bottom
                val summaryY = h - LINE_SPACING_PX * 4
                guidePaint.color = Color.parseColor("#AAAAFF")
                canvas.drawLine(0f, summaryY, w, summaryY, guidePaint)
                guidePaint.color = Color.parseColor("#DDDDDD")
            }
            PaperBackground.BLANK -> { /* no guides */ }
        }
    }

    // ─── Touch handling ───────────────────────────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                currentStrokeStartTime = System.currentTimeMillis()
                currentPoints.clear()
                currentPath.moveTo(x, y)
                currentPoints.add(StrokePoint(x, y, 0))
                lastX = x; lastY = y
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(x - lastX)
                val dy = abs(y - lastY)
                if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                    currentPath.quadTo(lastX, lastY, (x + lastX) / 2f, (y + lastY) / 2f)
                    val elapsed = System.currentTimeMillis() - currentStrokeStartTime
                    currentPoints.add(StrokePoint(x, y, elapsed))
                    lastX = x; lastY = y
                    // Extend canvas if writing near the bottom
                    if (y > canvasHeight - CANVAS_EXTENSION_THRESHOLD_PX) {
                        canvasHeight = y.toInt() + CANVAS_EXTENSION_PX
                        requestLayout()
                    }
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                currentPath.lineTo(x, y)
                commitCurrentStroke()
                currentPath.reset()
                currentPoints.clear()
                onStrokeCompleted?.invoke()
                invalidate()
            }
        }
        return true
    }

    private fun commitCurrentStroke() {
        if (penType == PenType.ERASER) {
            eraseAtPath(currentPath)
        } else {
            val snapshotPath = Path(currentPath)
            val snapshotPaint = Paint(drawPaint)
            val snapshotPoints = currentPoints.toList()
            val stroke = StrokeData(
                path = snapshotPath,
                paint = snapshotPaint,
                points = snapshotPoints,
                colour = strokeColour,
                widthDp = strokeWidthDp,
                penType = penType
            )
            strokes.add(stroke)
            redoStack.clear()
    }

    private fun eraseAtPath(eraserPath: Path) {
        val eraserRegion = Region()
        val clip = RectF()
        eraserPath.computeBounds(clip, true)
        clip.inset(-strokeWidthDp * 4, -strokeWidthDp * 4)
        eraserRegion.setPath(eraserPath, Region(clip.left.toInt(), clip.top.toInt(), clip.right.toInt(), clip.bottom.toInt()))
        strokes.removeAll { stroke ->
            val strokeRegion = Region()
            val strokeBounds = RectF()
            stroke.path.computeBounds(strokeBounds, true)
            strokeRegion.setPath(stroke.path, Region(strokeBounds.left.toInt(), strokeBounds.top.toInt(), strokeBounds.right.toInt(), strokeBounds.bottom.toInt()))
            !strokeRegion.quickReject(eraserRegion) && strokeRegion.op(eraserRegion, Region.Op.INTERSECT)
        }
    }

    // ─── Undo / Redo ──────────────────────────────────────────────────────────

    fun undo() {
        if (strokes.isNotEmpty()) {
            val removed = strokes.removeLast()
            redoStack.add(removed)
            invalidate()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val restored = redoStack.removeLast()
            strokes.add(restored)
            invalidate()
        }
    }

    fun canUndo(): Boolean = strokes.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    // ─── Serialisation ────────────────────────────────────────────────────────

    /**
     * Serialises all committed strokes to a JSON string suitable for storage in Room.
     */
    fun toJson(): String {
        val serialisable = strokes.map { it.toSerialised() }
        return gson.toJson(serialisable)
    }

    /**
     * Deserialises stroke data from a JSON string and re-renders the canvas.
     */
    fun fromJson(json: String?) {
        if (json.isNullOrBlank()) return
        try {
            val type = object : TypeToken<List<SerialisedStroke>>() {}.type
            val list: List<SerialisedStroke> = gson.fromJson(json, type)
            strokes.clear()
            strokes.addAll(list.map { it.toStrokeData() })
            canvasHeight = strokes.maxOfOrNull { stroke ->
                val b = RectF(); stroke.path.computeBounds(b, true); b.bottom.toInt()
            } ?: 0
            invalidate()
            requestLayout()
        } catch (e: Exception) {
            // Malformed JSON — start fresh
        }
    }

    /**
     * Returns all strokes in a format suitable for ML Kit handwriting recognition.
     */
    fun toRecognitionStrokes(): List<RecognitionStroke> =
        strokes.map { RecognitionStroke(it.points) }

    /** Clears the canvas completely. */
    fun clear() {
        strokes.clear()
        undoStack.clear()
        redoStack.clear()
        currentPath.reset()
        currentPoints.clear()
        canvasHeight = 0
        invalidate()
        requestLayout()
    }

    // ─── Paint builder ────────────────────────────────────────────────────────

    private fun updatePaint() {
        drawPaint.apply {
            strokeWidth = strokeWidthDp * resources.displayMetrics.density
            when (penType) {
                PenType.PEN -> {
                    color = strokeColour
                    alpha = 255
                    maskFilter = null
                    xfermode = null
                }
                PenType.PENCIL -> {
                    color = strokeColour
                    alpha = 180
                    maskFilter = BlurMaskFilter(2f, BlurMaskFilter.Blur.NORMAL)
                    xfermode = null
                }
                PenType.HIGHLIGHTER -> {
                    color = strokeColour
                    alpha = 80
                    strokeWidth = strokeWidthDp * resources.displayMetrics.density * 4
                    maskFilter = null
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
                }
                PenType.ERASER -> {
                    color = paperColour
                    alpha = 255
                    strokeWidth = strokeWidthDp * resources.displayMetrics.density * 6
                    maskFilter = null
                    xfermode = null
                }
            }
        }
    }

    // ─── Data classes ─────────────────────────────────────────────────────────

    internal data class StrokeData(
        val path: Path,
        val paint: Paint,
        val points: List<StrokePoint>,
        val colour: Int,
        val widthDp: Float,
        val penType: PenType
    ) {
        fun toSerialised() = SerialisedStroke(
            points = points.map { listOf(it.x, it.y, it.timestamp.toFloat()) },
            colour = colour,
            widthDp = widthDp,
            penType = penType.name
        )
    }

    data class SerialisedStroke(
        val points: List<List<Float>>,
        val colour: Int,
        val widthDp: Float,
        val penType: String
    ) {
        fun toStrokeData(): StrokeData {
            val path = Path()
            val strokePoints = points.map { StrokePoint(it[0], it[1], it[2].toLong()) }
            strokePoints.forEachIndexed { i, pt ->
                if (i == 0) path.moveTo(pt.x, pt.y)
                else {
                    val prev = strokePoints[i - 1]
                    path.quadTo(prev.x, prev.y, (pt.x + prev.x) / 2f, (pt.y + prev.y) / 2f)
                }
            }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = colour
                strokeWidth = widthDp * 3f // approximate density
                val resolvedPenType = PenType.values().find { it.name == penType } ?: PenType.PEN
                if (resolvedPenType == PenType.HIGHLIGHTER) { alpha = 80 }
                if (resolvedPenType == PenType.PENCIL) { alpha = 180 }
            }
            return StrokeData(
                path = path,
                paint = paint,
                points = strokePoints,
                colour = colour,
                widthDp = widthDp,
                penType = PenType.values().find { it.name == penType } ?: PenType.PEN
            )
        }
    }

    companion object {
        private const val TOUCH_TOLERANCE = 4f
        private const val LINE_SPACING_PX = 64
        private const val CANVAS_EXTENSION_PX = 1200
        private const val CANVAS_EXTENSION_THRESHOLD_PX = 200
    }
}
