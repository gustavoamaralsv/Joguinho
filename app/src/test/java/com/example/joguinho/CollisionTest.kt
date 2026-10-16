package com.example.joguinho

import com.example.joguinho.game.Collision
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollisionTest {

    private val rect = Collision.Rect(10f, 10f, 30f, 40f)

    @Test
    fun circleInsideRect_overlaps() {
        assertTrue(Collision.circleOverlapsRect(20f, 25f, 5f, rect))
    }

    @Test
    fun circleTouchingLeftEdge_overlaps() {
        assertTrue(Collision.circleOverlapsRect(5f, 25f, 5f, rect))
    }

    @Test
    fun circleTouchingCorner_overlaps() {
        // Distância do canto (10,10) ao centro (7,7) = ~4.24 < 5
        assertTrue(Collision.circleOverlapsRect(7f, 7f, 5f, rect))
    }

    @Test
    fun circleNearButOutside_doesNotOverlap() {
        assertFalse(Collision.circleOverlapsRect(0f, 25f, 5f, rect))
    }

    @Test
    fun circleFarAway_doesNotOverlap() {
        assertFalse(Collision.circleOverlapsRect(100f, 100f, 5f, rect))
    }

    @Test
    fun zeroRadiusOnBoundary_overlaps() {
        assertTrue(Collision.circleOverlapsRect(10f, 25f, 0f, rect))
    }
}
