package com.pratikmardi.galaxystrike

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.random.Random

class GameView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val stars = MutableList(100) {
        PointF(Random.nextFloat(), Random.nextFloat())
    }

    private val enemies = mutableListOf<Enemy>()
    private val bullets = mutableListOf<Bullet>()

    private var playerX = 0.5f

    private var score = 0
    private var health = 3

    private var gameOver = false
    private var dragging = false

    private var lastShot = 0L
    private var lastSpawn = 0L

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

    init {
        paint.typeface = Typeface.create("sans", Typeface.BOLD)
        isFocusable = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(Color.rgb(3, 6, 20))

        drawStars(canvas)

        val now = System.currentTimeMillis()

        if (!gameOver) {

            // Spawn enemies
            if (now - lastSpawn > 650) {

                enemies.add(
                    Enemy(
                        x = Random.nextFloat() * 0.86f + 0.07f,
                        y = -0.08f,
                        speed = 0.00022f +
                                Random.nextFloat() * 0.00018f,
                        size = 0.055f
                    )
                )

                lastSpawn = now
            }

            // Automatic shooting
            if (now - lastShot > 170) {

                bullets.add(
                    Bullet(
                        x = playerX,
                        y = 0.88f,
                        speed = 0.0012f
                    )
                )

                lastShot = now
            }

            updateGame()
        }

        drawEnemies(canvas)
        drawBullets(canvas)
        drawPlayer(canvas)
        drawHud(canvas)

        if (gameOver) {
            drawGameOver(canvas)
        }

        postInvalidateOnAnimation()
    }

    private fun drawStars(canvas: Canvas) {

        paint.color = Color.WHITE

        stars.forEachIndexed { index, star ->

            val movement =
                (System.currentTimeMillis() % 6000) /
                        6000f * 0.12f

            val y = ((star.y + movement) % 1f) * height

            val size =
                if (index % 5 == 0) 2.2f
                else 1.1f

            canvas.drawCircle(
                star.x * width,
                y,
                size,
                paint
            )
        }
    }

    private fun drawPlayer(canvas: Canvas) {

        val x = playerX * width
        val y = 0.88f * height

        // Spaceship
        paint.color = Color.CYAN

        val ship = Path()

        ship.moveTo(x, y - 42)

        ship.lineTo(
            x - 30,
            y + 30
        )

        ship.lineTo(
            x,
            y + 17
        )

        ship.lineTo(
            x + 30,
            y + 30
        )

        ship.close()

        canvas.drawPath(ship, paint)

        // Cockpit
        paint.color = Color.WHITE

        canvas.drawCircle(
            x,
            y - 4,
            7f,
            paint
        )

        // Engine flame
        paint.color = Color.rgb(
            255,
            150,
            30
        )

        canvas.drawCircle(
            x,
            y + 34,
            8f,
            paint
        )
    }

    private fun drawEnemies(canvas: Canvas) {

        enemies.forEach { enemy ->

            val x = enemy.x * width
            val y = enemy.y * height

            // Alien body
            paint.color = Color.rgb(
                210,
                70,
                255
            )

            canvas.drawCircle(
                x,
                y,
                enemy.size * width,
                paint
            )

            // Eyes
            paint.color = Color.BLACK

            canvas.drawCircle(
                x - 9,
                y - 3,
                4f,
                paint
            )

            canvas.drawCircle(
                x + 9,
                y - 3,
                4f,
                paint
            )

            // Alien mouth
            paint.color = Color.rgb(
                90,
                255,
                120
            )

            canvas.drawCircle(
                x,
                y + 10,
                5f,
                paint
            )
        }
    }

    private fun drawBullets(canvas: Canvas) {

        paint.color = Color.YELLOW

        bullets.forEach { bullet ->

            val x = bullet.x * width
            val y = bullet.y * height

            canvas.drawRoundRect(
                x - 3,
                y - 15,
                x + 3,
                y + 15,
                5f,
                5f,
                paint
            )
        }
    }

    private fun drawHud(canvas: Canvas) {

        paint.textSize = 28f
        paint.color = Color.WHITE

        canvas.drawText(
            "SCORE  $score",
            24f,
            42f,
            paint
        )

        paint.color = Color.rgb(
            255,
            90,
            90
        )

        canvas.drawText(
            "♥".repeat(health),
            width - 125f,
            42f,
            paint
        )
    }

    private fun drawGameOver(canvas: Canvas) {

        paint.color = Color.argb(
            210,
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

        paint.textAlign = Paint.Align.CENTER

        paint.color = Color.CYAN
        paint.textSize = 56f

        canvas.drawText(
            "GAME OVER",
            width / 2f,
            height / 2f - 50,
            paint
        )

        paint.color = Color.WHITE
        paint.textSize = 28f

        canvas.drawText(
            "Score: $score",
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

        paint.textAlign = Paint.Align.LEFT
    }

    private fun updateGame() {

        // Move bullets
        bullets.forEach {
            it.y -= it.speed * 16f
        }

        bullets.removeAll {
            it.y < -0.1f
        }

        // Move enemies
        enemies.forEach {
            it.y += it.speed * 16f
        }

        val destroyedEnemies =
            mutableSetOf<Enemy>()

        val usedBullets =
            mutableSetOf<Bullet>()

        for (enemy in enemies) {

            // Enemy reached player
            if (enemy.y > 0.93f) {

                destroyedEnemies.add(enemy)

                health--

                if (health <= 0) {
                    gameOver = true
                }

                continue
            }

            // Bullet collision
            for (bullet in bullets) {

                val dx =
                    enemy.x - bullet.x

                val dy =
                    enemy.y - bullet.y

                val distance =
                    dx * dx + dy * dy

                if (distance < 0.004f) {

                    destroyedEnemies.add(enemy)

                    usedBullets.add(bullet)

                    score += 10

                    break
                }
            }
        }

        enemies.removeAll(destroyedEnemies)

        bullets.removeAll(usedBullets)
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
