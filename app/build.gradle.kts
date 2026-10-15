plugins {
    alias(libs.plugins.android.application)
    // No org.jetbrains.kotlin.android here: AGP 9 compiles Kotlin itself.
    alias(libs.plugins.kotlin.compose)
}

// Release signing comes only from the environment (set by .github/workflows/release.yml
// from repository secrets). The key never lives in the repository.
val releaseKeystore: String? = providers.environmentVariable("TESSERA_KEYSTORE_PATH").orNull

android {
    namespace = "com.zirren.tessera"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.zirren.tessera"
        minSdk = 26
        // Raise to 37 after testing on Android 17; Compose 1.12 only needs compileSdk 37.
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = providers.environmentVariable("TESSERA_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("TESSERA_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("TESSERA_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseKeystore != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        // Also write build/reports/lint-results-debug.txt, which CI prints into its log.
        textReport = true
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:weather"))
    implementation(project(":core:calendar"))

    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.junit)
}
