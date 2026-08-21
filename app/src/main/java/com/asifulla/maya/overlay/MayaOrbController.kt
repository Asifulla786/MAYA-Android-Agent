package com.asifulla.maya.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.*
import android.widget.TextView
import kotlin.math.roundToInt

class MayaOrbController(private val context: Context) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: TextView? = null

    fun show() {
        if (view != null || !android.provider.Settings.canDrawOverlays(context)) return
        val orb = TextView(context).apply {
            text = "M"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xCC171A24.toInt())
        }
        val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        val params = WindowManager.LayoutParams(72.dp, 72.dp, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.END; x = 16.dp; y = 180.dp }
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0
        orb.setOnTouchListener { _, e -> when (e.action) {
            MotionEvent.ACTION_DOWN -> { downX=e.rawX; downY=e.rawY; startX=params.x; startY=params.y; true }
            MotionEvent.ACTION_MOVE -> { params.x=startX-(e.rawX-downX).roundToInt(); params.y=startY+(e.rawY-downY).roundToInt(); wm.updateViewLayout(orb, params); true }
            MotionEvent.ACTION_UP -> { if (kotlin.math.abs(e.rawX-downX)<12 && kotlin.math.abs(e.rawY-downY)<12) orb.alpha=if(orb.alpha>0.5f)0.45f else 1f; true }
            else -> false }
        }
        wm.addView(orb, params); view = orb
    }
    fun hide() { view?.let { wm.removeView(it) }; view = null }
    private val Int.dp get() = (this * context.resources.displayMetrics.density).roundToInt()
}
