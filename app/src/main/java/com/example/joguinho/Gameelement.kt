package com.example.joguinho

import android.graphics.Canvas
import android.graphics.Paint

/** Base de tudo que é desenhado no jogo. */
abstract class GameElement {
    protected val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    abstract fun draw(canvas: Canvas)
}
