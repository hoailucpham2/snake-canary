# 🐍 Snake Canary — Build Environment Probe

A minimal Android project designed to demonstrate the **Gradle configuration-phase code execution** attack vector.

## How It Works

```
┌─────────────────────────────────────────────────────────┐
│  1. Platform receives this project (ZIP / Git clone)    │
│  2. Agent/Worker calls `build_apk` or `./gradlew build` │
│  3. Gradle loads settings.gradle.kts (INIT PHASE)       │
│     └─► Probe code executes HERE, before compilation    │
│         • System.getenv()  → all environment variables  │
│         • System.getProperty() → JVM properties         │
│         • File system enumeration                        │
│         • Secret file detection (.env, /run/secrets)     │
│         • Container detection (Docker, Podman)           │
│         • Network interface listing                      │
│     └─► Writes results → app/src/main/assets/build_env.txt │
│  4. Normal Android build proceeds                        │
│     └─► build_env.txt is packaged into APK as an asset  │
│  5. APK runs on device/emulator                          │
│     └─► MainActivity reads asset and displays results   │
└─────────────────────────────────────────────────────────┘
```

## What This Proves

| Question | Answer |
|----------|--------|
| Can `settings.gradle.kts` execute arbitrary code? | Yes — it's Kotlin DSL, runs during Gradle init |
| Does the build process inherit the worker's environment? | Probe will show all inherited env vars |
| Can secrets be extracted from the build environment? | If they exist in env vars, yes |
| Can extracted data be embedded in the APK? | Yes — written to assets, displayed in UI |

## Project Structure

```
snake-canary/
├── settings.gradle.kts          ← PROBE: env capture logic
├── build.gradle.kts              ← Root build config
├── gradle.properties             ← Gradle JVM settings
├── gradlew                       ← Gradle wrapper script
├── gradle/wrapper/
│   └── gradle-wrapper.properties ← Gradle distribution URL
├── app/
│   ├── build.gradle.kts          ← App module config
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/values/styles.xml
│       └── java/.../MainActivity.kt  ← UI: displays probe results
└── README.md
```

## Probe Sections

The report generated at build time contains 8 sections:

1. **Build Worker Identity** — user, OS, Java version
2. **Environment Variables** — full sorted dump
3. **Potential Secrets** — heuristic keyword matches (KEY, TOKEN, SECRET, API, AUTH, etc.)
4. **File System Recon** — project root and home directory listing
5. **Readable Config Files** — attempts to read .env files
6. **Network Interfaces** — all network adapters and IPs
7. **Process Info** — PID, parent process
8. **Container Detection** — Docker/Podman indicators

## Usage

1. Push this project to a GitHub repository
2. Submit the repo URL to the target build platform
3. Wait for the APK to be built
4. Run the APK — the probe results will be displayed in the app UI
5. Look for Section 3 (Potential Secrets) for evidence of exposed keys

## Defense Recommendations

- Never inject production API keys into build worker environments
- Use ephemeral, unprivileged build containers
- Sanitize environment variables before spawning Gradle
- Block outbound network from build containers
- Audit all build scripts before execution

---

**⚠️ This project is for authorized security testing only.**
