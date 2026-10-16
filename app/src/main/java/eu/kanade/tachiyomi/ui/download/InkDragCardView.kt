package eu.kanade.tachiyomi.ui.download

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.widget.FrameLayout

/** Smoked-glass RecyclerView container with a lightweight drag highlight. */
class InkDragCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    var isDragged: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            elevation = if (value) dp(6).toFloat() else 0f
            refreshGlass()
        }

    init {
        clipToOutline = true
        refreshGlass()
    }

    private fun refreshGlass() {
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(16).toFloat()
            setColor(Color.argb(if (isDragged) 242 else 208, 25, 29, 33))
            setStroke(
                dp(1),
                if (isDragged) Color.argb(150, 255, 89, 103) else Color.argb(32, 255, 255, 255),
            )
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
