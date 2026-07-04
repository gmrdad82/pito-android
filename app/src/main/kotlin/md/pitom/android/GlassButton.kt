package md.pitom.android

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat

/**
 * Terminal-glass button: translucent pito-blue fill, a thin top highlight,
 * and a border stroked with a slowly drifting blue<->purple gradient
 * (the "shimmer"). Corners stay square — the design system has no radius.
 */
class GlassButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatButton(context, attrs) {

    private val blue = ContextCompat.getColor(context, R.color.pito_blue)
    private val purple = ContextCompat.getColor(context, R.color.pito_purple)

    private val fillPaint = Paint().apply { color = blue and 0x00FFFFFF or (0x30 shl 24) }
    private val pressedPaint = Paint().apply { color = blue and 0x00FFFFFF or (0x60 shl 24) }
    private val highlightPaint = Paint().apply { color = 0x33FFFFFF }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val shaderMatrix = Matrix()
    private var shimmerOffset = 0f
    private val shimmer = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2600
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            shimmerOffset = it.animatedValue as Float
            updateShader()
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        shimmer.start()
    }

    override fun onDetachedFromWindow() {
        shimmer.cancel()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        borderPaint.shader = LinearGradient(
            0f, 0f, w.toFloat(), 0f,
            intArrayOf(blue, purple, blue), null, Shader.TileMode.MIRROR,
        )
        updateShader()
    }

    private fun updateShader() {
        val shader = borderPaint.shader ?: return
        shaderMatrix.setTranslate(shimmerOffset * width * 2f, 0f)
        shader.setLocalMatrix(shaderMatrix)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, if (isPressed) pressedPaint else fillPaint)
        // Glassy top edge.
        canvas.drawRect(2f, 2f, w - 2f, 4f, highlightPaint)
        canvas.drawRect(1.5f, 1.5f, w - 1.5f, h - 1.5f, borderPaint)
        super.onDraw(canvas)
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        invalidate()
    }
}
