plugins {
    alias(libs.plugins.pocketcp.android.library)
    alias(libs.plugins.pocketcp.android.compose)
}

android {
    namespace = "app.pocketcp.core.screenshot"
}

// A test-support library: other Compose modules get it as `testImplementation` from the compose convention,
// so the test frameworks a screenshot test needs are `api` here.
dependencies {
    api(libs.androidx.test.ext.junit)
    api(libs.junit)
    api(libs.robolectric)
    api(libs.roborazzi)
    api(libs.roborazzi.compose)
}
