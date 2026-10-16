package com.example.joguinho

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

/** Bala do canhão: um círculo com rastro e velocidade em pixels por segundo. */
class Cannonball(val radius: Float) : GameElement() {
    companion object {
        private const val TRAIL = 8
    }

    var x = 0f
    var y = 0f
    var velocityX = 0f
    var velocityY = 0f
    var onScreen = false

    private val trailX = FloatArray(TRAIL)
    private val trailY = FloatArray(TRAIL)
    private var trailCount = 0
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 140, 0) }

    init {
        paint.color = Color.rgb(40, 40, 40)
    }

    fun launch(startX: Float, startY: Float, vx: Float, vy: Float) {
        x = startX
        y = startY
        velocityX = vx
        velocityY = vy
        trailCount = 0
        onScreen = true
    }

    fun update(seconds: Double) {
        for (i in TRAIL - 1 downTo 1) {
            trailX[i] = trailX[i - 1]
            trailY[i] = trailY[i - 1]
        }
        trailX[0] = x
        trailY[0] = y
        if (trailCount < TRAIL) trailCount++

        x += (velocityX * seconds).toFloat()
        y += (velocityY * seconds).toFloat()
    }

    fun reverseHorizontal() {
        velocityX = -velocityX
    }

    fun isOutside(width: Int, height: Int): Boolean =
        x < -radius || x > width + radius || y < -radius || y > height + radius

    fun overlaps(r: RectF): Boolean {
        val nearestX = x.coerceIn(r.left, r.right)
        val nearestY = y.coerceIn(r.top, r.bottom)
        val dx = x - nearestX
        val dy = y - nearestY
        return dx * dx + dy * dy <= radius * radius
    }

    override fun draw(canvas: Canvas) {
        if (!onScreen) return
        for (i in 0 until trailCount) {
            val f = 1f - i / TRAIL.toFloat()
            trailPaint.alpha = (f * 140).toInt()
            canvas.drawCircle(trailX[i], trailY[i], radius * f, trailPaint)
        }
        canvas.drawCircle(x, y, radius, paint)
    }
}