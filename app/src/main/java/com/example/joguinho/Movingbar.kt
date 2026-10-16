package com.example.joguinho

import android.graphics.Canvas
import android.graphics.RectF

/** Barra vertical que sobe e desce, quicando nos limites. */
open class MovingBar(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    private var velocityY: Float,
    private val minY: Float,
    private val maxY: Float,
    color: Int
) : GameElement() {

    val rect = RectF(left, top, left + width, top + height)

    init {
        paint.color = color
    }

    fun update(seconds: Double) {
        rect.offset(0f, (velocityY * seconds).toFloat())
        if (rect.top < minY) {
            rect.offset(0f, minY - rect.top)
            velocityY = -velocityY
        } else if (rect.bottom > maxY) {
            rect.offset(0f, maxY - rect.bottom)
            velocityY = -velocityY
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(rect, paint)
    }
}
