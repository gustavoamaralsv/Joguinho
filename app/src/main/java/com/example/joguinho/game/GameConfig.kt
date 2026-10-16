package com.example.joguinho.game

import kotlin.math.min

/**
 * Constantes e regras puras do Cannon Game (Cap. 6 + extras).
 * Mantidas sem dependência de Android para facilitar testes unitários.
 */
object GameConfig {
    const val TARGET_COUNT = 7
    const val START_TIME = 20.0
    const val HIT_BONUS = 3.0
    const val BLOCKER_PENALTY = 2.0
    const val LEVEL_BONUS = 8.0
    const val POINTS_PER_TARGET = 100
    const val MAX_SPEED_FACTOR = 3f
    const val SPEED_GROWTH_PER_LEVEL = 0.25f
    const val BALL_SPEED_FACTOR = 1.5f
    const val MAX_DELTA_SECONDS = 1.0 / 30.0
    const val BALL_SUBSTEP_DISTANCE_FACTOR = 0.5f
    const val LEVEL_BANNER_SECONDS = 1.5

    /** Fator de velocidade do nível (1.0 no nível 1, limitado a [MAX_SPEED_FACTOR]). */
    fun speedFactor(level: Int): Float =
        min(1f + SPEED_GROWTH_PER_LEVEL * (level - 1).coerceAtLeast(0), MAX_SPEED_FACTOR)

    fun pointsForHit(level: Int): Int = POINTS_PER_TARGET * level.coerceAtLeast(1)

    /** Fração da barra de tempo (0..1) relativa ao tempo inicial. */
    fun timeBarFraction(timeLeft: Double): Float =
        min(timeLeft / START_TIME, 1.0).toFloat().coerceIn(0f, 1f)
}
