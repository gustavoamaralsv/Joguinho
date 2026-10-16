package com.example.joguinho.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.joguinho.R

/** Encapsula o SoundPool e os efeitos do Cannon Game. */
class SoundManager(context: Context) {

    enum class Sound {
        TARGET_HIT,
        BLOCKER_HIT,
        CANNON_FIRE,
        LEVEL_UP,
        GAME_OVER,
    }

    private var soundPool: SoundPool?
    private val soundIds = IntArray(Sound.values().size)

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val pool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(attributes)
            .build()
        soundIds[Sound.TARGET_HIT.ordinal] = pool.load(context, R.raw.target_hit, 1)
        soundIds[Sound.BLOCKER_HIT.ordinal] = pool.load(context, R.raw.blocker_hit, 1)
        soundIds[Sound.CANNON_FIRE.ordinal] = pool.load(context, R.raw.cannon_fire, 1)
        soundIds[Sound.LEVEL_UP.ordinal] = pool.load(context, R.raw.level_up, 1)
        soundIds[Sound.GAME_OVER.ordinal] = pool.load(context, R.raw.game_over, 1)
        soundPool = pool
    }

    fun play(sound: Sound) {
        soundPool?.play(soundIds[sound.ordinal], 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        soundPool?.release()
        soundPool = null
    }
}
