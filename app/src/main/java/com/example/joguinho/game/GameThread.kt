package com.example.joguinho.game

import android.graphics.Canvas
import android.view.SurfaceHolder

/** Game loop: atualiza posições e desenha a cada quadro. */
class GameThread(
    private val surfaceHolder: SurfaceHolder,
    private val view: CannonView,
) : Thread("CannonGameThread") {

    @Volatile
    var running = true

    override fun run() {
        var previous = System.nanoTime()
        while (running) {
            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                if (canvas != null) {
                    val now = System.nanoTime()
                    val elapsedSeconds = ((now - previous) / 1_000_000_000.0)
                        .coerceAtMost(GameConfig.MAX_DELTA_SECONDS)
                    previous = now
                    view.updatePositions(elapsedSeconds)
                    view.drawGameElements(canvas)
                }
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    } catch (_: Exception) {
                        // superfície já destruída
                    }
                }
            }
        }
    }
}
