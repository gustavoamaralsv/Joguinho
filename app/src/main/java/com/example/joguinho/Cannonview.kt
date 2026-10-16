package com.example.joguinho

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class CannonView(context: Context, attrs: AttributeSet?) :
    SurfaceView(context, attrs), SurfaceHolder.Callback {

    companion object {
        private const val TARGET_COUNT = 7
        private const val START_TIME = 20.0       // segundos (no livro: 10)
        private const val HIT_BONUS = 3.0         // ganho por alvo
        private const val BLOCKER_PENALTY = 2.0   // perda ao bater no bloqueador
        private const val LEVEL_BONUS = 8.0       // tempo extra ao passar de nível
        private const val POINTS_PER_TARGET = 100

        private const val SOUND_TARGET = 0
        private const val SOUND_BLOCKER = 1
        private const val SOUND_FIRE = 2
        private const val SOUND_LEVEL = 3
    }

    private val activity = context as Activity
    private val prefs = context.getSharedPreferences("cannon_prefs", Context.MODE_PRIVATE)

    private var thread: GameThread? = null
    private var dialogIsDisplayed = false
    private var gameOver = false

    private lateinit var cannon: Cannon
    private lateinit var ball: Cannonball
    private lateinit var blocker: Blocker
    private val targets = mutableListOf<Target>()
    private val particles = mutableListOf<Particle>()

    private var screenWidth = 0
    private var screenHeight = 0
    private var timeLeft = 0.0
    private var totalElapsed = 0.0
    private var shotsFired = 0
    private var targetsHit = 0
    private var level = 1
    private var score = 0
    private var highScore = prefs.getInt("high_score", 0)
    private var bannerTime = 0.0
    private var cloudOffset = 0f

    private var soundPool: SoundPool? = null
    private val sounds = IntArray(4)

    private val skyPaint = Paint()
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(170, 255, 255, 255) }
    private val barBackPaint = Paint().apply { color = Color.argb(90, 0, 0, 0) }
    private val barPaint = Paint()

    private val textColor = Color.rgb(20, 30, 70)
    private val titlePaint = makeTextPaint(Paint.Align.LEFT)
    private val titleRightPaint = makeTextPaint(Paint.Align.RIGHT)
    private val smallPaint = makeTextPaint(Paint.Align.LEFT)
    private val smallRightPaint = makeTextPaint(Paint.Align.RIGHT)
    private val bannerPaint = makeTextPaint(Paint.Align.CENTER).apply { color = Color.rgb(255, 120, 0) }

    private fun makeTextPaint(align: Paint.Align) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textAlign = align
        isFakeBoldText = true
    }

    init {
        holder.addCallback(this)

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val pool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attributes)
            .build()
        sounds[SOUND_TARGET] = pool.load(context, R.raw.target_hit, 1)
        sounds[SOUND_BLOCKER] = pool.load(context, R.raw.blocker_hit, 1)
        sounds[SOUND_FIRE] = pool.load(context, R.raw.cannon_fire, 1)
        sounds[SOUND_LEVEL] = pool.load(context, R.raw.level_up, 1)
        soundPool = pool
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w
        screenHeight = h
        titlePaint.textSize = w / 22f
        titleRightPaint.textSize = w / 22f
        smallPaint.textSize = w / 34f
        smallRightPaint.textSize = w / 34f
        bannerPaint.textSize = w / 9f
        skyPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            Color.rgb(110, 180, 245), Color.rgb(225, 245, 255), Shader.TileMode.CLAMP
        )
        newGame()
    }

    /** Cria bloqueador e alvos do nível atual (mais rápidos a cada nível). */
    private fun createLevel() {
        val w = screenWidth.toFloat()
        val h = screenHeight.toFloat()
        val speedFactor = min(1f + 0.25f * (level - 1), 3f)

        blocker = Blocker(w * 5f / 8f, h / 8f, w / 50f, h / 4f, h / 2f * speedFactor, 0f, h)

        targets.clear()
        val slot = (h * 0.9f) / TARGET_COUNT
        for (i in 0 until TARGET_COUNT) {
            val speed = (if (i % 2 == 0) 1 else -1) * h * (0.15f + 0.03f * i) * speedFactor
            val color = if (i % 2 == 0) Color.rgb(30, 90, 200) else Color.rgb(240, 180, 20)
            targets.add(
                Target(w * 7f / 8f, h * 0.05f + i * slot, w / 40f, slot * 0.7f, speed, 0f, h, color)
            )
        }
    }

    @Synchronized
    fun newGame() {
        if (screenWidth == 0 || screenHeight == 0) return

        val h = screenHeight.toFloat()
        val w = screenWidth.toFloat()

        level = 1
        score = 0
        timeLeft = START_TIME
        totalElapsed = 0.0
        shotsFired = 0
        targetsHit = 0
        bannerTime = 0.0
        particles.clear()

        ball = Cannonball(h / 36f)
        createLevel()
        cannon = Cannon(h / 2f, h / 18f, w / 8f, h / 36f)

        if (gameOver) {
            gameOver = false
            thread = GameThread(holder, this).also { it.start() }
        }
    }

    @Synchronized
    fun updatePositions(dt: Double) {
        if (!::cannon.isInitialized) return

        cloudOffset += (screenWidth * 0.02f * dt).toFloat()
        if (bannerTime > 0) bannerTime -= dt

        if (ball.onScreen) {
            ball.update(dt)
            if (ball.overlaps(blocker.rect) && ball.velocityX > 0) {
                ball.reverseHorizontal()
                timeLeft -= BLOCKER_PENALTY
                explode(ball.x, ball.y, Color.rgb(180, 30, 30), 8)
                play(SOUND_BLOCKER)
            } else if (ball.isOutside(screenWidth, screenHeight)) {
                ball.onScreen = false
            } else {
                for (t in targets) {
                    if (!t.hit && ball.overlaps(t.rect)) {
                        t.hit = true
                        ball.onScreen = false
                        targetsHit++
                        score += POINTS_PER_TARGET * level
                        timeLeft += HIT_BONUS
                        explode(t.rect.centerX(), t.rect.centerY(), t.color, 22)
                        play(SOUND_TARGET)
                        break
                    }
                }
            }
        }

        blocker.update(dt)
        for (t in targets) t.update(dt)

        for (p in particles) p.update(dt)
        particles.removeAll { !it.alive }

        timeLeft -= dt
        totalElapsed += dt

        if (targetsHit == TARGET_COUNT) {
            level++
            targetsHit = 0
            timeLeft += LEVEL_BONUS
            bannerTime = 1.5
            createLevel()
            play(SOUND_LEVEL)
        } else if (timeLeft <= 0) {
            timeLeft = 0.0
            endGame()
        }
    }

    @Synchronized
    fun drawGameElements(canvas: Canvas) {
        if (!::cannon.isInitialized) return
        val w = screenWidth.toFloat()
        val h = screenHeight.toFloat()

        canvas.drawRect(0f, 0f, w, h, skyPaint)
        drawCloud(canvas, 0.15f, 0.2f, h / 14f)
        drawCloud(canvas, 0.50f, 0.75f, h / 11f)
        drawCloud(canvas, 0.80f, 0.45f, h / 16f)

        drawHud(canvas)

        for (t in targets) t.draw(canvas)
        blocker.draw(canvas)
        cannon.draw(canvas)
        ball.draw(canvas)
        for (p in particles) p.draw(canvas)

        if (bannerTime > 0) {
            bannerPaint.alpha = (min(bannerTime / 1.5, 1.0) * 255).toInt()
            canvas.drawText(activity.getString(R.string.level_banner, level), w / 2f, h / 2f, bannerPaint)
        }
    }

    private fun drawCloud(canvas: Canvas, fx: Float, fy: Float, r: Float) {
        val x = ((screenWidth * fx + cloudOffset) % (screenWidth + 4 * r)) - 2 * r
        val y = screenHeight * fy
        canvas.drawCircle(x, y, r, cloudPaint)
        canvas.drawCircle(x + r, y + r / 4, r * 0.8f, cloudPaint)
        canvas.drawCircle(x - r, y + r / 4, r * 0.7f, cloudPaint)
    }

    private fun drawHud(canvas: Canvas) {
        val w = screenWidth.toFloat()
        val pad = w / 40f
        val line1 = titlePaint.textSize + pad / 2
        val line2 = line1 + smallPaint.textSize * 1.4f

        canvas.drawText(activity.getString(R.string.hud_time, timeLeft), pad, line1, titlePaint)
        canvas.drawText(activity.getString(R.string.hud_score, score), w - pad, line1, titleRightPaint)
        canvas.drawText(activity.getString(R.string.hud_level, level), pad, line2, smallPaint)
        canvas.drawText(activity.getString(R.string.hud_record, highScore), w - pad, line2, smallRightPaint)

        // barra de tempo
        val barW = w * 0.3f
        val barTop = line2 + smallPaint.textSize * 0.5f
        val barH = smallPaint.textSize * 0.5f
        val fraction = min(timeLeft / START_TIME, 1.0).toFloat()
        barPaint.color = if (fraction < 0.25f) Color.rgb(210, 40, 40) else Color.rgb(50, 170, 70)
        canvas.drawRect(pad, barTop, pad + barW, barTop + barH, barBackPaint)
        canvas.drawRect(pad, barTop, pad + barW * fraction, barTop + barH, barPaint)
    }

    private fun explode(cx: Float, cy: Float, color: Int, count: Int) {
        repeat(count) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = screenWidth * (0.08f + Random.nextFloat() * 0.3f)
            particles.add(
                Particle(
                    cx, cy, speed * cos(angle), speed * sin(angle),
                    color, screenHeight / 90f, screenHeight * 0.8f
                )
            )
        }
    }

    private fun endGame() {
        gameOver = true
        thread?.running = false

        val newRecord = score > highScore
        if (newRecord) {
            highScore = score
            prefs.edit().putInt("high_score", highScore).apply()
        }
        val finalScore = score
        val finalLevel = level
        val record = highScore
        val shots = shotsFired
        val total = totalElapsed

        activity.runOnUiThread {
            dialogIsDisplayed = true
            AlertDialog.Builder(activity)
                .setTitle(if (newRecord) R.string.new_record else R.string.game_over)
                .setMessage(
                    activity.getString(R.string.results_format, finalScore, finalLevel, record, shots, total)
                )
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
            val speed = screenWidth * 1.5f
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