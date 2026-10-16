package com.example.joguinho

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.max

/** Partícula de explosão: cai com gravidade e some aos poucos. */
class Particle(
    private var x: Float,
    private var y: Float,
    private var vx: Float,
    private var vy: Float,
    private val color: Int,
    private val radius: Float,
    private val gravity: Float
) {
    private var life = 1f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val alive: Boolean get() = life > 0f

    fun update(dt: Double) {
        x += (vx * dt).toFloat()
        y += (vy * dt).toFloat()
        vy += (gravity * dt).toFloat()
        life -= (dt * 1.6).toFloat()
    }

    fun draw(canvas: Canvas) {
        if (!alive) return
        paint.color = color
        paint.alpha = (life * 255).toInt().coerceIn(0, 255)
        canvas.drawCircle(x, y, radius * max(life, 0.25f), paint)
    }
}

