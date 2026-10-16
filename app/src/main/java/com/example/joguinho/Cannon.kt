package com.example.joguinho

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Canhão fixo na borda esquerda, no meio da tela. O cano gira conforme o toque. */
class Cannon(
    private val baseY: Float,
    private val baseRadius: Float,
    private val barrelLength: Float,
    barrelWidth: Float
) : GameElement() {

    private val baseX = 0f
    private val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(150, 150, 165) }

    var angle = 0f
        private set

    val tipX: Float get() = baseX + barrelLength * cos(angle)
    val tipY: Float get() = baseY + barrelLength * sin(angle)

    init {
        paint.color = Color.rgb(50, 50, 60)
        paint.strokeWidth = barrelWidth
        paint.strokeCap = Paint.Cap.ROUND
    }

    fun aimAt(touchX: Float, touchY: Float) {
        val dx = max(touchX - baseX, 1f)
        angle = atan2(touchY - baseY, dx)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawLine(baseX, baseY, tipX, tipY, paint)
        canvas.drawCircle(baseX, baseY, baseRadius, paint)
        canvas.drawCircle(baseX, baseY, baseRadius * 0.45f, hubPaint)
    }
}