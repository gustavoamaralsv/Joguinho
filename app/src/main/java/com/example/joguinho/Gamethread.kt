package com.example.joguinho

import android.graphics.Canvas
import android.view.SurfaceHolder
import com.example.joguinho.CannonView

/** Game loop: atualiza posições e desenha a cada quadro. */
class GameThread(
    private val surfaceHolder: SurfaceHolder,
    private val view: CannonView
) : Thread("CannonGameThread") {

    @Volatile
    var running = true

    override fun run() {
        var previous = System.currentTimeMillis()
        while (running) {
            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                if (canvas != null) {
                    val now = System.currentTimeMillis()
                    val elapsedSeconds = (now - previous) / 1000.0
                    previous = now
                    view.updatePositions(elapsedSeconds)
                    view.drawGameElements(canvas)
                }
            } finally {
                if (canvas != null) surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }
    }
}