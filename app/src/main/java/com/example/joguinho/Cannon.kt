package com.example.joguinho

import android.graphics.Canvas
import android.graphics.Color
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

    /** Ângulo em radianos; 0 = apontando para a direita. */
    var angle = 0f
        private set

    val tipX: Float get() = baseX + barrelLength * cos(angle)
    val tipY: Float get() = baseY + barrelLength * sin(angle)

    init {
        paint.color = Color.BLACK
        paint.strokeWidth = barrelWidth
    }

    /** Aponta o cano para o ponto tocado (nunca para trás). */
    fun aimAt(touchX: Float, touchY: Float) {
        val dx = max(touchX - baseX, 1f)
        angle = atan2(touchY - baseY, dx)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawLine(baseX, baseY, tipX, tipY, paint)
        canvas.drawCircle(baseX, baseY, baseRadius, paint)
    }
}
