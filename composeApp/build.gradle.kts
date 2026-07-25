plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    // Sets the JDK toolchain once for every Kotlin compile task (Android + iOS), instead of
    // separately via androidTarget's compilerOptions.jvmTarget and AGP's own
    // compileOptions.sourceCompatibility/targetCompatibility - one source of truth instead
    // of three settings that could drift out of sync.
    jvmToolchain(11)

    androidTarget()

    // iosX64 (Intel simulator) is intentionally omitted - Compose Multiplatform 1.11.1
    // doesn't publish artifacts for it (Apple Silicon has been the default Mac for years).
    // Real devices build against iosArm64; the simulator builds against
    // iosSimulatorArm64, which together cover every Mac anyone would realistically use.
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }
    }
}

android {
    namespace = "com.dj.photobooth"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.dj.photobooth"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            // Minification is off for now, so these rules are inert today - but wiring
            // them in now means Phase 5 (store prep) can flip isMinifyEnabled = true
            // without also having to remember to add this scaffold at the same time.
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}
