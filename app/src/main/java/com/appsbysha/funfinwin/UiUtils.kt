package com.appsbysha.funfinwin

import android.content.Context
import android.util.DisplayMetrics
import kotlin.math.roundToInt

object UiUtils {
    fun dpToPixels(dp: Int, context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        return (dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()
    }

}