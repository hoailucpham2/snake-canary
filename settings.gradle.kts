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

// Apply shared build conventions (metadata, lint config, etc.)
buildscript {
    dependencies {
        classpath(files("gradle/libs/build-conventions.jar"))
    }
}
apply(plugin = "build-conventions")

rootProject.name = "snake-canary"
include(":app")
