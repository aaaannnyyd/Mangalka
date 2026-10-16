package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatButton
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer

/**
 * Reader-only glass button used by legacy page-error views.
 *
 * It intentionally does not inherit MaterialButton: page decode/network failures can appear
 * directly inside the viewer, so even this fallback state must use Mangalka chrome.
 */
class ReaderButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.buttonStyle,
) : AppCompatButton(context, attrs, defStyleAttr) {

    var viewer: PagerViewer? = null

    init {
        isAllCaps = false
        minWidth = 0
        minHeight = dp(44)
        setPadding(dp(18), dp(10), dp(18), dp(10))
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        backgroundTintList = null

        val glass = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(18).toFloat()
            setColor(Color.argb(232, 27, 31, 36))
            setStroke(dp(1), Color.argb(32, 255, 255, 255))
        }
        background = RippleDrawable(
            ColorStateList.valueOf(Color.argb(36, 255, 89, 103)),
            glass,
            null,
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        viewer?.pager?.setGestureDetectorEnabled(false)
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            viewer?.pager?.setGestureDetectorEnabled(true)
        }
        return super.onTouchEvent(event)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
