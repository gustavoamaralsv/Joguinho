package com.example.joguinho.model

import android.graphics.Canvas
import android.graphics.RectF

/** Barra vertical (cantos arredondados) que sobe e desce, quicando nos limites. */
open class MovingBar(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    private var velocityY: Float,
    private val minY: Float,
    private val maxY: Float,
    val color: Int,
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
        val r = rect.width() / 2f
        canvas.drawRoundRect(rect, r, r, paint)
    }
}
