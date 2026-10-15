// Top-level build file. Modules apply what they need.
//
// AGP 9 compiles Kotlin itself ("built-in Kotlin"), so Android modules must NOT apply
// org.jetbrains.kotlin.android. Declaring the Kotlin JVM plugin here also pins the
// Kotlin Gradle plugin version that AGP's built-in Kotlin uses.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
