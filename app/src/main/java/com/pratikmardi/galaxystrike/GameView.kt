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

    // --------------------------------------------------
    // DATA
    // --------------------------------------------------

    private enum class EnemyType {
        SCOUT,
        FIGHTER,
        TANK
    }

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
        var speed: Float,
        var type: EnemyType,
        var health: Int,
        var maxHealth: Int,
        var hitFlash: Float = 0f
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

    // --------------------------------------------------
    // OBJECTS
    // --------------------------------------------------

    private val stars = mutableListOf<Star>()
    private val enemies = mutableListOf<Enemy>()
    private val bullets = mutableListOf<Bullet>()
    private val explosions = mutableListOf<Explosion>()
    private val shootingStars = mutableListOf<ShootingStar>()

    // --------------------------------------------------
    // TIMERS
    // --------------------------------------------------

    private var lastSpawnTime = 0L
    private var lastShotTime = 0L
    private var lastShootingStarTime = 0L
    private var lastFrameTime = System.currentTimeMillis()

    // --------------------------------------------------
    // PLAYER IMAGE
    // --------------------------------------------------

    private val playerShip: Bitmap =
        BitmapFactory.decodeResource(
            resources,
            resources.getIdentifier(
                "player_ship",
                "drawable",
                context.packageName
            )
        )

    // --------------------------------------------------
    // INITIALIZATION
    // --------------------------------------------------

    init {

        repeat(180) {

            stars.add(
                Star(
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    speed =
                        Random.nextFloat() * 2.8f + 0.4f,
                    size =
                        Random.nextFloat() * 2.8f + 0.4f,
                    alpha =
                        Random.nextInt(90, 256)
                )
            )
        }
    }

    // --------------------------------------------------
    // MAIN GAME LOOP
    // --------------------------------------------------

    override fun onDraw(canvas: Canvas) {

        super.onDraw(canvas)

        val currentTime =
            System.currentTimeMillis()

        val delta =
            ((currentTime - lastFrameTime)
                .coerceAtMost(50L)) / 16.67f

        lastFrameTime = currentTime

        drawSpaceBackground(canvas)

        if (!initialized) {

            playerX = width / 2f
            playerY = height * 0.82f

            initialized = true
        }

        drawStars(canvas, delta)

        updateShootingStars(
            canvas,
            delta,
            currentTime
        )

        drawHUD(canvas)

        // --------------------------------------------------
        // DIFFICULTY
        // --------------------------------------------------

        val difficulty =
            1f + (score / 3000f).coerceAtMost(2.5f)

        // --------------------------------------------------
        // ENEMY SPAWNING
        // --------------------------------------------------

        val spawnDelay =
            (950f / difficulty)
                .toLong()
                .coerceAtLeast(350L)

        if (currentTime - lastSpawnTime >
            spawnDelay) {

            spawnEnemy(difficulty)

            lastSpawnTime =
                currentTime
        }

        // --------------------------------------------------
        // ENEMIES
        // --------------------------------------------------

        updateEnemies(
            canvas,
            delta
        )

        // --------------------------------------------------
        // BULLETS
        // --------------------------------------------------

        updateBullets(
            canvas,
            delta
        )

        // --------------------------------------------------
        // COLLISIONS
        // --------------------------------------------------

        handleCollisions()

        // --------------------------------------------------
        // EXPLOSIONS
        // --------------------------------------------------

        updateExplosions(
            canvas,
            delta
        )

        // --------------------------------------------------
        // PLAYER
        // --------------------------------------------------

        drawPlayerShip(
            canvas,
            playerX,
            playerY
        )

        postInvalidateOnAnimation()
    }

    // ==================================================
    // BACKGROUND
    // ==================================================

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

    // ==================================================
    // STARS
    // ==================================================

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

    // ==================================================
    // SHOOTING STARS
    // ==================================================

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
                        x =
                            Random.nextFloat() *
                                width,
                        y =
                            Random.nextFloat() *
                                height * 0.45f,
                        speed =
                            Random.nextFloat() *
                                12f + 10f,
                        length =
                            Random.nextFloat() *
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
                star.speed *
                0.35f *
                delta

            paint.style =
                Paint.Style.STROKE

            paint.strokeWidth = 3f

            paint.color =
                Color.argb(
                    ((1f -
                        star.life / 40f) *
                        220f)
                        .toInt()
                        .coerceIn(
                            0,
                            220
                        ),
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

    // ==================================================
    // SPAWN ENEMY
    // ==================================================

    private fun spawnEnemy(
        difficulty: Float
    ) {

        val safeWidth =
            width.coerceAtLeast(160)

        val x =
            Random.nextFloat() *
                (safeWidth - 140f) +
                70f

        val roll =
            Random.nextFloat()

        val type: EnemyType

        if (roll < 0.55f) {

            type =
                EnemyType.SCOUT

        } else if (roll < 0.88f) {

            type =
                EnemyType.FIGHTER

        } else {

            type =
                EnemyType.TANK
        }

        val health: Int

        val speed: Float

        when (type) {

            EnemyType.SCOUT -> {

                health = 1

                speed =
                    Random.nextFloat() *
                        3f +
                        7f
            }

            EnemyType.FIGHTER -> {

                health = 2

                speed =
                    Random.nextFloat() *
                        2.5f +
                        4.5f
            }

            EnemyType.TANK -> {

                health = 4

                speed =
                    Random.nextFloat() *
                        1.5f +
                        2.5f
            }
        }

        val boostedHealth =
            health +
                ((difficulty - 1f) / 1.5f)
                    .toInt()

        enemies.add(
            Enemy(
                x = x,
                y = -100f,
                speed = speed *
                    (0.9f +
                        difficulty * 0.1f),
                type = type,
                health = boostedHealth,
                maxHealth = boostedHealth
            )
        )
    }

    // ==================================================
    // UPDATE ENEMIES
    // ==================================================

    private fun updateEnemies(
        canvas: Canvas,
        delta: Float
    ) {

        val iterator =
            enemies.iterator()

        while (iterator.hasNext()) {

            val enemy =
                iterator.next()

            enemy.y +=
                enemy.speed *
                delta

            if (enemy.hitFlash > 0f) {

                enemy.hitFlash -= delta
            }

            drawEnemy(
                canvas,
                enemy
            )

            if (enemy.y >
                height + 120f) {

                iterator.remove()
            }
        }
    }

    // ==================================================
    // DRAW ENEMY
    // ==================================================

    private fun drawEnemy(
        canvas: Canvas,
        enemy: Enemy
    ) {

        val x = enemy.x
        val y = enemy.y

        paint.shader = null
        paint.style =
            Paint.Style.FILL

        when (enemy.type) {

            // ------------------------------------------
            // SCOUT
            // ------------------------------------------

            EnemyType.SCOUT -> {

                paint.color =
                    Color.rgb(
                        40,
                        220,
                        130
                    )

                val path =
                    Path()

                path.moveTo(
                    x,
                    y - 45f
                )

                path.lineTo(
                    x - 32f,
                    y + 35f
                )

                path.lineTo(
                    x,
                    y + 20f
                )

                path.lineTo(
                    x + 32f,
                    y + 35f
                )

                path.close()

                canvas.drawPath(
                    path,
                    paint
                )

                paint.color =
                    Color.CYAN

                canvas.drawCircle(
                    x,
                    y - 5f,
                    8f,
                    paint
                )
            }

            // ------------------------------------------
            // FIGHTER
            // ------------------------------------------

            EnemyType.FIGHTER -> {

                paint.color =
                    Color.rgb(
                        200,
                        35,
                        50
                    )

                val path =
                    Path()

                path.moveTo(
                    x,
                    y - 55f
                )

                path.lineTo(
                    x - 45f,
                    y + 35f
                )

                path.lineTo(
                    x - 15f,
                    y + 20f
                )

                path.lineTo(
                    x,
                    y + 45f
                )

                path.lineTo(
                    x + 15f,
                    y + 20f
                )

                path.lineTo(
                    x + 45f,
                    y + 35f
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
                    y - 10f,
                    11f,
                    paint
                )
            }

            // ------------------------------------------
            // TANK
            // ------------------------------------------

            EnemyType.TANK -> {

                paint.color =
                    Color.rgb(
                        130,
                        50,
                        180
                    )

                canvas.drawOval(
                    x - 58f,
                    y - 38f,
                    x + 58f,
                    y + 38f,
                    paint
                )

                paint.color =
                    Color.rgb(
                        75,
                        20,
                        110
                    )

                canvas.drawRect(
                    x - 70f,
                    y - 12f,
                    x + 70f,
                    y + 12f,
                    paint
                )

                paint.color =
                    Color.rgb(
                        255,
                        80,
                        220
                    )

                canvas.drawCircle(
                    x,
                    y,
                    18f,
                    paint
                )
            }
        }

        // --------------------------------------------------
        // HIT FLASH
        // --------------------------------------------------

        if (enemy.hitFlash > 0f) {

            paint.color =
                Color.WHITE

            when (enemy.type) {

                EnemyType.SCOUT -> {

                    canvas.drawCircle(
                        x,
                        y,
                        40f,
                        paint
                    )
                }

                EnemyType.FIGHTER -> {

                    canvas.drawCircle(
                        x,
                        y,
                        48f,
                        paint
                    )
                }

                EnemyType.TANK -> {

                    canvas.drawCircle(
                        x,
                        y,
                        65f,
                        paint
                    )
                }
            }
        }

        // --------------------------------------------------
        // HEALTH BAR
        // --------------------------------------------------

        if (enemy.health <
            enemy.maxHealth) {

            val barWidth =
                when (enemy.type) {

                    EnemyType.SCOUT ->
                        50f

                    EnemyType.FIGHTER ->
                        65f

                    EnemyType.TANK ->
                        95f
                }

            val healthPercent =
                enemy.health.toFloat() /
                    enemy.maxHealth

            paint.color =
                Color.DKGRAY

            canvas.drawRect(
                x - barWidth / 2f,
                y - 75f,
                x + barWidth / 2f,
                y - 68f,
                paint
            )

            paint.color =
                Color.GREEN

            canvas.drawRect(
                x - barWidth / 2f,
                y - 75f,
                x -
                    barWidth / 2f +
                    barWidth *
                    healthPercent,
                y - 68f,
                paint
            )
        }
    }

    // ==================================================
    // BULLETS
    // ==================================================

    private fun updateBullets(
        canvas: Canvas,
        delta: Float
    ) {

        val iterator =
            bullets.iterator()

        while (iterator.hasNext()) {

            val bullet =
                iterator.next()

            bullet.y -=
                bullet.speed *
                delta

            drawBullet(
                canvas,
                bullet.x,
                bullet.y
            )

            if (bullet.y < -50f) {

                iterator.remove()
            }
        }
    }

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

    // ==================================================
    // COLLISION
    // ==================================================

    private fun handleCollisions() {

        val bulletsToRemove =
            mutableListOf<Bullet>()

        val enemiesToRemove =
            mutableListOf<Enemy>()

        for (bullet in bullets) {

            for (enemy in enemies) {

                val dx =
                    bullet.x -
                        enemy.x

                val dy =
                    bullet.y -
                        enemy.y

                val distanceSquared =
                    dx * dx +
                    dy * dy

                val hitRadius =
                    when (enemy.type) {

                        EnemyType.SCOUT ->
                            45f

                        EnemyType.FIGHTER ->
                            55f

                        EnemyType.TANK ->
                            70f
                    }

                if (distanceSquared <
                    hitRadius *
                    hitRadius) {

                    bulletsToRemove.add(
                        bullet
                    )

                    enemy.health--

                    enemy.hitFlash =
                        4f

                    if (enemy.health <= 0) {

                        enemiesToRemove.add(
                            enemy
                        )

                        val reward =
                            when (enemy.type) {

                                EnemyType.SCOUT ->
                                    100

                                EnemyType.FIGHTER ->
                                    250

                                EnemyType.TANK ->
                                    600
                            }

                        score += reward

                        explosions.add(
                            Explosion(
                                enemy.x,
                                enemy.y
                            )
                        )
                    }

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
    }

    // ==================================================
    // EXPLOSIONS
    // ==================================================

    private fun updateExplosions(
        canvas: Canvas,
        delta: Float
    ) {

        val iterator =
            explosions.iterator()

        while (iterator.hasNext()) {

            val explosion =
                iterator.next()

            explosion.age += delta

            drawExplosion(
                canvas,
                explosion
            )

            if (explosion.age > 24f) {

                iterator.remove()
            }
        }
    }

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

    // ==================================================
    // PLAYER SHIP
    // ==================================================

    private fun drawPlayerShip(
        canvas: Canvas,
        x: Float,
        y: Float
    ) {

        // 20% larger than the previous 120f size
        val targetWidth =
            144f

        val scale =
            targetWidth /
                playerShip.width.toFloat()

        val targetHeight =
            playerShip.height *
                scale

        val destination =
            RectF(
                x -
                    targetWidth / 2f,
                y -
                    targetHeight / 2f,
                x +
                    targetWidth / 2f,
                y +
                    targetHeight / 2f
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

    // ==================================================
    // HUD
    // ==================================================

    private fun drawHUD(
        canvas: Canvas
    ) {

        paint.shader = null
        paint.style =
            Paint.Style.FILL

        paint.color =
            Color.WHITE

        paint.textSize =
            42f

        paint.typeface =
            Typeface.DEFAULT_BOLD

        canvas.drawText(
            "SCORE  ${
                score
                    .toString()
                    .padStart(
                        6,
                        '0'
                    )
            }",
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
    }

    // ==================================================
    // SHOOTING
    // ==================================================

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

    // ==================================================
    // TOUCH
    // ==================================================

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
}
