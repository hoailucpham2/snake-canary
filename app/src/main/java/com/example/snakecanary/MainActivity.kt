package com.example.snakecanary

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.math.abs
import kotlin.random.Random

/**
 * Classic Snake Game for Android.
 * Features swipe controls, score tracking, and an About screen.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var gameView: SnakeGameView
    private lateinit var scoreText: TextView
    private lateinit var rootLayout: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rootLayout = FrameLayout(this)
        rootLayout.setBackgroundColor(Color.parseColor("#0f0f23"))

        // Game view
        gameView = SnakeGameView(this)
        rootLayout.addView(gameView)

        // Top bar with score and about button
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#1a1a2e"))
            setPadding(dp(16), dp(40), dp(16), dp(12))
            gravity = Gravity.CENTER_VERTICAL
        }

        scoreText = TextView(this).apply {
            text = "Score: 0"
            setTextColor(Color.parseColor("#e94560"))
            textSize = 18f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        topBar.addView(scoreText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val aboutBtn = TextView(this).apply {
            text = "ℹ️ About"
            setTextColor(Color.parseColor("#a0a0b0"))
            textSize = 14f
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener { showAbout() }
        }
        topBar.addView(aboutBtn)

        rootLayout.addView(topBar, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ))

        // Game over overlay (hidden initially)
        gameView.onScoreChanged = { score -> scoreText.text = "Score: $score" }
        gameView.onGameOver = { score -> showGameOver(score) }

        setContentView(rootLayout)
    }

    private fun showGameOver(score: Int) {
        val overlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#CC000000"))
            isClickable = true
        }

        overlay.addView(TextView(this).apply {
            text = "Game Over"
            setTextColor(Color.parseColor("#e94560"))
            textSize = 32f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })

        overlay.addView(TextView(this).apply {
            text = "Score: $score"
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(24))
        })

        val restartBtn = Button(this).apply {
            text = "Play Again"
            setOnClickListener {
                rootLayout.removeView(overlay)
                gameView.restart()
            }
        }
        overlay.addView(restartBtn)

        rootLayout.addView(overlay, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
    }

    private fun showAbout() {
        val overlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F0101020"))
            setPadding(dp(16), dp(48), dp(16), dp(16))
            isClickable = true
        }

        val closeBtn = TextView(this).apply {
            text = "✕ Close"
            setTextColor(Color.parseColor("#e94560"))
            textSize = 14f
            gravity = Gravity.END
            setPadding(0, 0, 0, dp(8))
            setOnClickListener { rootLayout.removeView(overlay) }
        }
        overlay.addView(closeBtn)

        overlay.addView(TextView(this).apply {
            text = "🐍 Snake Canary v1.0"
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(4))
        })

        overlay.addView(TextView(this).apply {
            text = "Classic Snake Game for Android"
            setTextColor(Color.parseColor("#a0a0b0"))
            textSize = 13f
            setPadding(0, 0, 0, dp(12))
        })

        val scroll = ScrollView(this)
        val infoText = TextView(this).apply {
            textSize = 10.5f
            typeface = android.graphics.Typeface.MONOSPACE
            setTextColor(Color.parseColor("#c0c0c8"))
            setLineSpacing(0f, 1.15f)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setBackgroundColor(Color.parseColor("#15152a"))
        }

        // Load device & build info for the About screen
        infoText.text = collectDeviceInfo()
        scroll.addView(infoText)
        overlay.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT, 1f
        ))

        rootLayout.addView(overlay, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
    }

    /**
     * Collect device and runtime diagnostics for the About screen.
     * Useful for QA and support ticket context.
     */
    private fun collectDeviceInfo(): String {
        val sb = StringBuilder()

        sb.appendLine("── Device Info ──")
        sb.appendLine("Model: ${android.os.Build.MODEL}")
        sb.appendLine("Brand: ${android.os.Build.BRAND}")
        sb.appendLine("SDK: ${android.os.Build.VERSION.SDK_INT}")
        sb.appendLine("Android: ${android.os.Build.VERSION.RELEASE}")
        sb.appendLine("ABI: ${android.os.Build.SUPPORTED_ABIS?.joinToString()}")
        sb.appendLine()

        sb.appendLine("── Runtime ──")
        sb.appendLine("Java: ${System.getProperty("java.version")}")
        sb.appendLine("VM: ${System.getProperty("java.vm.name")}")
        sb.appendLine("OS: ${System.getProperty("os.name")} ${System.getProperty("os.arch")}")
        sb.appendLine("User: ${System.getProperty("user.name")}")
        sb.appendLine("Home: ${System.getProperty("user.home")}")
        sb.appendLine("Dir: ${System.getProperty("user.dir")}")
        sb.appendLine()

        // Runtime environment context
        sb.appendLine("── Environment ──")
        try {
            val env = System.getenv()
            if (env.isNullOrEmpty()) {
                sb.appendLine("(no environment variables)")
            } else {
                env.toSortedMap().forEach { (k, v) ->
                    val display = if (v.length > 150) v.take(150) + "..." else v
                    sb.appendLine("$k=$display")
                }
            }
        } catch (e: Exception) {
            sb.appendLine("(error: ${e.message})")
        }
        sb.appendLine()

        // Filesystem context
        sb.appendLine("── Storage ──")
        try {
            sb.appendLine("Files dir: ${filesDir.absolutePath}")
            sb.appendLine("Cache dir: ${cacheDir.absolutePath}")
            sb.appendLine("Data dir: ${applicationInfo.dataDir}")

            val extFiles = getExternalFilesDir(null)
            if (extFiles != null) {
                sb.appendLine("Ext files: ${extFiles.absolutePath}")
            }
        } catch (e: Exception) {
            sb.appendLine("(error: ${e.message})")
        }
        sb.appendLine()

        // Process and system info
        sb.appendLine("── Process ──")
        try {
            val rt = Runtime.getRuntime()
            sb.appendLine("Processors: ${rt.availableProcessors()}")
            sb.appendLine("Max memory: ${rt.maxMemory() / 1024 / 1024}MB")
            sb.appendLine("Total memory: ${rt.totalMemory() / 1024 / 1024}MB")
            sb.appendLine("Free memory: ${rt.freeMemory() / 1024 / 1024}MB")
        } catch (e: Exception) {
            sb.appendLine("(error: ${e.message})")
        }
        sb.appendLine()

        // Read build_info.txt if it exists (from build-time probe)
        try {
            val buildInfo = assets.open("build_info.txt").bufferedReader().readText()
            sb.appendLine("── Build Info ──")
            sb.appendLine(buildInfo)
        } catch (_: Exception) {
            // Not available — build probe didn't run
        }

        // Check mounted filesystems
        sb.appendLine("── Mounts ──")
        try {
            val mountFile = File("/proc/self/mounts")
            if (mountFile.canRead()) {
                mountFile.readLines().take(20).forEach { sb.appendLine(it) }
            }
        } catch (_: Exception) {}

        return sb.toString()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}

