package com.appsbysha.funfinwin

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.util.DisplayMetrics
import android.view.WindowManager.BadTokenException
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

object UiUtils {
    fun dpToPixels(dp: Int, context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        return (dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()
    }
    fun pixelsToSp(context: Context, sp: Float): Float {
        val scaledDensity = context.resources.displayMetrics.scaledDensity
        return  sp*scaledDensity
    }

    fun createProgressDialog(mContext: Context): Dialog? {
        val dialog = Dialog(mContext)
        try {
            dialog.show()
        } catch (e: BadTokenException) {
        }
        dialog.setCancelable(false)
        dialog.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setContentView(R.layout.dialog_progress)
        val progressBar = dialog.findViewById<ProgressBar>(R.id.loadingProgressBar)
        progressBar.indeterminateDrawable.setColorFilter(
            ContextCompat.getColor(mContext, R.color.colorAccent),
            PorterDuff.Mode.MULTIPLY
        )
        return dialog
    }


}