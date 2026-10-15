import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Plain Kotlin: the logic behind Tessera's small offline tools (calculator, timer, tip split…).
plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    // Number formatting and unit conversion live in core/model and appear in this module's API.
    api(project(":core:model"))
    testImplementation(libs.junit)
}
