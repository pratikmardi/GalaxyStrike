package com.pratikmardi.galaxystrike

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.random.Random

class GameView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var playerX = 0.5f
    private var score = 0
    private var health = 3

    private var gameOver = false
    private var dragging = false

    private var lastShot = 0L
    private var lastSpawn = 0L

    private val enemies = mutableListOf<Enemy>()
    private val bullets = mutableListOf<Bullet>()
    private val explosions = mutableListOf<Explosion>()

    private val stars = MutableList(120) {
        Star(
            Random.nextFloat(),
            Random.nextFloat(),
            Random.nextFloat() * 2f + 1f
        )
    }

    private var playerBitmap: Bitmap? = null
    private var alienBitmap: Bitmap? = null
    private var backgroundBitmap: Bitmap? = null

    data class Enemy(
        var x: Float,
        var y: Float,
        var speed: Float,
        var size: Float
    )

    data class Bullet(
        var x: Float,
        var y: Float,
        var speed: Float
    )

    data class Explosion(
        var x: Float,
        var y: Float,
        var life: Int = 20
    )

    data class Star(
        var x: Float,
        var y: Float,
        var speed: Float
    )

    init {
        loadAssets()
    }

    private fun loadAssets() {

        try {
            playerBitmap =
                BitmapFactory.decodeStream(
                    context.assets.open("player_ship.png")
                )
        } catch (_: Exception) {
            playerBitmap = null
        }

        try {
            alienBitmap =
                BitmapFactory.decodeStream(
                    context.assets.open("alien_ship.png")
                )
        } catch (_: Exception) {
            alienBitmap = null
        }

        try {
            backgroundBitmap =
                BitmapFactory.decodeStream(
                    context.assets.open("space_background.jpg")
                )
        } catch (_: Exception) {
            backgroundBitmap = null
        }
    }

    override fun onDraw(canvas: Canvas) {

        super.onDraw(canvas)

        drawBackground(canvas)
        updateGame()

        drawEnemies(canvas)
        drawBullets(canvas)
        drawExplosions(canvas)
        drawPlayer(canvas)
        drawHud(canvas)

        if (gameOver) {
            drawGameOver(canvas)
        }

        postInvalidateOnAnimation()
    }

    private fun drawBackground(canvas: Canvas) {

        if (backgroundBitmap != null) {

            val src = Rect(
                0,
                0,
                backgroundBitmap!!.width,
                backgroundBitmap!!.height
            )

            val dst = Rect(
                0,
                0,
                width,
                height
            )

            canvas.drawBitmap(
                backgroundBitmap!!,
                src,
                dst,
                paint
            )

        } else {

            canvas.drawColor(
                Color.rgb(3, 6, 20)
            )

            paint.color = Color.WHITE

            stars.forEach {

                it.y += it.speed * 0.001f

                if (it.y > 1f) {
                    it.y = 0f
                    it.x = Random.nextFloat()
                }

                canvas.drawCircle(
                    it.x * width,
                    it.y * height,
                    it.speed,
                    paint
                )
            }
        }
    }

    private fun drawPlayer(canvas: Canvas) {

        val x = playerX * width
        val y = height * 0.87f

        if (playerBitmap != null) {

            val size = width * 0.18f

            val dst = RectF(
                x - size / 2,
                y - size / 2,
                x + size / 2,
                y + size / 2
            )

            canvas.drawBitmap(
                playerBitmap!!,
                null,
                dst,
                paint
            )

        } else {

            paint.color = Color.CYAN

            val path = Path()

            path.moveTo(
                x,
                y - 45
            )

            path.lineTo(
                x - 32,
                y + 30
            )

            path.lineTo(
                x,
                y + 15
            )

            path.lineTo(
                x + 32,
                y + 30
            )

            path.close()

            canvas.drawPath(
                path,
                paint
            )
        }
    }

    private fun drawEnemies(canvas: Canvas) {

        enemies.forEach {

            val x = it.x * width
            val y = it.y * height

            if (alienBitmap != null) {

                val size =
                    it.size * width * 3f

                val dst = RectF(
                    x - size / 2,
                    y - size / 2,
                    x + size / 2,
                    y + size / 2
                )

                canvas.drawBitmap(
                    alienBitmap!!,
                    null,
                    dst,
                    paint
                )

            } else {

                paint.color =
                    Color.rgb(
                        210,
                        70,
                        255
                    )

                canvas.drawCircle(
                    x,
                    y,
                    it.size * width,
                    paint
                )

                paint.color = Color.BLACK

                canvas.drawCircle(
                    x - 8,
                    y - 3,
                    4f,
                    paint
                )

                canvas.drawCircle(
                    x + 8,
                    y - 3,
                    4f,
                    paint
                )
            }
        }
    }

    private fun drawBullets(canvas: Canvas) {

        paint.color = Color.YELLOW

        bullets.forEach {

            val x = it.x * width
            val y = it.y * height

            canvas.drawRoundRect(
                x - 4,
                y - 18,
                x + 4,
                y + 18,
                5f,
                5f,
                paint
            )
        }
    }

    private fun drawExplosions(canvas: Canvas) {

        explosions.forEach {

            val radius =
                (20 - it.life) * 5f

            paint.color =
                Color.argb(
                    (it.life * 10).coerceAtMost(255),
                    255,
                    140,
                    20
                )

            canvas.drawCircle(
                it.x * width,
                it.y * height,
                radius,
                paint
            )

            it.life--
        }

        explosions.removeAll {
            it.life <= 0
        }
    }

    private fun drawHud(canvas: Canvas) {

        paint.color = Color.WHITE
        paint.textSize = 28f
        paint.typeface =
            Typeface.DEFAULT_BOLD

        canvas.drawText(
            "SCORE  $score",
            24f,
            42f,
            paint
        )

        paint.color =
            Color.rgb(
                255,
                80,
                80
            )

        canvas.drawText(
            "♥".repeat(health),
            width - 125f,
            42f,
            paint
        )
    }

    private fun drawGameOver(canvas: Canvas) {

        paint.color =
            Color.argb(
                220,
                0,
                0,
                0
            )

        canvas.drawRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            paint
        )

        paint.textAlign =
            Paint.Align.CENTER

        paint.color = Color.CYAN
        paint.textSize = 55f

        canvas.drawText(
            "GAME OVER",
            width / 2f,
            height / 2f - 50,
            paint
        )

        paint.color = Color.WHITE
        paint.textSize = 28f

        canvas.drawText(
            "SCORE: $score",
            width / 2f,
            height / 2f + 5,
            paint
        )

        paint.color = Color.YELLOW

        canvas.drawText(
            "TAP TO RESTART",
            width / 2f,
            height / 2f + 65,
            paint
        )

        paint.textAlign =
            Paint.Align.LEFT
    }

    private fun updateGame() {

        val now =
            System.currentTimeMillis()

        if (!gameOver) {

            if (now - lastSpawn > 600) {

                enemies.add(
                    Enemy(
                        Random.nextFloat()
                            .coerceIn(
                                0.08f,
                                0.92f
                            ),
                        -0.08f,
                        0.00025f +
                                Random.nextFloat()
                                * 0.0002f,
                        0.06f
                    )
                )

                lastSpawn = now
            }

            if (now - lastShot > 160) {

                bullets.add(
                    Bullet(
                        playerX,
                        0.84f,
                        0.0015f
                    )
                )

                lastShot = now
            }
        }

        bullets.forEach {
            it.y -= it.speed * 16f
        }

        bullets.removeAll {
            it.y < -0.1f
        }

        enemies.forEach {
            it.y += it.speed * 16f
        }

        val deadEnemies =
            mutableSetOf<Enemy>()

        val usedBullets =
            mutableSetOf<Bullet>()

        enemies.forEach { enemy ->

            if (enemy.y > 0.94f) {

                deadEnemies.add(enemy)

                health--

                if (health <= 0) {
                    gameOver = true
                }

            } else {

                bullets.forEach { bullet ->

                    val distance =
                        abs(enemy.x - bullet.x) +
                                abs(enemy.y - bullet.y)

                    if (distance < 0.07f) {

                        deadEnemies.add(enemy)

                        usedBullets.add(bullet)

                        explosions.add(
                            Explosion(
                                enemy.x,
                                enemy.y
                            )
                        )

                        score += 10
                    }
                }
            }
        }

        enemies.removeAll(
            deadEnemies
        )

        bullets.removeAll(
            usedBullets
        )
    }

    override fun onTouchEvent(
        event: MotionEvent
    ): Boolean {

        when (event.action) {

            MotionEvent.ACTION_DOWN -> {

                if (gameOver) {

                    gameOver = false
                    score = 0
                    health = 3

                    enemies.clear()
                    bullets.clear()
                    explosions.clear()
                }

                dragging = true

                playerX =
                    (event.x / width)
                        .coerceIn(
                            0.08f,
                            0.92f
                        )

                return true
            }

            MotionEvent.ACTION_MOVE -> {

                if (dragging) {

                    playerX =
                        (event.x / width)
                            .coerceIn(
                                0.08f,
                                0.92f
                            )
                }

                return true
            }

            MotionEvent.ACTION_UP -> {

                dragging = false

                return true
            }
        }

        return true
    }
}
