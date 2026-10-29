plugins {
    alias(libs.plugins.pocketcp.android.library)
    alias(libs.plugins.pocketcp.android.compose)
}

android {
    namespace = "app.pocketcp.core.designsystem"
}

dependencies {
    // PocketCpTheme also provides a matching MaterialTheme, so Material 3 types are part of the API.
    api(libs.androidx.compose.material3)
}
