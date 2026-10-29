plugins {
    alias(libs.plugins.pocketcp.android.library)
    alias(libs.plugins.pocketcp.android.compose)
}

android {
    namespace = "app.pocketcp.core.adaptive"
}

dependencies {
    api(libs.androidx.window)
    implementation(libs.androidx.compose.material3.adaptive)

    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.window.testing)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
