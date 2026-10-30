plugins {
    alias(libs.plugins.pocketcp.android.library)
    alias(libs.plugins.pocketcp.android.compose)
}

android {
    namespace = "app.pocketcp.core.ui"
}

dependencies {
    // The rows take `Contest`, `Problem` and `JudgeError`, so the domain is part of this module's API.
    api(project(":core:model"))
    implementation(project(":core:designsystem"))
}
