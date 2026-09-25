package com.example.snakecanary

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Main activity — shows a simple snake game landing page
 * with an "About / Build Info" section for QA traceability.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1a1a2e"))
        }

        // ── Title ───────────────────────────────────────────
        val titleBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#16213e"))
            setPadding(dp(20), dp(48), dp(20), dp(16))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        titleBar.addView(TextView(this).apply {
            text = "\uD83D\uDC0D Snake Canary"
            setTextColor(Color.parseColor("#e94560"))
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })

        titleBar.addView(TextView(this).apply {
            text = "Build Info & Diagnostics"
            setTextColor(Color.parseColor("#a0a0b0"))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        })

        root.addView(titleBar)

        // ── Content ─────────────────────────────────────────
        val scroll = ScrollView(this).apply {
            setPadding(dp(12), dp(8), dp(12), dp(8))
            isFillViewport = true
        }

        val content = TextView(this).apply {
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setPadding(dp(8), dp(8), dp(8), dp(24))
            setLineSpacing(0f, 1.2f)
        }

        val raw = loadBuildInfo()
        content.text = colorize(raw)

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT, 1f
        ))

        // ── Footer ──────────────────────────────────────────
        root.addView(TextView(this).apply {
            text = "v1.0 • ${raw.lines().size} lines"
            setTextColor(Color.parseColor("#606070"))
            textSize = 11f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0f0f23"))
            setPadding(dp(8), dp(10), dp(8), dp(10))
        })

        setContentView(root)
    }

    private fun loadBuildInfo(): String {
        return try {
            assets.open("build_info.txt").bufferedReader().readText()
        } catch (e: Exception) {
            "Build info not available.\n\n${e.message}"
        }
    }

    private fun colorize(text: String): SpannableString {
        val s = SpannableString(text)
        var offset = 0
        for (line in text.lines()) {
            val end = offset + line.length
            if (end > s.length) break
            when {
                line.startsWith("[") && line.endsWith("]") -> {
                    s.setSpan(ForegroundColorSpan(Color.parseColor("#e94560")),
                        offset, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    s.setSpan(StyleSpan(Typeface.BOLD),
                        offset, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                line.startsWith("---") -> {
                    s.setSpan(ForegroundColorSpan(Color.parseColor("#00ff88")),
                        offset, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                line.contains("=") -> {
                    val eq = line.indexOf('=')
                    s.setSpan(ForegroundColorSpan(Color.parseColor("#80c0e0")),
                        offset, offset + eq, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    s.setSpan(ForegroundColorSpan(Color.parseColor("#d0d0d8")),
                        offset + eq, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                else -> {
                    s.setSpan(ForegroundColorSpan(Color.parseColor("#c0c0c8")),
                        offset, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            offset = end + 1
        }
        return s
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
