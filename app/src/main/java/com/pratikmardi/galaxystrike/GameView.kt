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

    private data class Star(
        var x: Float,
        var y: Float,
        var speed: Float,
        var size: Float,
        var alpha: Int
    )

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

    private data class Explosion(
        var x: Float,
        var y: Float,
        var age: Float = 0f
    )

    private data class ShootingStar(
        var x: Float,
        var y: Float,
        var speed: Float,
        var length: Float,
        var life: Float = 0f
    )

    private val stars = mutableListOf<Star>()
    private val enemies = mutableListOf<Enemy>()
    private val bullets = mutableListOf<Bullet>()
    private val explosions = mutableListOf<Explosion>()
    private val shootingStars = mutableListOf<ShootingStar>()

    private var lastSpawnTime = 0L
    private var lastShotTime = 0L
    private var lastShootingStarTime = 0L
    private var lastFrameTime = System.currentTimeMillis()

    private val playerShip: Bitmap = BitmapFactory.decodeResource(
        resources,
        resources.getIdentifier(
            "player_ship",
            "drawable",
            context.packageName
        )
    )

    init {

        // Background star field
        repeat(180) {

            stars.add(
                Star(
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    speed = Random.nextFloat() * 2.8f + 0.4f,
                    size = Random.nextFloat() * 2.8f + 0.4f,
                    alpha = Random.nextInt(90, 256)
                )
            )
        }
    }

    override fun onDraw(canvas: Canvas) {

        super.onDraw(canvas)

        val currentTime = System.currentTimeMillis()

        val delta =
            ((currentTime - lastFrameTime)
                .coerceAtMost(50L)) / 16.67f

        lastFrameTime = currentTime

        // -----------------------------------------
        // DEEP SPACE BACKGROUND
        // -----------------------------------------

        drawSpaceBackground(canvas)

        // -----------------------------------------
        // PLAYER POSITION
        // -----------------------------------------

        if (!initialized) {

            playerX = width / 2f
            playerY = height * 0.82f

            initialized = true
        }

        // -----------------------------------------
        // STARS
        // -----------------------------------------

        drawStars(
            canvas,
            delta
        )

        // -----------------------------------------
        // SHOOTING STARS
        // -----------------------------------------

        updateShootingStars(
            canvas,
            delta,
            currentTime
        )

        // -----------------------------------------
        // HUD
        // -----------------------------------------

        paint.shader = null
        paint.style = Paint.Style.FILL
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

        // -----------------------------------------
        // SPAWN ENEMIES
        // -----------------------------------------

        if (currentTime - lastSpawnTime > 900) {

            spawnEnemy()

            lastSpawnTime = currentTime
        }

        // -----------------------------------------
        // ENEMIES
        // -----------------------------------------

        val enemyIterator =
            enemies.iterator()

        while (enemyIterator.hasNext()) {

            val enemy =
                enemyIterator.next()

            enemy.y +=
                enemy.speed * delta

            drawEnemy(
                canvas,
                enemy.x,
                enemy.y
            )

            if (enemy.y > height + 100f) {

                enemyIterator.remove()
            }
        }

        // -----------------------------------------
        // BULLETS
        // -----------------------------------------

        val bulletIterator =
            bullets.iterator()

        while (bulletIterator.hasNext()) {

            val bullet =
                bulletIterator.next()

            bullet.y -=
                bullet.speed * delta

            drawBullet(
                canvas,
                bullet.x,
                bullet.y
            )

            if (bullet.y < -50f) {

                bulletIterator.remove()
            }
        }

        // -----------------------------------------
        // COLLISIONS
        // -----------------------------------------

        val bulletsToRemove =
            mutableListOf<Bullet>()

        val enemiesToRemove =
            mutableListOf<Enemy>()

        for (bullet in bullets) {

            for (enemy in enemies) {

                val dx =
                    bullet.x - enemy.x

                val dy =
                    bullet.y - enemy.y

                val distanceSquared =
                    dx * dx + dy * dy

                if (distanceSquared <
                    55f * 55f) {

                    bulletsToRemove.add(
                        bullet
                    )

                    enemiesToRemove.add(
                        enemy
                    )

                    explosions.add(
                        Explosion(
                            enemy.x,
                            enemy.y
                        )
                    )

                    score += 100

                    break
                }
            }
        }

        bullets.removeAll(
            bulletsToRemove
        )

        enemies.removeAll(
            enemiesToRemove
        )

        // -----------------------------------------
        // EXPLOSIONS
        // -----------------------------------------

        val explosionIterator =
            explosions.iterator()

        while (explosionIterator.hasNext()) {

            val explosion =
                explosionIterator.next()

            explosion.age += delta

            drawExplosion(
                canvas,
                explosion
            )

            if (explosion.age > 24f) {

                explosionIterator.remove()
            }
        }

        // -----------------------------------------
        // PLAYER SHIP
        // -----------------------------------------

        drawPlayerShip(
            canvas,
            playerX,
            playerY
        )

        postInvalidateOnAnimation()
    }

    // =================================================
    // SPACE BACKGROUND
    // =================================================

    private fun drawSpaceBackground(
        canvas: Canvas
    ) {

        val gradient =
            LinearGradient(
                0f,
                0f,
                0f,
                height.toFloat(),
                Color.rgb(1, 2, 15),
                Color.rgb(0, 0, 5),
                Shader.TileMode.CLAMP
            )

        paint.shader = gradient

        canvas.drawRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            paint
        )

        paint.shader = null

        // Subtle nebula cloud 1

        val nebula1 =
            RadialGradient(
                width * 0.25f,
                height * 0.35f,
                width * 0.55f,
                Color.argb(
                    28,
                    70,
                    60,
                    180
                ),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )

        paint.shader = nebula1

        canvas.drawRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            paint
        )

        paint.shader = null

        // Subtle nebula cloud 2

        val nebula2 =
            RadialGradient(
                width * 0.80f,
                height * 0.65f,
                width * 0.50f,
                Color.argb(
                    22,
                    120,
                    30,
                    150
                ),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )

        paint.shader = nebula2

        canvas.drawRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            paint
        )

        paint.shader = null
    }

    // =================================================
    // STARS
    // =================================================

    private fun drawStars(
        canvas: Canvas,
        delta: Float
    ) {

        for (star in stars) {

            star.y +=
                star.speed *
                delta /
                height.toFloat()

            if (star.y > 1f) {

                star.y = 0f
                star.x = Random.nextFloat()
            }

            paint.style =
                Paint.Style.FILL

            paint.color =
                Color.argb(
                    star.alpha,
                    255,
                    255,
                    255
                )

            canvas.drawCircle(
                star.x * width,
                star.y * height,
                star.size,
                paint
            )
        }
    }

    // =================================================
    // SHOOTING STARS
    // =================================================

    private fun updateShootingStars(
        canvas: Canvas,
        delta: Float,
        currentTime: Long
    ) {

        if (currentTime -
            lastShootingStarTime > 5000) {

            if (Random.nextFloat() < 0.35f) {

                shootingStars.add(
                    ShootingStar(
                        x = Random.nextFloat() *
                            width,
                        y = Random.nextFloat() *
                            height * 0.45f,
                        speed = Random.nextFloat() *
                            12f + 10f,
                        length = Random.nextFloat() *
                            70f + 50f
                    )
                )
            }

            lastShootingStarTime =
                currentTime
        }

        val iterator =
            shootingStars.iterator()

        while (iterator.hasNext()) {

            val star =
                iterator.next()

            star.life += delta

            star.x +=
                star.speed * delta

            star.y +=
                star.speed * 0.35f * delta

            paint.shader = null

            paint.strokeWidth = 3f

            paint.style =
                Paint.Style.STROKE

            paint.color =
                Color.argb(
                    ((1f -
                        star.life / 40f) *
                        220f)
                        .toInt()
                        .coerceIn(0, 220),
                    220,
                    240,
                    255
                )

            canvas.drawLine(
                star.x,
                star.y,
                star.x -
                    star.length,
                star.y -
                    star.length * 0.35f,
                paint
            )

            if (star.life > 40f ||
                star.x > width + 150f ||
                star.y > height + 100f) {

                iterator.remove()
            }
        }

        paint.style =
            Paint.Style.FILL
    }

    // =================================================
    // ENEMY
    // =================================================

    private fun spawnEnemy() {

        val safeWidth =
            width.coerceAtLeast(120)

        val x =
            Random.nextFloat() *
                (safeWidth - 120f) +
                60f

        val speed =
            Random.nextFloat() *
                4f + 3f

        enemies.add(
            Enemy(
                x,
                -80f,
                speed
            )
        )
    }

    private fun drawEnemy(
        canvas: Canvas,
        x: Float,
        y: Float
    ) {

        paint.shader = null
        paint.style =
            Paint.Style.FILL

        paint.color =
            Color.rgb(
                180,
                20,
                30
            )

        val path =
            Path()

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

        paint.color =
            Color.YELLOW

        canvas.drawCircle(
            x,
            y - 5f,
            12f,
            paint
        )

        paint.color =
            Color.rgb(
                110,
                10,
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

    // =================================================
    // BULLET
    // =================================================

    private fun drawBullet(
        canvas: Canvas,
        x: Float,
        y: Float
    ) {

        paint.style =
            Paint.Style.FILL

        paint.color =
            Color.argb(
                70,
                0,
                255,
                255
            )

        canvas.drawCircle(
            x,
            y,
            13f,
            paint
        )

        paint.color =
            Color.CYAN

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

    // =================================================
    // EXPLOSION
    // =================================================

    private fun drawExplosion(
        canvas: Canvas,
        explosion: Explosion
    ) {

        val progress =
            explosion.age / 24f

        val radius =
            10f +
                progress * 75f

        val alpha =
            ((1f - progress) *
                255f)
                .toInt()
                .coerceIn(
                    0,
                    255
                )

        paint.style =
            Paint.Style.FILL

        paint.color =
            Color.argb(
                alpha / 3,
                255,
                80,
                0
            )

        canvas.drawCircle(
            explosion.x,
            explosion.y,
            radius * 1.5f,
            paint
        )

        paint.color =
            Color.argb(
                alpha,
                255,
                100,
                0
            )

        canvas.drawCircle(
            explosion.x,
            explosion.y,
            radius,
            paint
        )

        paint.color =
            Color.argb(
                alpha,
                255,
                220,
                50
            )

        canvas.drawCircle(
            explosion.x,
            explosion.y,
            radius * 0.55f,
            paint
        )

        paint.color =
            Color.argb(
                alpha,
                255,
                255,
                220
            )

        canvas.drawCircle(
            explosion.x,
            explosion.y,
            radius * 0.25f,
            paint
        )
    }

    // =================================================
    // PLAYER SHIP — 20% BIGGER
    // =================================================

    private fun drawPlayerShip(
        canvas: Canvas,
        x: Float,
        y: Float
    ) {

        // Original: 120f
        // New: 144f = 20% larger

        val targetWidth =
            144f

        val scale =
            targetWidth /
                playerShip.width.toFloat()

        val targetHeight =
            playerShip.height * scale

        val destination =
            RectF(
                x - targetWidth / 2f,
                y - targetHeight / 2f,
                x + targetWidth / 2f,
                y + targetHeight / 2f
            )

        paint.alpha = 255
        paint.isFilterBitmap = true

        canvas.drawBitmap(
            playerShip,
            null,
            destination,
            paint
        )
    }

    // =================================================
    // TOUCH
    // =================================================

    override fun onTouchEvent(
        event: MotionEvent
    ): Boolean {

        when (event.action) {

            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {

                playerX =
                    min(
                        width - 70f,
                        max(
                            70f,
                            event.x
                        )
                    )

                playerY =
                    min(
                        height - 120f,
                        max(
                            height * 0.55f,
                            event.y
                        )
                    )

                shoot()

                invalidate()

                return true
            }
        }

        return true
    }

    // =================================================
    // SHOOT
    // =================================================

    private fun shoot() {

        val currentTime =
            System.currentTimeMillis()

        if (currentTime -
            lastShotTime < 180) {
            return
        }

        lastShotTime =
            currentTime

        bullets.add(
            Bullet(
                playerX,
                playerY - 65f,
                18f
            )
        )
    }
}
