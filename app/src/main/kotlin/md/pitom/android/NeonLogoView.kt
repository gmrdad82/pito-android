package md.pitom.android

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.random.Random

/**
 * The PITO block logo, drawn cell-by-cell (no font — Android's monospace
 * renders `█` with gaps, so text can't look like the web's solid blocks).
 * Faithful port of the web broken-neon behavior
 * (pito: logo_reveal_controller.js + pito-logo-flicker keyframes):
 *   - every cell lights at its OWN random offset within REVEAL_WINDOW_MS,
 *   - lighting is the discrete step-flicker (0 -> .85 -> .1 -> 1 -> .35 -> 1),
 *   - after the reveal, one random cell rarely flickers (2.5-7s apart) —
 *     a faulty neon sign warming up. Always plays (same as web, item 18).
 */
class NeonLogoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private class CellState(val cell: AsciiLogo.Cell) {
        var revealOffset = 0L   // ms after start when this cell begins its flicker-in
        var flickerStart = -1L  // absolute ms; -1 = not flickering
    }

    private val cells = AsciiLogo.cells().map { CellState(it) }
    private var startTime = 0L
    private var nextFlickerAt = 0L

    private val blue = ContextCompat.getColor(context, R.color.pito_blue)
    private val dim = ContextCompat.getColor(context, R.color.hint)

    private val blockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = blue }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = blue
        maskFilter = BlurMaskFilter(GLOW_RADIUS, BlurMaskFilter.Blur.NORMAL)
    }
    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dim
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val driver = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1000
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener { invalidate() }
    }

    init {
        // BlurMaskFilter needs software rendering.
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        contentDescription = context.getString(R.string.app_name)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startTime = now()
        nextFlickerAt = startTime + REVEAL_WINDOW_MS + FLICKER_LEAD_MS
        cells.forEach { it.revealOffset = Random.nextLong(REVEAL_WINDOW_MS) }
        driver.start()
    }

    override fun onDetachedFromWindow() {
        driver.cancel()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val cellW = width.toFloat() / AsciiLogo.cols
        val height = (cellW * CELL_ASPECT * AsciiLogo.rows).toInt()
        setMeasuredDimension(width, resolveSize(height, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        val now = now()

        // Rare idle flicker: self-scheduling from the draw loop.
        if (now >= nextFlickerAt) {
            cells[Random.nextInt(cells.size)].flickerStart = now
            nextFlickerAt = now + FLICKER_MIN_MS + Random.nextLong(FLICKER_MAX_MS - FLICKER_MIN_MS)
        }

        val cellW = width.toFloat() / AsciiLogo.cols
        val cellH = cellW * CELL_ASPECT

        for (state in cells) {
            val alpha = alphaFor(state, now)
            if (alpha <= 0f) continue
            val left = state.cell.col * cellW
            val top = state.cell.row * cellH

            if (state.cell.isBlock) {
                glowPaint.alpha = (GLOW_ALPHA * alpha).toInt()
                canvas.drawRect(left, top, left + cellW, top + cellH, glowPaint)
                blockPaint.alpha = (255 * alpha).toInt()
                canvas.drawRect(left, top, left + cellW, top + cellH, blockPaint)
            } else {
                framePaint.alpha = (255 * alpha).toInt()
                drawFrameGlyph(canvas, state.cell.char, left, top, cellW, cellH)
            }
        }
    }

    /** Box-drawing glyphs as strokes so the frame stays connected and crisp. */
    private fun drawFrameGlyph(c: Canvas, ch: Char, l: Float, t: Float, w: Float, h: Float) {
        val mx = l + w / 2f
        val my = t + h / 2f
        when (ch) {
            '═' -> c.drawLine(l, my, l + w, my, framePaint)
            '║' -> c.drawLine(mx, t, mx, t + h, framePaint)
            '╔' -> { c.drawLine(mx, my, l + w, my, framePaint); c.drawLine(mx, my, mx, t + h, framePaint) }
            '╗' -> { c.drawLine(l, my, mx, my, framePaint); c.drawLine(mx, my, mx, t + h, framePaint) }
            '╚' -> { c.drawLine(mx, my, l + w, my, framePaint); c.drawLine(mx, t, mx, my, framePaint) }
            '╝' -> { c.drawLine(l, my, mx, my, framePaint); c.drawLine(mx, t, mx, my, framePaint) }
        }
    }

    private fun alphaFor(state: CellState, now: Long): Float {
        // Idle flicker overrides steady-on (web: .pito-logo__cell.flicker).
        if (state.flickerStart >= 0) {
            val t = now - state.flickerStart
            if (t >= FLICKER_HOLD_MS) state.flickerStart = -1L
            else return when {
                t < 98 -> 1f
                t < 182 -> 0.2f
                else -> 0.85f
            }
        }
        // Flicker-in (web: pito-logo-flicker-in, steps(1,end) over 0.55s).
        val t = now - startTime - state.revealOffset
        return when {
            t < 110 -> 0f
            t < 176 -> 0.85f
            t < 275 -> 0.1f
            t < 341 -> 1f
            t < 550 -> 0.35f
            else -> 1f
        }
    }

    private fun now() = System.currentTimeMillis()

    private companion object {
        // Timing constants verbatim from logo_reveal_controller.js.
        const val REVEAL_WINDOW_MS = 900L
        const val FLICKER_LEAD_MS = 800L
        const val FLICKER_MIN_MS = 2500L
        const val FLICKER_MAX_MS = 7000L
        const val FLICKER_HOLD_MS = 280L
        // Monospace cell proportions (0.6em advance / 1.2em line).
        const val CELL_ASPECT = 2.0f
        const val GLOW_RADIUS = 18f
        const val GLOW_ALPHA = 110
    }
}
