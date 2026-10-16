package com.example.joguinho.game

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.joguinho.R
import com.example.joguinho.audio.SoundManager
import com.example.joguinho.data.HighScoreRepository
import com.example.joguinho.model.Blocker
import com.example.joguinho.model.Cannon
import com.example.joguinho.model.Cannonball
import com.example.joguinho.model.Particle
import com.example.joguinho.model.Target
import com.example.joguinho.ui.GameOverDialogFragment
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class CannonView(context: Context, attrs: AttributeSet?) :
    SurfaceView(context, attrs), SurfaceHolder.Callback {

    private val activity = context as Activity
    private val highScores = HighScoreRepository(context)
    private val soundManager = SoundManager(context)

    private var thread: GameThread? = null
    private var dialogIsDisplayed = false
    private var gameOver = false
    private var surfaceReady = false

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
    private var highScore = highScores.highScore
    private var bannerTime = 0.0
    private var cloudOffset = 0f

    private val skyPaint = Paint()
    private val hillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hillFarPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(170, 255, 255, 255)
    }
    private val barBackPaint = Paint().apply { color = Color.argb(90, 0, 0, 0) }
    private val barPaint = Paint()
    private val hillPath = Path()

    private val textColor = ContextCompat.getColor(context, R.color.hud_text)
    private val colorSkyTop = ContextCompat.getColor(context, R.color.sky_top)
    private val colorSkyBottom = ContextCompat.getColor(context, R.color.sky_bottom)
    private val colorTargetBlue = ContextCompat.getColor(context, R.color.target_blue)
    private val colorTargetGold = ContextCompat.getColor(context, R.color.target_gold)
    private val colorBlocker = ContextCompat.getColor(context, R.color.blocker_red)
    private val colorBarOk = ContextCompat.getColor(context, R.color.time_bar_ok)
    private val colorBarLow = ContextCompat.getColor(context, R.color.time_bar_low)
    private val titlePaint = makeTextPaint(Paint.Align.LEFT)
    private val titleRightPaint = makeTextPaint(Paint.Align.RIGHT)
    private val smallPaint = makeTextPaint(Paint.Align.LEFT)
    private val smallRightPaint = makeTextPaint(Paint.Align.RIGHT)
    private val bannerPaint = makeTextPaint(Paint.Align.CENTER).apply {
        color = ContextCompat.getColor(context, R.color.banner_orange)
    }

    // Cache de strings do HUD para evitar alocação a cada quadro
    private var hudTimeText = ""
    private var hudScoreText = ""
    private var hudLevelText = ""
    private var hudRecordText = ""
    private var lastHudTimeTenths = -1
    private var lastHudScore = -1
    private var lastHudLevel = -1
    private var lastHudRecord = -1

    private fun makeTextPaint(align: Paint.Align) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textAlign = align
        isFakeBoldText = true
    }

    init {
        holder.addCallback(this)
        hillPaint.color = ContextCompat.getColor(context, R.color.hill_near)
        hillFarPaint.color = ContextCompat.getColor(context, R.color.hill_far)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val sizeChanged = w != screenWidth || h != screenHeight
        screenWidth = w
        screenHeight = h
        titlePaint.textSize = w / 22f
        titleRightPaint.textSize = w / 22f
        smallPaint.textSize = w / 34f
        smallRightPaint.textSize = w / 34f
        bannerPaint.textSize = w / 9f
        skyPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            colorSkyTop, colorSkyBottom, Shader.TileMode.CLAMP,
        )
        if (sizeChanged && w > 0 && h > 0) {
            // Só reinicia quando a superfície ganha tamanho real (ou muda de fato)
            if (oldw == 0 || oldh == 0 || !::cannon.isInitialized) {
                newGame()
            }
        }
    }

    /** Cria bloqueador e alvos do nível atual (mais rápidos a cada nível). */
    private fun createLevel() {
        val w = screenWidth.toFloat()
        val h = screenHeight.toFloat()
        val speedFactor = GameConfig.speedFactor(level)

        blocker = Blocker(
            w * 5f / 8f, h / 8f, w / 50f, h / 4f,
            h / 2f * speedFactor, 0f, h,
        )

        targets.clear()
        val slot = (h * 0.9f) / GameConfig.TARGET_COUNT
        for (i in 0 until GameConfig.TARGET_COUNT) {
            val speed = (if (i % 2 == 0) 1 else -1) * h * (0.15f + 0.03f * i) * speedFactor
            val color = if (i % 2 == 0) colorTargetBlue else colorTargetGold
            targets.add(
                Target(
                    w * 7f / 8f, h * 0.05f + i * slot, w / 40f, slot * 0.7f,
                    speed, 0f, h, color,
                ),
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
        timeLeft = GameConfig.START_TIME
        totalElapsed = 0.0
        shotsFired = 0
        targetsHit = 0
        bannerTime = 0.0
        particles.clear()
        highScore = highScores.highScore
        invalidateHudCache()

        ball = Cannonball(h / 36f)
        createLevel()
        cannon = Cannon(h / 2f, h / 18f, w / 8f, h / 36f)

        if (gameOver) {
            gameOver = false
        }
        if (surfaceReady && (thread == null || thread?.running != true) && !dialogIsDisplayed) {
            startThread()
        }
    }

    @Synchronized
    fun updatePositions(dt: Double) {
        if (!::cannon.isInitialized || gameOver) return

        cloudOffset += (screenWidth * 0.02f * dt).toFloat()
        if (bannerTime > 0) bannerTime -= dt

        if (ball.onScreen) {
            updateBallWithSubsteps(dt)
        }

        blocker.update(dt)
        for (t in targets) t.update(dt)

        for (p in particles) p.update(dt)
        particles.removeAll { !it.alive }

        timeLeft -= dt
        totalElapsed += dt
        refreshHudTexts()

        if (targetsHit == GameConfig.TARGET_COUNT) {
            level++
            targetsHit = 0
            timeLeft += GameConfig.LEVEL_BONUS
            bannerTime = GameConfig.LEVEL_BANNER_SECONDS
            createLevel()
            soundManager.play(SoundManager.Sound.LEVEL_UP)
            refreshHudTexts()
        } else if (timeLeft <= 0) {
            timeLeft = 0.0
            refreshHudTexts()
            endGame()
        }
    }

    /** Subdivide o movimento da bala para não atravessar alvos finos em um único passo. */
    private fun updateBallWithSubsteps(dt: Double) {
        val speed = hypot(ball.velocityX.toDouble(), ball.velocityY.toDouble())
        val maxStep = (ball.radius * GameConfig.BALL_SUBSTEP_DISTANCE_FACTOR).toDouble()
            .coerceAtLeast(1.0)
        val steps = if (speed <= 0) 1 else maxOf(1, kotlin.math.ceil(speed * dt / maxStep).toInt())
        val stepDt = dt / steps

        repeat(steps) {
            if (!ball.onScreen) return
            ball.update(stepDt)
            if (ball.overlaps(blocker.rect) && ball.velocityX > 0) {
                ball.reverseHorizontal()
                timeLeft -= GameConfig.BLOCKER_PENALTY
                explode(ball.x, ball.y, colorBlocker, 8)
                soundManager.play(SoundManager.Sound.BLOCKER_HIT)
                return
            }
            if (ball.isOutside(screenWidth, screenHeight)) {
                ball.onScreen = false
                return
            }
            for (t in targets) {
                if (!t.hit && ball.overlaps(t.rect)) {
                    t.hit = true
                    ball.onScreen = false
                    targetsHit++
                    score += GameConfig.pointsForHit(level)
                    timeLeft += GameConfig.HIT_BONUS
                    explode(t.rect.centerX(), t.rect.centerY(), t.color, 22)
                    soundManager.play(SoundManager.Sound.TARGET_HIT)
                    return
                }
            }
        }
    }

    @Synchronized
    fun drawGameElements(canvas: Canvas) {
        if (!::cannon.isInitialized) return
        val w = screenWidth.toFloat()
        val h = screenHeight.toFloat()

        canvas.drawRect(0f, 0f, w, h, skyPaint)
        drawHills(canvas, w, h)
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
            bannerPaint.alpha =
                (min(bannerTime / GameConfig.LEVEL_BANNER_SECONDS, 1.0) * 255).toInt()
            canvas.drawText(
                activity.getString(R.string.level_banner, level),
                w / 2f,
                h / 2f,
                bannerPaint,
            )
        }
    }

    private fun drawHills(canvas: Canvas, w: Float, h: Float) {
        hillPath.reset()
        hillPath.moveTo(0f, h)
        hillPath.quadTo(w * 0.2f, h * 0.55f, w * 0.45f, h * 0.72f)
        hillPath.quadTo(w * 0.7f, h * 0.9f, w, h * 0.65f)
        hillPath.lineTo(w, h)
        hillPath.close()
        canvas.drawPath(hillPath, hillFarPaint)

        hillPath.reset()
        hillPath.moveTo(0f, h)
        hillPath.quadTo(w * 0.25f, h * 0.7f, w * 0.5f, h * 0.82f)
        hillPath.quadTo(w * 0.8f, h * 0.95f, w, h * 0.78f)
        hillPath.lineTo(w, h)
        hillPath.close()
        canvas.drawPath(hillPath, hillPaint)
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

        canvas.drawText(hudTimeText, pad, line1, titlePaint)
        canvas.drawText(hudScoreText, w - pad, line1, titleRightPaint)
        canvas.drawText(hudLevelText, pad, line2, smallPaint)
        canvas.drawText(hudRecordText, w - pad, line2, smallRightPaint)

        val barW = w * 0.3f
        val barTop = line2 + smallPaint.textSize * 0.5f
        val barH = smallPaint.textSize * 0.5f
        val fraction = GameConfig.timeBarFraction(timeLeft)
        barPaint.color = if (fraction < 0.25f) colorBarLow else colorBarOk
        canvas.drawRect(pad, barTop, pad + barW, barTop + barH, barBackPaint)
        canvas.drawRect(pad, barTop, pad + barW * fraction, barTop + barH, barPaint)
    }

    private fun invalidateHudCache() {
        lastHudTimeTenths = -1
        lastHudScore = -1
        lastHudLevel = -1
        lastHudRecord = -1
        refreshHudTexts()
    }

    private fun refreshHudTexts() {
        val tenths = (timeLeft * 10).toInt()
        if (tenths != lastHudTimeTenths) {
            lastHudTimeTenths = tenths
            hudTimeText = activity.getString(R.string.hud_time, timeLeft)
        }
        if (score != lastHudScore) {
            lastHudScore = score
            hudScoreText = activity.getString(R.string.hud_score, score)
        }
        if (level != lastHudLevel) {
            lastHudLevel = level
            hudLevelText = activity.getString(R.string.hud_level, level)
        }
        if (highScore != lastHudRecord) {
            lastHudRecord = highScore
            hudRecordText = activity.getString(R.string.hud_record, highScore)
        }
    }

    private fun explode(cx: Float, cy: Float, color: Int, count: Int) {
        repeat(count) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = screenWidth * (0.08f + Random.nextFloat() * 0.3f)
            particles.add(
                Particle(
                    cx, cy, speed * cos(angle), speed * sin(angle),
                    color, screenHeight / 90f, screenHeight * 0.8f,
                ),
            )
        }
    }

    private fun endGame() {
        if (gameOver) return
        gameOver = true
        thread?.running = false

        val newRecord = highScores.updateIfHigher(score)
        if (newRecord) highScore = highScores.highScore

        val message = activity.getString(
            R.string.results_format,
            score,
            level,
            highScore,
            shotsFired,
            totalElapsed,
        )
        soundManager.play(SoundManager.Sound.GAME_OVER)

        activity.runOnUiThread {
            dialogIsDisplayed = true
            val host = activity as? FragmentActivity ?: return@runOnUiThread
            if (host.isFinishing || host.isDestroyed) return@runOnUiThread
            val existing = host.supportFragmentManager.findFragmentByTag(GameOverDialogFragment.TAG)
            if (existing != null) return@runOnUiThread
            GameOverDialogFragment.newInstance(newRecord, message)
                .show(host.supportFragmentManager, GameOverDialogFragment.TAG)
        }
    }

    fun onPlayAgain() {
        dialogIsDisplayed = false
        newGame()
    }

    @Synchronized
    private fun aimAndFire(x: Float, y: Float) {
        if (!::cannon.isInitialized || gameOver) return
        cannon.aimAt(x, y)
        if (!ball.onScreen) {
            val speed = screenWidth * GameConfig.BALL_SPEED_FACTOR
            val angle = cannon.angle
            ball.launch(cannon.tipX, cannon.tipY, speed * cos(angle), speed * sin(angle))
            shotsFired++
            soundManager.play(SoundManager.Sound.CANNON_FIRE)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> aimAndFire(e.x, e.y)
        }
        return true
    }

    fun releaseResources() {
        soundManager.release()
    }

    fun pause() {
        stopThread()
    }

    fun resumeGame() {
        if (surfaceReady && !dialogIsDisplayed && !gameOver) {
            startThread()
        }
    }

    private fun startThread() {
        if (thread?.isAlive == true) return
        thread = GameThread(holder, this).also {
            it.running = true
            it.start()
        }
    }

    private fun stopThread() {
        val t = thread ?: return
        t.running = false
        var retry = true
        while (retry) {
            try {
                t.join(500)
                retry = false
            } catch (_: InterruptedException) {
                // tenta novamente
            }
        }
        thread = null
    }

    // ---- SurfaceHolder.Callback ----

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceReady = true
        if (!dialogIsDisplayed && !gameOver) {
            startThread()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceReady = false
        stopThread()
    }
}
