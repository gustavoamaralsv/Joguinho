package com.example.joguinho.game

/**
 * Detecção de colisão círculo x retângulo alinhado aos eixos.
 * Função pura (sem Android) para testes JVM.
 */
object Collision {

    data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)

    fun circleOverlapsRect(
        cx: Float,
        cy: Float,
        radius: Float,
        rect: Rect,
    ): Boolean {
        val nearestX = cx.coerceIn(rect.left, rect.right)
        val nearestY = cy.coerceIn(rect.top, rect.bottom)
        val dx = cx - nearestX
        val dy = cy - nearestY
        return dx * dx + dy * dy <= radius * radius
    }
}
