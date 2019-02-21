package com.sorianog.arstickers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.drawable.Drawable



class PointerDrawable : Drawable() {
    private val paint = Paint()
    private var enabled: Boolean = false

    fun isEnabled(): Boolean {
        return enabled
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun draw(canvas: Canvas) {
        val cx : Float = (canvas.width / 2).toFloat()
        val cy : Float = (canvas.height / 2).toFloat()
        if (enabled) {
            paint.setColor(Color.GREEN)
            canvas.drawCircle(cx, cy, 10F, paint)
        } else {
            paint.setColor(Color.GRAY)
            canvas.drawText("X", cx, cy, paint)
        }
    }

    override fun setAlpha(alpha: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOpacity(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}