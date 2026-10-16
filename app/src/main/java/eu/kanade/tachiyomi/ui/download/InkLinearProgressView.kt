package eu.kanade.tachiyomi.ui.download

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

/** Lightweight Mangalka progress bar for the legacy RecyclerView download queue. */
class InkLinearProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var max: Int = 100
        set(value) {
            field = max(1, value)
            progress = progress.coerceIn(0, field)
            invalidate()
        }

    var progress: Int = 0
        set(value) {
            field = value.coerceIn(0, max)
            invalidate()
        }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(32, 255, 255, 255)
    }
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 89, 103)
    }

    fun setProgressCompat(value: Int, @Suppress("UNUSED_PARAMETER") animated: Boolean) {
        progress = value
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = dp(4)
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(resolveSize(suggestedMinimumWidth, widthMeasureSpec), height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val radius = height / 2f
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, trackPaint)

        val fraction = progress.toFloat() / max.toFloat()
        val indicatorWidth = width * fraction
        if (indicatorWidth > 0f) {
            canvas.drawRoundRect(0f, 0f, indicatorWidth, height.toFloat(), radius, radius, indicatorPaint)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
