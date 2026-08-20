package com.pratikmardi.galaxystrike

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GameView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var playerX = 0f
    private var playerY = 0f
    private var initialized = false

    private var score = 0

    private data class Enemy(
        var x: Float,
        var y: Float,
        var speed: Float
    )

    private data class Bullet(
        var x: Float,
        var y: Float,
        var speed: Float
    )

    private val enemies = mutableListOf<Enemy>()
    private val bullets = mutableListOf<Bullet>()

    private var lastSpawnTime = 0L
    private var lastShotTime = 0L

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(Color.BLACK)

        if (!initialized) {
            playerX = width / 2f
            playerY = height * 0.82f
            initialized = true
        }

        // Stars
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL

        for (i in 0 until 80) {
            val x = ((i * 97) % max(width, 1)).toFloat()
            val y = ((i * 173) % max(height, 1)).toFloat()

            canvas.drawCircle(
                x,
                y,
                1.5f,
                paint
            )
        }

        // HUD
        paint.color = Color.WHITE
        paint.textSize = 42f
        paint.typeface = Typeface.DEFAULT_BOLD

        canvas.drawText(
            "SCORE  ${score.toString().padStart(6, '0')}",
            30f,
            55f,
            paint
        )

        canvas.drawText(
            "LIVES  3",
            width - 190f,
            55f,
            paint
        )

        val currentTime = System.currentTimeMillis()

        // Spawn enemies
        if (currentTime - lastSpawnTime > 900) {
            spawnEnemy()
            lastSpawnTime = currentTime
        }

        // Update enemies
        val enemyIterator = enemies.iterator()

        while (enemyIterator.hasNext()) {

            val enemy = enemyIterator.next()

            enemy.y += enemy.speed

            drawEnemy(
                canvas,
                enemy.x,
                enemy.y
            )

            if (enemy.y > height + 100f) {
                enemyIterator.remove()
            }
        }

        // Update bullets
        val bulletIterator = bullets.iterator()

        while (bulletIterator.hasNext()) {

            val bullet = bulletIterator.next()

            bullet.y -= bullet.speed

            drawBullet(
                canvas,
                bullet.x,
                bullet.y
            )

            if (bullet.y < -50f) {
                bulletIterator.remove()
            }
        }

        // Bullet / enemy collision
        val bulletsToRemove = mutableListOf<Bullet>()
        val enemiesToRemove = mutableListOf<Enemy>()

        for (bullet in bullets) {

            for (enemy in enemies) {

                val distanceX =
                    bullet.x - enemy.x

                val distanceY =
                    bullet.y - enemy.y

                val distanceSquared =
                    distanceX * distanceX +
                    distanceY * distanceY

                if (distanceSquared < 55f * 55f) {

                    bulletsToRemove.add(bullet)
                    enemiesToRemove.add(enemy)

                    score += 100

                    break
                }
            }
        }

        bullets.removeAll(bulletsToRemove)
        enemies.removeAll(enemiesToRemove)

        // Player
        drawPlayerShip(
            canvas,
            playerX,
            playerY
        )

        // Keep game running
        postInvalidateOnAnimation()
    }

    private fun spawnEnemy() {

        val safeWidth =
            width.coerceAtLeast(120)

        val x =
            Random.nextFloat() *
            (safeWidth - 120f) +
            60f

        val speed =
            Random.nextFloat() * 4f + 3f

        enemies.add(
            Enemy(
                x = x,
                y = -80f,
                speed = speed
            )
        )
    }

    private fun shoot() {

        val currentTime =
            System.currentTimeMillis()

        // Fire rate
        if (currentTime - lastShotTime < 180) {
            return
        }

        lastShotTime = currentTime

        bullets.add(
            Bullet(
                x = playerX,
                y = playerY - 65f,
                speed = 18f
            )
        )
    }

    private fun drawBullet(
        canvas: Canvas,
        x: Float,
        y: Float
    ) {

        paint.style = Paint.Style.FILL
        paint.color = Color.CYAN

        canvas.drawRoundRect(
            x - 5f,
            y - 22f,
            x + 5f,
            y + 22f,
            5f,
            5f,
            paint
        )
    }

    private fun drawEnemy(
        canvas: Canvas,
        x: Float,
        y: Float
    ) {

        paint.style = Paint.Style.FILL
        paint.color = Color.RED

        val path = Path()

        path.moveTo(
            x,
            y + 55f
        )

        path.lineTo(
            x - 45f,
            y - 25f
        )

        path.lineTo(
            x - 18f,
            y - 15f
        )

        path.lineTo(
            x,
            y - 55f
        )

        path.lineTo(
            x + 18f,
            y - 15f
        )

        path.lineTo(
            x + 45f,
            y - 25f
        )

        path.close()

        canvas.drawPath(
            path,
            paint
        )

        // Cockpit
        paint.color = Color.YELLOW

        canvas.drawCircle(
            x,
            y - 5f,
            12f,
            paint
        )

        // Wings
        paint.color = Color.rgb(
            180,
            20,
            20
        )

        canvas.drawRect(
            x - 55f,
            y - 10f,
            x - 15f,
            y + 8f,
            paint
        )

        canvas.drawRect(
            x + 15f,
            y - 10f,
            x + 55f,
            y + 8f,
            paint
        )
    }

    private fun drawPlayerShip(
        canvas: Canvas,
        x: Float,
        y: Float
    ) {

        val path = Path()

        path.moveTo(
            x,
            y - 65f
        )

        path.lineTo(
            x - 42f,
            y + 45f
        )

        path.lineTo(
            x - 18f,
            y + 32f
        )

        path.lineTo(
            x,
            y + 55f
        )

        path.lineTo(
            x + 18f,
            y + 32f
        )

        path.lineTo(
            x + 42f,
            y + 45f
        )

        path.close()

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(
            40,
            150,
            255
        )

        canvas.drawPath(
            path,
            paint
        )

        // Cockpit
        paint.color = Color.CYAN

        canvas.drawCircle(
            x,
            y - 15f,
            13f,
            paint
        )

        // Wings
        paint.color = Color.rgb(
            20,
            90,
            180
        )

        canvas.drawRect(
            x - 48f,
            y + 20f,
            x - 12f,
            y + 38f,
            paint
        )

        canvas.drawRect(
            x + 12f,
            y + 20f,
            x + 48f,
            y + 38f,
            paint
        )

        // Engine flames
        paint.color = Color.YELLOW

        val flameLeft = Path()

        flameLeft.moveTo(
            x - 18f,
            y + 45f
        )

        flameLeft.lineTo(
            x - 8f,
            y + 82f
        )

        flameLeft.lineTo(
            x,
            y + 48f
        )

        flameLeft.close()

        canvas.drawPath(
            flameLeft,
            paint
        )

        val flameRight = Path()

        flameRight.moveTo(
            x,
            y + 48f
        )

        flameRight.lineTo(
            x + 8f,
            y + 82f
        )

        flameRight.lineTo(
            x + 18f,
            y + 45f
        )

        flameRight.close()

        canvas.drawPath(
            flameRight,
            paint
        )
    }

    override fun onTouchEvent(
        event: MotionEvent
    ): Boolean {

        when (event.action) {

            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {

                playerX = min(
                    width - 60f,
                    max(60f, event.x)
                )

                playerY = min(
                    height - 100f,
                    max(
                        height * 0.55f,
                        event.y
                    )
                )

                // Shoot
                shoot()

                invalidate()

                return true
            }
        }

        return true
    }
}
