package com.example.joguinho

import com.example.joguinho.game.GameConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class GameConfigTest {

    @Test
    fun speedFactor_level1_isBase() {
        assertEquals(1f, GameConfig.speedFactor(1), 0.001f)
    }

    @Test
    fun speedFactor_growsPerLevel() {
        assertEquals(1.25f, GameConfig.speedFactor(2), 0.001f)
        assertEquals(1.5f, GameConfig.speedFactor(3), 0.001f)
    }

    @Test
    fun speedFactor_isCapped() {
        assertEquals(GameConfig.MAX_SPEED_FACTOR, GameConfig.speedFactor(100), 0.001f)
    }

    @Test
    fun pointsForHit_scalesWithLevel() {
        assertEquals(100, GameConfig.pointsForHit(1))
        assertEquals(300, GameConfig.pointsForHit(3))
    }

    @Test
    fun timeBarFraction_fullAtStart() {
        assertEquals(1f, GameConfig.timeBarFraction(GameConfig.START_TIME), 0.001f)
    }

    @Test
    fun timeBarFraction_halfAndZero() {
        assertEquals(0.5f, GameConfig.timeBarFraction(GameConfig.START_TIME / 2), 0.001f)
        assertEquals(0f, GameConfig.timeBarFraction(0.0), 0.001f)
    }

    @Test
    fun timeBarFraction_neverExceedsOne() {
        assertEquals(1f, GameConfig.timeBarFraction(GameConfig.START_TIME * 2), 0.001f)
    }
}
