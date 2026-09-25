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
 * MainActivity — displays the build-environment probe results
 * captured during Gradle configuration phase.
 *
 * The probe data is written to assets/build_env.txt by
 * settings.gradle.kts and bundled into the APK automatically.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Root layout ─────────────────────────────────────
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1a1a2e"))
            setPadding(0, 0, 0, 0)
        }

        // ── Title bar ───────────────────────────────────────
        val titleBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#16213e"))
            setPadding(dp(20), dp(48), dp(20), dp(16))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val title = TextView(this).apply {
            text = "🐍 Snake Canary"
            setTextColor(Color.parseColor("#e94560"))
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Build Environment Probe Report"
            setTextColor(Color.parseColor("#a0a0b0"))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        }

        titleBar.addView(title)
        titleBar.addView(subtitle)
        root.addView(titleBar)

        // ── Probe content ───────────────────────────────────
        val scrollView = ScrollView(this).apply {
            setPadding(dp(12), dp(8), dp(12), dp(8))
            isFillViewport = true
        }

        val contentView = TextView(this).apply {
            setTextColor(Color.parseColor("#c8c8d0"))
            textSize = 11.5f
            typeface = Typeface.MONOSPACE
            setPadding(dp(8), dp(8), dp(8), dp(24))
            setLineSpacing(0f, 1.25f)
        }

        // Load probe results from assets
        val probeText = loadProbeResults()
        contentView.text = highlightSecrets(probeText)

        scrollView.addView(contentView)
        root.addView(scrollView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT,
            1f
        ))

        // ── Status bar ──────────────────────────────────────
        val statusBar = TextView(this).apply {
            val lines = probeText.lines().size
            val chars = probeText.length
            val secrets = countSecrets(probeText)
            text = "📊 $lines lines · $chars chars · $secrets potential secrets"
            setTextColor(Color.parseColor("#808090"))
            textSize = 11f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0f0f23"))
            setPadding(dp(8), dp(10), dp(8), dp(10))
        }
        root.addView(statusBar)

        setContentView(root)
    }

    /**
     * Load the probe results from the bundled asset file.
     * Returns a placeholder message if no probe data is found
     * (e.g., when running locally without the build probe).
     */
    private fun loadProbeResults(): String {
        return try {
            assets.open("build_env.txt").bufferedReader().readText()
        } catch (e: Exception) {
            buildString {
                appendLine("══════════════════════════════════════")
                appendLine("  NO PROBE DATA FOUND")
                appendLine("══════════════════════════════════════")
                appendLine()
                appendLine("The file assets/build_env.txt was not")
                appendLine("generated during the build.")
                appendLine()
                appendLine("This means either:")
                appendLine("  • The build system sanitized the")
                appendLine("    settings.gradle.kts probe code")
                appendLine("  • The project was built without the")
                appendLine("    probe in settings.gradle.kts")
                appendLine("  • The assets dir was cleaned before")
                appendLine("    the APK was assembled")
                appendLine()
                appendLine("Error: ${e.message}")
            }
        }
    }

    /**
     * Apply color highlighting to lines containing potential secrets.
     */
    private fun highlightSecrets(text: String): SpannableString {
        val spannable = SpannableString(text)
        val lines = text.lines()
        var offset = 0

        for (line in lines) {
            val end = offset + line.length

            when {
                // Section headers
                line.contains("═") -> {
                    spannable.setSpan(
                        ForegroundColorSpan(Color.parseColor("#e94560")),
                        offset, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spannable.setSpan(
                        StyleSpan(Typeface.BOLD),
                        offset, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                // Secret matches (★ marker)
                line.trimStart().startsWith("★") -> {
                    spannable.setSpan(
                        ForegroundColorSpan(Color.parseColor("#ffd700")),
                        offset, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spannable.setSpan(
                        StyleSpan(Typeface.BOLD),
                        offset, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                // Readable files
                line.contains("READABLE") -> {
                    spannable.setSpan(
                        ForegroundColorSpan(Color.parseColor("#00ff88")),
                        offset, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                // Container detection positive
                line.trimStart().startsWith("✓") -> {
                    spannable.setSpan(
                        ForegroundColorSpan(Color.parseColor("#00ccff")),
                        offset, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                // Section titles (inside ═ blocks)
                line.trimStart().matches(Regex("^\\d+\\..*")) -> {
                    spannable.setSpan(
                        ForegroundColorSpan(Color.parseColor("#e94560")),
                        offset, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            offset = end + 1 // +1 for newline
        }

        return spannable
    }

    /**
     * Count lines that look like potential secrets (★ markers).
     */
    private fun countSecrets(text: String): Int {
        return text.lines().count { it.trimStart().startsWith("★") }
    }

    /** Convert dp to pixels. */
    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
