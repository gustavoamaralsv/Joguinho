package com.example.joguinho

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.joguinho.Blocker
import com.example.joguinho.Cannon
import com.example.joguinho.Cannonball
import com.example.joguinho.GameThread
import kotlin.math.cos
import kotlin.math.sin

class CannonView(context: Context, attrs: AttributeSet?) :
    SurfaceView(context, attrs), SurfaceHolder.Callback {

    companion object {
        private const val TARGET_COUNT = 7
        private const val START_TIME = 10.0      // segundos
        private const val HIT_BONUS = 3.0        // ganho por alvo
        private const val BLOCKER_PENALTY = 2.0  // perda ao bater no bloqueador

        private const val SOUND_TARGET = 0
        private const val SOUND_BLOCKER = 1
        private const val SOUND_FIRE = 2
    }

    private val activity = context as Activity
    private var thread: GameThread? = null
    private var dialogIsDisplayed = false
    private var gameOver = false

    private lateinit var cannon: Cannon
    private lateinit var ball: Cannonball
    private lateinit var blocker: Blocker
    private val targets = mutableListOf<Target>()

    private var screenWidth = 0
    private var screenHeight = 0
    private var timeLeft = 0.0
    private var totalElapsed = 0.0
    private var shotsFired = 0
    private var targetsHit = 0

    private var soundPool: SoundPool? = null
    private val sounds = IntArray(3)

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }

    init {
        holder.addCallback(this)

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val pool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(attributes)
            .build()
        sounds[SOUND_TARGET] = pool.load(context, R.raw.target_hit, 1)
        sounds[SOUND_BLOCKER] = pool.load(context, R.raw.blocker_hit, 1)
        sounds[SOUND_FIRE] = pool.load(context, R.raw.cannon_fire, 1)
        soundPool = pool
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w
        screenHeight = h
        textPaint.textSize = w / 22f
        newGame()
    }

    @Synchronized
    fun newGame() {
        if (screenWidth == 0 || screenHeight == 0) return

        val w = screenWidth.toFloat()
        val h = screenHeight.toFloat()

        cannon = Cannon(h / 2f, h / 18f, w / 8f, h / 36f)
        ball = Cannonball(h / 36f)
        blocker = Blocker(w * 5f / 8f, h / 8f, w / 50f, h / 4f, h / 2f, 0f, h)

        targets.clear()
        val slot = (h * 0.9f) / TARGET_COUNT
        for (i in 0 until TARGET_COUNT) {
            val speed = (if (i % 2 == 0) 1 else -1) * h * (0.15f + 0.03f * i)
            val color = if (i % 2 == 0) Color.rgb(30, 90, 200) else Color.rgb(240, 180, 20)
            targets.add(
                Target(w * 7f / 8f, h * 0.05f + i * slot, w / 40f, slot * 0.7f, speed, 0f, h, color)
            )
        }

        timeLeft = START_TIME
        totalElapsed = 0.0
        shotsFired = 0
        targetsHit = 0

        if (gameOver) { // reinício depois de fim de jogo: novo game loop
            gameOver = false
            thread = GameThread(holder, this).also { it.start() }
        }
    }

    @Synchronized
    fun updatePositions(dt: Double) {
        if (!::cannon.isInitialized) return

        if (ball.onScreen) {
            ball.update(dt)
            if (ball.overlaps(blocker.rect) && ball.velocityX > 0) {
                ball.reverseHorizontal()
                timeLeft -= BLOCKER_PENALTY
                play(SOUND_BLOCKER)
            } else if (ball.isOutside(screenWidth, screenHeight)) {
                ball.onScreen = false
            } else {
                for (t in targets) {
                    if (!t.hit && ball.overlaps(t.rect)) {
                        t.hit = true
                        ball.onScreen = false
                        targetsHit++
                        timeLeft += HIT_BONUS
                        play(SOUND_TARGET)
                        break
                    }
                }
            }
        }

        blocker.update(dt)
        for (t in targets) t.update(dt)

        timeLeft -= dt
        totalElapsed += dt

        if (targetsHit == TARGET_COUNT) {
            endGame(R.string.win)
        } else if (timeLeft <= 0) {
            timeLeft = 0.0
            endGame(R.string.lose)
        }
    }

    @Synchronized
    fun drawGameElements(canvas: Canvas) {
        if (!::cannon.isInitialized) return
        canvas.drawColor(Color.WHITE)
        canvas.drawText(
            activity.getString(R.string.time_remaining, timeLeft),
            30f, textPaint.textSize + 10f, textPaint
        )
        cannon.draw(canvas)
        ball.draw(canvas)
        blocker.draw(canvas)
        for (t in targets) t.draw(canvas)
    }

    private fun endGame(titleId: Int) {
        gameOver = true
        thread?.running = false
        activity.runOnUiThread {
            dialogIsDisplayed = true
            AlertDialog.Builder(activity)
                .setTitle(titleId)
                .setMessage(activity.getString(R.string.results_format, shotsFired, totalElapsed))
                .setCancelable(false)
                .setPositiveButton(R.string.reset_game) { _, _ ->
                    dialogIsDisplayed = false
                    newGame()
                }
                .show()
        }
    }

    @Synchronized
    private fun aimAndFire(x: Float, y: Float) {
        if (!::cannon.isInitialized || gameOver) return
        cannon.aimAt(x, y)
        if (!ball.onScreen) {
            val speed = screenWidth * 1.5f // pixels por segundo
            val angle = cannon.angle
            ball.launch(cannon.tipX, cannon.tipY, speed * cos(angle), speed * sin(angle))
            shotsFired++
            play(SOUND_FIRE)
        }
    }

    private fun play(key: Int) {
        soundPool?.play(sounds[key], 1f, 1f, 1, 0, 1f)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> aimAndFire(e.x, e.y)
        }
        return true
    }

    fun releaseResources() {
        soundPool?.release()
        soundPool = null
    }

    // ---- SurfaceHolder.Callback ----

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (!dialogIsDisplayed) {
            thread = GameThread(holder, this).also { it.start() }
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        val t = thread ?: return
        t.running = false
        var retry = true
        while (retry) {
            try {
                t.join()
                retry = false
            } catch (e: InterruptedException) {
                // tenta novamente
            }
        }
    }
}