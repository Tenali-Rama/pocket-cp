import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// A com.android.test module that drives the release-like app on a device; CI's perf job runs it (ADR 0013).
// It configures AGP itself: the build-logic conventions target app and library modules, and this module
// ships nothing, so it has no ciCheck.
plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "app.pocketcp.benchmark"
    // Keep in step with COMPILE_SDK / TARGET_SDK in build-logic (KotlinAndroid.kt).
    compileSdk = 36

    defaultConfig {
        // BaselineProfileRule needs API 28+.
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // CI measures on an emulator: the budgets catch regressions, not absolute speed (ADR 0013).
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

baselineProfile {
    // The perf job starts the emulator itself (reactivecircus/android-emulator-runner).
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.uiautomator)
}
