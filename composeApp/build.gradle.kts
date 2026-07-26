plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    // Phase 3: Room KMP (local history, uncapped per CLAUDE.md) needs its DAO/database
    // codegen, hence KSP; the androidx.room plugin itself just wires schema export.
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

// Room KMP schema export - kept out of version control noise by pointing at a schemas/
// directory (git-ignored below isn't required, but exporting is what lets Room warn about
// destructive migrations instead of silently dropping data on a future version bump).
room {
    schemaDirectory("$projectDir/schemas")
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
            // CameraX: core (use-case framework) + camera2 (the Camera2-backed implementation
            // it needs at runtime) + lifecycle (bindToLifecycle, so the camera session follows
            // the Activity/lifecycle owner automatically) + view (PreviewView, the Android View
            // we host in a Compose AndroidView for the live preview surface).
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.kotlinx.coroutines.core)
            // MVVM: CaptureViewModel extends androidx.lifecycle.ViewModel (multiplatform
            // artifact) so it gets real lifecycle-scoped survival semantics (survives
            // recomposition and, on Android, configuration changes) instead of a plain class
            // that only survives via `remember`.
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            // Phase 3: Room KMP (local history) + its bundled SQLite driver, so the same
            // GalleryRepo/AppDatabase code runs on both targets without a platform-supplied
            // SQLite (Android's framework one isn't available on iOS, so Room KMP always
            // needs an explicit driver - bundled is the simplest cross-platform choice).
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            // Date-stamp formatting for HistoryEntry.stamp / gallery cards (design/handoff/
            // README.md's "JUL 25, 2026" format) - kotlin.time has no locale-aware month-name
            // formatter, and hand-rolling one duplicates what this library already does well.
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        // Robolectric is Android/JVM-only tooling (it can't compile for iOS targets), so
        // tests needing real Bitmap/Canvas rendering behavior - not available in the plain
        // Android unit-test stub jar, see StripCompositorTest's move here for why - live in
        // this Android-specific test source set instead of commonTest.
        androidUnitTest.dependencies {
            implementation(libs.robolectric)
            // ApplicationProvider.getApplicationContext() - GalleryRepoTest needs a real
            // Context to build an in-memory Room database.
            implementation(libs.androidx.test.core)
            // Room's bundled SQLite driver (used in production, see AndroidAppDatabaseFactory)
            // ships its native library as an Android-ABI .so meant to load from an APK's
            // jniLibs - it can't load on a plain Windows/Linux/macOS JVM, which is what
            // Robolectric tests actually run on (UnsatisfiedLinkError: no sqliteJni). The
            // framework driver instead routes through android.database.sqlite, which
            // Robolectric already shadows with a real native SQLite implementation for
            // exactly this purpose - so tests use AndroidSQLiteDriver, production code
            // doesn't change.
            implementation(libs.androidx.sqlite.framework)
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
    testOptions {
        unitTests {
            // Robolectric needs this to load the real Android resource/graphics stack
            // (Bitmap/Canvas) instead of the stub jar's "not mocked" placeholders.
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
    // Room KMP's DAO/database implementations are codegen'd per-target, not shared - each
    // target needs its own KSP compiler invocation even though the @Entity/@Dao/@Database
    // *declarations* they read live once in commonMain (gallery/AppDatabase.kt).
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}
