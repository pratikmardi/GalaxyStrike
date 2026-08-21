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
    private var wave = 1

    private var lives = 3
    private var playerInvulnerable = false
    private var invulnerabilityTimer = 0f
    private var gameOver = false

    private var lastSpawnTime = 0L
    private var lastShotTime = 0L
    private var lastBossShotTime = 0L
    private var lastFrameTime = System.currentTimeMillis()

    private var bossActive = false
    private var bossEntering = false
    private var bossDefeatedMessage = 0f

    private var bossX = 0f
    private var bossY = -250f
    private var bossHealth = 100
    private val bossMaxHealth = 100
    private var bossDirection = 1f
    private var bossHitFlash = 0f

    private data class Star(
        var x: Float,
        var y: Float,
        var speed: Float,
        var size: Float,
        var alpha: Int
    )

    private enum class EnemyType {
        SCOUT,
        FIGHTER,
        TANK
    }

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
        var speed: Float,
        var enemyBullet: Boolean = false
    )

    private data class Explosion(
        var x: Float,
        var y: Float,
        var age: Float = 0f,
        var size: Float = 1f
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

    private val playerShip: Bitmap =
        BitmapFactory.decodeResource(
            resources,
            resources.getIdentifier(
                "player_ship",
                "drawable",
                context.packageName
            )
        )

    private val bossShip: Bitmap =
        BitmapFactory.decodeResource(
            resources,
            resources.getIdentifier(
                "boss_ship",
                "drawable",
                context.packageName
            )
        )

    init {

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

        if (playerInvulnerable) {
    invulnerabilityTimer -= delta

    if (invulnerabilityTimer <= 0f) {
        playerInvulnerable = false
        invulnerabilityTimer = 0f
    }
        }
        

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

        updateBossMessage(delta)

        drawHUD(canvas)

        // -----------------------------
        // BOSS SYSTEM
        // -----------------------------

        if (!bossActive && score >= wave * 3000) {

            startBoss()
        }

        if (bossActive) {

            updateBoss(
                canvas,
                delta,
                currentTime
            )

        } else {

            val difficulty =
                1f +
                    (score / 3000f)
                        .coerceAtMost(3f)

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

            updateEnemies(
                canvas,
                delta
            )
        }

        updateBullets(
            canvas,
            delta
        )

        handleCollisions()

        updateExplosions(
            canvas,
            delta
        )

        if (!gameOver) {

    drawPlayerShip(
        canvas,
        playerX,
        playerY
    )

} else {

    drawGameOver(
        canvas
    )
}

postInvalidateOnAnimation()
    }

    // =================================================
    // BACKGROUND
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
            lastBossShotTime > 5000) {

            if (Random.nextFloat() < 0.35f) {

                shootingStars.add(
                    ShootingStar(
                        x = Random.nextFloat() * width,
                        y = Random.nextFloat() *
                            height * 0.45f,
                        speed = Random.nextFloat() *
                            12f + 10f,
                        length = Random.nextFloat() *
                            70f + 50f
                    )
                )
            }

            lastBossShotTime =
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
                star.x - star.length,
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
    // BOSS START
    // =================================================

    private fun startBoss() {

        bossActive = true
        bossEntering = true

        bossX = width / 2f
        bossY = -250f

        bossHealth = bossMaxHealth

        bossDirection = 1f

        enemies.clear()
        bullets.removeAll {
            it.enemyBullet
        }
    }

    // =================================================
    // BOSS UPDATE
    // =================================================

    private fun updateBoss(
        canvas: Canvas,
        delta: Float,
        currentTime: Long
    ) {

        if (bossEntering) {

            bossY +=
                5f * delta

            if (bossY >= 170f) {

                bossY = 170f
                bossEntering = false
            }

        } else {

            bossX +=
                bossDirection *
                    4f *
                    delta

            if (bossX > width - 180f) {

                bossDirection = -1f
            }

            if (bossX < 180f) {

                bossDirection = 1f
            }

            // Boss firing

            if (currentTime -
                lastBossShotTime > 850) {

                bullets.add(
                    Bullet(
                        x = bossX,
                        y = bossY + 90f,
                        speed = -9f,
                        enemyBullet = true
                    )
                )

                lastBossShotTime =
                    currentTime
            }
        }

        if (bossHitFlash > 0f) {

            bossHitFlash -= delta
        }

        drawBoss(
            canvas
        )

        drawBossHealthBar(
            canvas
        )

        if (bossEntering) {

            drawBossWarning(
                canvas
            )
        }

        if (bossDefeatedMessage > 0f) {

            bossDefeatedMessage -= delta

            drawBossDefeated(
                canvas
            )
        }
    }

    // =================================================
    // DRAW BOSS
    // =================================================

    private fun drawBoss(
        canvas: Canvas
    ) {

        val bossWidth = 300f

        val scale =
            bossWidth /
                bossShip.width.toFloat()

        val bossHeight =
            bossShip.height *
                scale

        val destination =
            RectF(
                bossX -
                    bossWidth / 2f,
                bossY -
                    bossHeight / 2f,
                bossX +
                    bossWidth / 2f,
                bossY +
                    bossHeight / 2f
            )

        paint.alpha = 255
        paint.isFilterBitmap = true

        canvas.drawBitmap(
            bossShip,
            null,
            destination,
            paint
        )

        if (bossHitFlash > 0f) {

            paint.color =
                Color.argb(
                    130,
                    255,
                    255,
                    255
                )

            canvas.drawCircle(
                bossX,
                bossY,
                150f,
                paint
            )
        }
    }

    // =================================================
    // BOSS HEALTH BAR
    // =================================================

    private fun drawBossHealthBar(
        canvas: Canvas
    ) {

        if (bossEntering) {
            return
        }

        val barWidth =
            width * 0.72f

        val left =
            (width - barWidth) / 2f

        val top = 95f

        paint.color =
            Color.DKGRAY

        canvas.drawRoundRect(
            left,
            top,
            left + barWidth,
            top + 22f,
            10f,
            10f,
            paint
        )

        val health =
            bossHealth.toFloat() /
                bossMaxHealth

        paint.color =
            Color.RED

        canvas.drawRoundRect(
            left,
            top,
            left +
                barWidth *
                health,
            top + 22f,
            10f,
            10f,
            paint
        )

        paint.color =
            Color.WHITE

        paint.textSize = 28f
        paint.typeface =
            Typeface.DEFAULT_BOLD

        paint.textAlign =
            Paint.Align.CENTER

        canvas.drawText(
            "BOSS",
            width / 2f,
            top - 10f,
            paint
        )

        paint.textAlign =
            Paint.Align.LEFT
    }

    // =================================================
    // BOSS WARNING
    // =================================================

    private fun drawBossWarning(
        canvas: Canvas
    ) {

        paint.color =
            Color.argb(
                180,
                120,
                0,
                0
            )

        canvas.drawRect(
            0f,
            height * 0.42f,
            width.toFloat(),
            height * 0.58f,
            paint
        )

        paint.color =
            Color.WHITE

        paint.textSize = 52f
        paint.typeface =
            Typeface.DEFAULT_BOLD

        paint.textAlign =
            Paint.Align.CENTER

        canvas.drawText(
            "⚠ BOSS INCOMING ⚠",
            width / 2f,
            height * 0.52f,
            paint
        )

        paint.textAlign =
            Paint.Align.LEFT
    }

    // =================================================
    // BOSS DEFEATED
    // =================================================

    private fun drawBossDefeated(
        canvas: Canvas
    ) {

        paint.color =
            Color.WHITE

        paint.textSize = 48f
        paint.typeface =
            Typeface.DEFAULT_BOLD

        paint.textAlign =
            Paint.Align.CENTER

        canvas.drawText(
            "BOSS DESTROYED!",
            width / 2f,
            height * 0.48f,
            paint
        )

        paint.textSize = 32f

        canvas.drawText(
            "+5000",
            width / 2f,
            height * 0.54f,
            paint
        )

        paint.textAlign =
            Paint.Align.LEFT
    }

    // =================================================
    // ENEMIES
    // =================================================

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

        val type =
            if (roll < 0.55f) {

                EnemyType.SCOUT

            } else if (roll < 0.88f) {

                EnemyType.FIGHTER

            } else {

                EnemyType.TANK
            }

        val health: Int
        val speed: Float

        when (type) {

            EnemyType.SCOUT -> {

                health = 1

                speed =
                    Random.nextFloat() *
                        3f + 7f
            }

            EnemyType.FIGHTER -> {

                health = 2

                speed =
                    Random.nextFloat() *
                        2.5f + 4.5f
            }

            EnemyType.TANK -> {

                health = 4

                speed =
                    Random.nextFloat() *
                        1.5f + 2.5f
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
                enemy.speed * delta

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

    // =================================================
    // DRAW ENEMY
    // =================================================

    private fun drawEnemy(
        canvas: Canvas,
        enemy: Enemy
    ) {

        val x = enemy.x
        val y = enemy.y

        paint.style =
            Paint.Style.FILL

        when (enemy.type) {

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

        if (enemy.hitFlash > 0f) {

            paint.color =
                Color.WHITE

            canvas.drawCircle(
                x,
                y,
                45f,
                paint
            )
        }

        if (enemy.health <
            enemy.maxHealth) {

            val barWidth =
                when (enemy.type) {

                    EnemyType.SCOUT -> 50f
                    EnemyType.FIGHTER -> 65f
                    EnemyType.TANK -> 95f
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

    // =================================================
    // BULLETS
    // =================================================

    private fun updateBullets(
        canvas: Canvas,
        delta: Float
    ) {

        val iterator =
            bullets.iterator()

        while (iterator.hasNext()) {

            val bullet =
                iterator.next()

            if (bullet.enemyBullet) {

                bullet.y -=
                    bullet.speed * delta

                drawEnemyBullet(
                    canvas,
                    bullet.x,
                    bullet.y
                )

                if (bullet.y >
                    height + 50f) {

                    iterator.remove()
                }

            } else {

                bullet.y -=
                    bullet.speed * delta

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
    }

    private fun drawBullet(
        canvas: Canvas,
        x: Float,
        y: Float
    ) {

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

    private fun drawEnemyBullet(
        canvas: Canvas,
        x: Float,
        y: Float
    ) {

        paint.color =
            Color.argb(
                80,
                255,
                30,
                30
            )

        canvas.drawCircle(
            x,
            y,
            16f,
            paint
        )

        paint.color =
            Color.RED

        canvas.drawCircle(
            x,
            y,
            7f,
            paint
        )
    }

    // =================================================
    // COLLISIONS
    // =================================================

    private fun damagePlayer() {

    if (playerInvulnerable || gameOver) {
        return
    }

    lives--

    playerInvulnerable = true
    invulnerabilityTimer = 90f

    explosions.add(
        Explosion(
            playerX,
            playerY,
            size = 1.2f
        )
    )

    if (lives <= 0) {

        lives = 0
        gameOver = true

        bullets.clear()
        enemies.clear()
        bossActive = false
    }
    }

    private fun handleCollisions() {

        // Boss bullet / player collision

val enemyBulletsToRemove =
    mutableListOf<Bullet>()

for (bullet in bullets) {

    if (!bullet.enemyBullet) {
        continue
    }

    val dx =
        playerX - bullet.x

    val dy =
        playerY - bullet.y

    val distanceSquared =
        dx * dx + dy * dy

    if (distanceSquared < 70f * 70f) {

        damagePlayer()

        enemyBulletsToRemove.add(
            bullet
        )
    }
}

bullets.removeAll(
    enemyBulletsToRemove
)

        val bulletsToRemove =
            mutableListOf<Bullet>()

        val enemiesToRemove =
            mutableListOf<Enemy>()

        for (bullet in bullets) {

            if (bullet.enemyBullet) {
                continue
            }

            // Boss collision

            if (bossActive &&
                !bossEntering) {

                val dx =
                    bullet.x -
                        bossX

                val dy =
                    bullet.y -
                        bossY

                if (dx * dx +
                    dy * dy <
                    150f * 150f) {

                    bulletsToRemove.add(
                        bullet
                    )

                    bossHealth--

                    bossHitFlash =
                        4f

                    if (bossHealth <= 0) {

                        destroyBoss()
                    }

                    continue
                }
            }

            // Normal enemies

            for (enemy in enemies) {

                val dx =
                    bullet.x -
                        enemy.x

                val dy =
                    bullet.y -
                        enemy.y

                val radius =
                    when (enemy.type) {

                        EnemyType.SCOUT -> 45f
                        EnemyType.FIGHTER -> 55f
                        EnemyType.TANK -> 70f
                    }

                if (dx * dx +
                    dy * dy <
                    radius * radius) {

                    bulletsToRemove.add(
                        bullet
                    )

                    enemy.health--
                    enemy.hitFlash = 4f

                    if (enemy.health <= 0) {

                        enemiesToRemove.add(
                            enemy
                        )

                        val reward =
                            when (enemy.type) {

                                EnemyType.SCOUT -> 100
                                EnemyType.FIGHTER -> 250
                                EnemyType.TANK -> 600
                            }

                        score += reward

                        explosions.add(
                            Explosion(
                                enemy.x,
                                enemy.y,
                                size = 1f
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
        // Player / enemy collision

val enemiesToRemoveOnPlayerHit =
    mutableListOf<Enemy>()

for (enemy in enemies) {

    val dx =
        playerX - enemy.x

    val dy =
        playerY - enemy.y

    val distanceSquared =
        dx * dx + dy * dy

    if (distanceSquared < 95f * 95f) {

        damagePlayer()

        enemiesToRemoveOnPlayerHit.add(enemy)

        explosions.add(
            Explosion(
                enemy.x,
                enemy.y,
                size = 1.0f
            )
        )
    }
}

enemies.removeAll(
    enemiesToRemoveOnPlayerHit
)
    }

    // =================================================
    // DESTROY BOSS
    // =================================================

    private fun destroyBoss() {

        bossActive = false
        bossEntering = false

        score += 5000

        wave++

        bossDefeatedMessage =
            120f

        repeat(8) {

            explosions.add(
                Explosion(
                    bossX +
                        Random.nextFloat() *
                        300f -
                        150f,
                    bossY +
                        Random.nextFloat() *
                        200f -
                        100f,
                    size =
                        Random.nextFloat() *
                            1.5f +
                            1f
                )
            )
        }
    }

    // =================================================
    // EXPLOSIONS
    // =================================================

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

            if (explosion.age > 28f) {

                iterator.remove()
            }
        }
    }

    private fun drawExplosion(
        canvas: Canvas,
        explosion: Explosion
    ) {

        val progress =
            explosion.age / 28f

        val radius =
            (
                10f +
                    progress * 80f
                ) *
                explosion.size

        val alpha =
            ((1f - progress) *
                255f)
                .toInt()
                .coerceIn(
                    0,
                    255
                )

        paint.color =
            Color.argb(
                alpha / 3,
                255,
                60,
                0
            )

        canvas.drawCircle(
            explosion.x,
            explosion.y,
            radius * 1.6f,
            paint
        )

        paint.color =
            Color.argb(
                alpha,
                255,
                90,
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
                40
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
    // PLAYER
    // =================================================

    private fun drawPlayerShip(
        canvas: Canvas,
        x: Float,
        y: Float
    ) {

        val targetWidth = 144f

        val scale =
            targetWidth /
                playerShip.width.toFloat()

        val targetHeight =
            playerShip.height * scale

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

        private fun drawGameOver(
    canvas: Canvas
) {

    paint.color =
        Color.argb(
            190,
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

    paint.color =
        Color.RED

    paint.textSize = 64f
    paint.typeface =
        Typeface.DEFAULT_BOLD

    paint.textAlign =
        Paint.Align.CENTER

    canvas.drawText(
        "GAME OVER",
        width / 2f,
        height * 0.42f,
        paint
    )

    paint.color =
        Color.WHITE

    paint.textSize = 38f

    canvas.drawText(
        "SCORE  ${
            score
                .toString()
                .padStart(
                    6,
                    '0'
                )
        }",
        width / 2f,
        height * 0.50f,
        paint
    )

    paint.textSize = 32f

    canvas.drawText(
        "TAP TO RESTART",
        width / 2f,
        height * 0.60f,
        paint
    )

    paint.textAlign =
        Paint.Align.LEFT
        
        
    }

    // =================================================
    // HUD
    // =================================================

    private fun drawHUD(
        canvas: Canvas
    ) {

        paint.shader = null
        paint.color = Color.WHITE
        paint.textSize = 42f
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
            "LIVES  $lives",
            width - 190f,
            55f,
            paint
        )

        paint.textSize = 28f

        canvas.drawText(
            "WAVE $wave",
            30f,
            90f,
            paint
        )
    }

    // =================================================
    // BOSS MESSAGE
    // =================================================

    private fun updateBossMessage(
        delta: Float
    ) {

        if (bossDefeatedMessage > 0f) {

            bossDefeatedMessage -= delta
        }
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
                x = playerX,
                y = playerY - 65f,
                speed = 18f
            )
        )
    }

    // =================================================
    // TOUCH
    // =================================================

private fun restartGame() {

    score = 0
    wave = 1
    lives = 3

    gameOver = false

    playerInvulnerable = false
    invulnerabilityTimer = 0f

    bossActive = false
    bossEntering = false
    bossHealth = bossMaxHealth

    enemies.clear()
    bullets.clear()
    explosions.clear()
    shootingStars.clear()

    playerX = width / 2f
    playerY = height * 0.82f

    lastSpawnTime =
        System.currentTimeMillis()

    lastShotTime = 0L
    lastBossShotTime = 0L
}
    
    override fun onTouchEvent(
        event: MotionEvent
    ): Boolean {

        when (event.action) {
            if (gameOver &&
    event.action == MotionEvent.ACTION_DOWN) {

    restartGame()

    invalidate()

    return true
            }

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
