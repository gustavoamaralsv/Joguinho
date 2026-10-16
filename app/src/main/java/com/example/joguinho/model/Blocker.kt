package com.example.joguinho.model

import android.graphics.Color

/** Obstáculo: se a bala o atingir, o jogador perde tempo. */
class Blocker(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    velocityY: Float,
    minY: Float,
    maxY: Float,
) : MovingBar(left, top, width, height, velocityY, minY, maxY, Color.rgb(180, 30, 30))
