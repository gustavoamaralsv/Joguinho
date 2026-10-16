package com.example.joguinho.data

import android.content.Context

/** Persistência do recorde local via SharedPreferences. */
class HighScoreRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var highScore: Int
        get() = prefs.getInt(KEY_HIGH_SCORE, 0)
        set(value) {
            prefs.edit().putInt(KEY_HIGH_SCORE, value).apply()
        }

    /** Atualiza o recorde se [score] for maior; retorna true quando há novo recorde. */
    fun updateIfHigher(score: Int): Boolean {
        if (score > highScore) {
            highScore = score
            return true
        }
        return false
    }

    companion object {
        private const val PREFS_NAME = "cannon_prefs"
        private const val KEY_HIGH_SCORE = "high_score"
    }
}