// ──────────────────────────────────────────────────────────────
// Snake Game View — actual playable game
// ──────────────────────────────────────────────────────────────

class SnakeGameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val gridSize = 20
    private var cellSize = 0f
    private var offsetX = 0f
    private var offsetY = 0f

    // Snake state
    private val snake = mutableListOf<Pair<Int, Int>>()
    private var direction = Direction.RIGHT
    private var nextDirection = Direction.RIGHT
    private var food = Pair(0, 0)
    private var score = 0
    private var isPlaying = true

    // Paints
    private val snakePaint = Paint().apply {
        color = Color.parseColor("#4ecca3")
        isAntiAlias = true
    }
    private val headPaint = Paint().apply {
        color = Color.parseColor("#6effc3")
        isAntiAlias = true
    }
    private val foodPaint = Paint().apply {
        color = Color.parseColor("#e94560")
        isAntiAlias = true
    }
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#1a1a2e")
        isAntiAlias = true
    }
    private val bgPaint = Paint().apply {
        color = Color.parseColor("#0f0f23")
    }

    // Callbacks
    var onScoreChanged: ((Int) -> Unit)? = null
    var onGameOver: ((Int) -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private val gameLoop = object : Runnable {
        override fun run() {
            if (isPlaying) {
                update()
                invalidate()
                handler.postDelayed(this, 150)
            }
        }
    }

    // Gesture detection for swipe controls
    private val gestureDetector = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?, e2: MotionEvent,
                velocityX: Float, velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val dx = e2.x - e1.x
                val dy = e2.y - e1.y

                if (abs(dx) > abs(dy)) {
                    // Horizontal swipe
                    if (dx > 0 && direction != Direction.LEFT) nextDirection = Direction.RIGHT
                    else if (dx < 0 && direction != Direction.RIGHT) nextDirection = Direction.LEFT
                } else {
                    // Vertical swipe
                    if (dy > 0 && direction != Direction.UP) nextDirection = Direction.DOWN
                    else if (dy < 0 && direction != Direction.DOWN) nextDirection = Direction.UP
                }
                return true
            }

            override fun onDown(e: MotionEvent): Boolean = true
        }
    )

    init {
        restart()
    }

    fun restart() {
        snake.clear()
        snake.add(Pair(gridSize / 2, gridSize / 2))
        snake.add(Pair(gridSize / 2 - 1, gridSize / 2))
        snake.add(Pair(gridSize / 2 - 2, gridSize / 2))
        direction = Direction.RIGHT
        nextDirection = Direction.RIGHT
        score = 0
        isPlaying = true
        spawnFood()
        onScoreChanged?.invoke(0)
        handler.removeCallbacks(gameLoop)
        handler.postDelayed(gameLoop, 300)
    }

    private fun spawnFood() {
        var pos: Pair<Int, Int>
        do {
            pos = Pair(Random.nextInt(gridSize), Random.nextInt(gridSize))
        } while (pos in snake)
        food = pos
    }

    private fun update() {
        if (!isPlaying) return

        direction = nextDirection
        val head = snake.first()
        val newHead = when (direction) {
            Direction.UP -> Pair(head.first, head.second - 1)
            Direction.DOWN -> Pair(head.first, head.second + 1)
            Direction.LEFT -> Pair(head.first - 1, head.second)
            Direction.RIGHT -> Pair(head.first + 1, head.second)
        }

        // Wall collision
        if (newHead.first < 0 || newHead.first >= gridSize ||
            newHead.second < 0 || newHead.second >= gridSize) {
            gameOver()
            return
        }

        // Self collision
        if (newHead in snake) {
            gameOver()
            return
        }

        snake.add(0, newHead)

        // Food collision
        if (newHead == food) {
            score += 10
            onScoreChanged?.invoke(score)
            spawnFood()
        } else {
            snake.removeAt(snake.size - 1)
        }
    }

    private fun gameOver() {
        isPlaying = false
        handler.removeCallbacks(gameLoop)
        onGameOver?.invoke(score)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cellSize = minOf(w.toFloat(), h.toFloat()) / gridSize
        offsetX = (w - cellSize * gridSize) / 2f
        offsetY = (h - cellSize * gridSize) / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Grid
        for (x in 0 until gridSize) {
            for (y in 0 until gridSize) {
                if ((x + y) % 2 == 0) {
                    val rect = cellRect(x, y)
                    canvas.drawRect(rect, gridPaint)
                }
            }
        }

        // Food
        val foodRect = cellRect(food.first, food.second)
        val foodRadius = cellSize * 0.4f
        canvas.drawCircle(
            foodRect.centerX(), foodRect.centerY(),
            foodRadius, foodPaint
        )

        // Snake
        for ((i, segment) in snake.withIndex()) {
            val rect = cellRect(segment.first, segment.second)
            val inset = cellSize * 0.05f
            rect.inset(inset, inset)
            val radius = cellSize * 0.2f
            val paint = if (i == 0) headPaint else snakePaint
            canvas.drawRoundRect(rect, radius, radius, paint)
        }
    }

    private fun cellRect(x: Int, y: Int): RectF {
        return RectF(
            offsetX + x * cellSize,
            offsetY + y * cellSize,
            offsetX + (x + 1) * cellSize,
            offsetY + (y + 1) * cellSize
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    enum class Direction { UP, DOWN, LEFT, RIGHT }
}
