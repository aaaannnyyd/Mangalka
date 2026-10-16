package eu.kanade.tachiyomi.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat

/** Mangalka-styled switch used by legacy Android Preference screens and extension settings. */
class InkPreferenceSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.switchStyle,
) : SwitchCompat(context, attrs, defStyleAttr) {

    init {
        showText = false
        splitTrack = false
        switchMinWidth = dp(46)
        minimumWidth = dp(46)

        thumbTintList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(),
            ),
            intArrayOf(
                Color.rgb(255, 89, 103),
                Color.rgb(112, 118, 126),
            ),
        )
        trackTintList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(),
            ),
            intArrayOf(
                Color.argb(112, 255, 89, 103),
                Color.argb(44, 255, 255, 255),
            ),
        )
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
