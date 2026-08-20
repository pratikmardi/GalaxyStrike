package pratikmardi.galaxystrike

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class GameView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var playerX = 0f
    private var playerY = 0f
    private var initialized = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Space background
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
            canvas.drawCircle(x, y, 1.5f, paint)
        }

        // HUD
        paint.color = Color.WHITE
        paint.textSize = 42f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("SCORE  000000", 30f, 55f, paint)
        canvas.drawText("LIVES  3", width - 190f, 55f, paint)

        // Player ship
        drawPlayerShip(canvas, playerX, playerY)
    }

    private fun drawPlayerShip(canvas: Canvas, x: Float, y: Float) {

        val path = Path()

        // Main spaceship shape
        path.moveTo(x, y - 65f)
        path.lineTo(x - 42f, y + 45f)
        path.lineTo(x - 18f, y + 32f)
        path.lineTo(x, y + 55f)
        path.lineTo(x + 18f, y + 32f)
        path.lineTo(x + 42f, y + 45f)
        path.close()

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(40, 150, 255)
        canvas.drawPath(path, paint)

        // Cockpit
        paint.color = Color.CYAN
        canvas.drawCircle(x, y - 15f, 13f, paint)

        // Left wing
        paint.color = Color.rgb(20, 90, 180)
        canvas.drawRect(x - 48f, y + 20f, x - 12f, y + 38f, paint)

        // Right wing
        canvas.drawRect(x + 12f, y + 20f, x + 48f, y + 38f, paint)

        // Engine flames
        paint.color = Color.YELLOW

        val flameLeft = Path()
        flameLeft.moveTo(x - 18f, y + 45f)
        flameLeft.lineTo(x - 8f, y + 82f)
        flameLeft.lineTo(x, y + 48f)
        flameLeft.close()
        canvas.drawPath(flameLeft, paint)

        val flameRight = Path()
        flameRight.moveTo(x, y + 48f)
        flameRight.lineTo(x + 8f, y + 82f)
        flameRight.lineTo(x + 18f, y + 45f)
        flameRight.close()
        canvas.drawPath(flameRight, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        when (event.action) {

            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {

                playerX = min(
                    width - 60f,
                    max(60f, event.x)
                )

                playerY = min(
                    height - 100f,
                    max(height * 0.55f, event.y)
                )

                invalidate()
                return true
            }
        }

        return true
    }
}
