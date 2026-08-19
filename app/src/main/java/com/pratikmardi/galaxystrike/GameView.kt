package com.pratikmardi.galaxystrike

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class GameView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var playerX = 0f
    private var playerY = 0f

    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private var score = 0
    private var lives = 3

    private var gameOver = false

    private val bullets = mutableListOf<Bullet>()
    private val enemies = mutableListOf<Enemy>()
    private val stars = mutableListOf<Star>()
    private val explosions = mutableListOf<Explosion>()

    private var lastEnemyTime = 0L
    private var lastBulletTime = 0L

    private val random = java.util.Random()

    init {
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        for (i in 0 until 80) {
            stars.add(
                Star(
                    random.nextFloat(),
                    random.nextFloat(),
                    1f + random.nextFloat() * 4f
                )
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Space background
        canvas.drawColor(Color.rgb(3, 5, 20))

        drawStars(canvas)

        if (playerX == 0f) {
            playerX = width / 2f
            playerY = height * 0.82f
        }

        if (!gameOver) {
            updateGame()
        }

        drawPlayer(canvas)
        drawBullets(canvas)
        drawEnemies(canvas)
        drawExplosions(canvas)
        drawHud(canvas)

        if (gameOver) {
            drawGameOver(canvas)
        }

        postInvalidateDelayed(16)
    }

    private fun drawStars(canvas: Canvas) {
        paint.color = Color.WHITE

        for (star in stars) {
            val x = star.x * width
            val y = star.y * height

            paint.alpha = (100 + star.size * 35).toInt().coerceIn(80, 255)

            canvas.drawCircle(
                x,
                y,
                star.size,
                paint
            )
        }

        paint.alpha = 255
    }

    private fun drawPlayer(canvas: Canvas) {
        val path = Path()

        path.moveTo(playerX, playerY - 55f)
        path.lineTo(playerX - 38f, playerY + 35f)
        path.lineTo(playerX - 12f, playerY + 25f)
        path.lineTo(playerX, playerY + 48f)
        path.lineTo(playerX + 12f, playerY + 25f)
        path.lineTo(playerX + 38f, playerY + 35f)
        path.close()

        paint.color = Color.CYAN
        paint.style = Paint.Style.FILL

        canvas.drawPath(path, paint)

        paint.color = Color.WHITE

        val cockpit = Path()
        cockpit.moveTo(playerX, playerY - 35f)
        cockpit.lineTo(playerX - 10f, playerY + 5f)
        cockpit.lineTo(playerX + 10f, playerY + 5f)
        cockpit.close()

        canvas.drawPath(cockpit, paint)

        // Engine flames
        paint.color = Color.YELLOW

        canvas.drawCircle(playerX - 17f, playerY + 38f, 7f, paint)
        canvas.drawCircle(playerX + 17f, playerY + 38f, 7f, paint)
    }

    private fun drawBullets(canvas: Canvas) {
        paint.color = Color.YELLOW

        for (bullet in bullets) {
            canvas.drawRoundRect(
                bullet.x - 4f,
                bullet.y - 15f,
                bullet.x + 4f,
                bullet.y + 15f,
                5f,
                5f,
                paint
            )
        }
    }

    private fun drawEnemies(canvas: Canvas) {
        for (enemy in enemies) {
            paint.color = Color.RED

            canvas.drawCircle(
                enemy.x,
                enemy.y,
                enemy.radius,
                paint
            )

            paint.color = Color.rgb(255, 120, 0)

            canvas.drawCircle(
                enemy.x,
                enemy.y,
                enemy.radius * 0.45f,
                paint
            )

            paint.color = Color.WHITE

            canvas.drawCircle(
                enemy.x - enemy.radius * 0.3f,
                enemy.y - enemy.radius * 0.15f,
                4f,
                paint
            )

            canvas.drawCircle(
                enemy.x + enemy.radius * 0.3f,
                enemy.y - enemy.radius * 0.15f,
                4f,
                paint
            )
        }
    }

    private fun drawExplosions(canvas: Canvas) {
        for (explosion in explosions) {
            paint.color = Color.YELLOW
            paint.alpha = explosion.alpha

            canvas.drawCircle(
                explosion.x,
                explosion.y,
                explosion.radius,
                paint
            )

            paint.color = Color.RED

            canvas.drawCircle(
                explosion.x,
                explosion.y,
                explosion.radius * 0.55f,
                paint
            )
        }

        paint.alpha = 255
    }

    private fun drawHud(canvas: Canvas) {
        textPaint.color = Color.WHITE
        textPaint.textSize = 42f

        canvas.drawText(
            "SCORE: $score",
            30f,
            55f,
            textPaint
        )

        canvas.drawText(
            "LIVES: $lives",
            width - 190f,
            55f,
            textPaint
        )
    }

    private fun drawGameOver(canvas: Canvas) {
        paint.color = Color.argb(190, 0, 0, 0)
        canvas.drawRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            paint
        )

        textPaint.textAlign = Paint.Align.CENTER

        textPaint.color = Color.RED
        textPaint.textSize = 80f

        canvas.drawText(
            "GAME OVER",
            width / 2f,
            height / 2f - 40f,
            textPaint
        )

        textPaint.color = Color.WHITE
        textPaint.textSize = 42f

        canvas.drawText(
            "Score: $score",
            width / 2f,
            height / 2f + 35f,
            textPaint
        )

        textPaint.textSize = 32f

        canvas.drawText(
            "Tap to restart",
            width / 2f,
            height / 2f + 95f,
            textPaint
        )

        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun updateGame() {
        val now = System.currentTimeMillis()

        // Automatic shooting
        if (now - lastBulletTime > 350) {
            bullets.add(
                Bullet(
                    playerX,
                    playerY - 60f
                )
            )

            lastBulletTime = now
        }

        // Spawn enemies
        if (now - lastEnemyTime > 800) {
            enemies.add(
                Enemy(
                    random.nextFloat() * (width - 100f) + 50f,
                    -60f,
                    28f + random.nextFloat() * 12f,
                    5f + random.nextFloat() * 4f
                )
            )

            lastEnemyTime = now
        }

        // Move bullets
        val bulletIterator = bullets.iterator()

        while (bulletIterator.hasNext()) {
            val bullet = bulletIterator.next()

            bullet.y -= 18f

            if (bullet.y < -30f) {
                bulletIterator.remove()
            }
        }

        // Move enemies
        val enemyIterator = enemies.iterator()

        while (enemyIterator.hasNext()) {
            val enemy = enemyIterator.next()

            enemy.y += enemy.speed

            if (enemy.y > height + 80f) {
                enemyIterator.remove()
                lives--

                if (lives <= 0) {
                    gameOver = true
                }

                continue
            }

            // Enemy-player collision
            val dx = enemy.x - playerX
            val dy = enemy.y - playerY

            if (dx * dx + dy * dy <
                (enemy.radius + 35f) * (enemy.radius + 35f)
            ) {
                explosions.add(
                    Explosion(enemy.x, enemy.y)
                )

                enemyIterator.remove()
                lives--

                if (lives <= 0) {
                    gameOver = true
                }
            }
        }

        // Bullet-enemy collisions
        val bulletsToRemove = mutableSetOf<Bullet>()
        val enemiesToRemove = mutableSetOf<Enemy>()

        for (bullet in bullets) {
            for (enemy in enemies) {

                val dx = bullet.x - enemy.x
                val dy = bullet.y - enemy.y

                if (dx * dx + dy * dy <
                    enemy.radius * enemy.radius
                ) {
                    bulletsToRemove.add(bullet)
                    enemiesToRemove.add(enemy)

                    explosions.add(
                        Explosion(enemy.x, enemy.y)
                    )

                    score += 10
                }
            }
        }

        bullets.removeAll(bulletsToRemove)
        enemies.removeAll(enemiesToRemove)

        // Update explosions
        val explosionIterator = explosions.iterator()

        while (explosionIterator.hasNext()) {
            val explosion = explosionIterator.next()

            explosion.radius += 5f
            explosion.alpha -= 15

            if (explosion.alpha <= 0) {
                explosionIterator.remove()
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        when (event.action) {

            MotionEvent.ACTION_DOWN -> {

                if (gameOver) {
                    restartGame()
                    return true
                }

                lastTouchX = event.x
                lastTouchY = event.y

                return true
            }

            MotionEvent.ACTION_MOVE -> {

                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY

                playerX += dx
                playerY += dy

                playerX = playerX.coerceIn(50f, width - 50f)
                playerY = playerY.coerceIn(
                    height * 0.45f,
                    height - 80f
                )

                lastTouchX = event.x
                lastTouchY = event.y

                return true
            }
        }

        return true
    }

    private fun restartGame() {
        score = 0
        lives = 3
        bullets.clear()
        enemies.clear()
        explosions.clear()
        gameOver = false

        playerX = width / 2f
        playerY = height * 0.82f
    }

    private data class Bullet(
        var x: Float,
        var y: Float
    )

    private data class Enemy(
        var x: Float,
        var y: Float,
        var radius: Float,
        var speed: Float
    )

    private data class Star(
        val x: Float,
        val y: Float,
        val size: Float
    )

    private data class Explosion(
        var x: Float,
        var y: Float,
        var radius: Float = 10f,
        var alpha: Int = 255
    )
}
