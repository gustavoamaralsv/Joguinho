package com.example.joguinho

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF

/** Bala do canhão: um círculo com velocidade em pixels por segundo. */
class Cannonball(val radius: Float) : GameElement() {
    var x = 0f
    var y = 0f
    var velocityX = 0f
    var velocityY = 0f
    var onScreen = false

    init {
        paint.color = Color.DKGRAY
    }

    fun launch(startX: Float, startY: Float, vx: Float, vy: Float) {
        x = startX
        y = startY
        velocityX = vx
        velocityY = vy
        onScreen = true
    }

    fun update(seconds: Double) {
        x += (velocityX * seconds).toFloat()
        y += (velocityY * seconds).toFloat()
    }

    fun reverseHorizontal() {
        velocityX = -velocityX
    }

    fun isOutside(width: Int, height: Int): Boolean =
        x < -radius || x > width + radius || y < -radius || y > height + radius

    /** Colisão círculo x retângulo. */
    fun overlaps(r: RectF): Boolean {
        val nearestX = x.coerceIn(r.left, r.right)
        val nearestY = y.coerceIn(r.top, r.bottom)
        val dx = x - nearestX
        val dy = y - nearestY
        return dx * dx + dy * dy <= radius * radius
    }

    override fun draw(canvas: Canvas) {
        if (onScreen) canvas.drawCircle(x, y, radius, paint)
    }
}
