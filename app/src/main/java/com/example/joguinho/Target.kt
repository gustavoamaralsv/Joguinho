package com.example.joguinho

import android.graphics.Canvas

/** Alvo: ao ser atingido, desaparece e o jogador ganha tempo. */
class Target(
    left: Float, top: Float, width: Float, height: Float,
    velocityY: Float, minY: Float, maxY: Float, color: Int
) : MovingBar(left, top, width, height, velocityY, minY, maxY, color) {

    var hit = false

    override fun draw(canvas: Canvas) {
        if (!hit) super.draw(canvas)
    }
}
