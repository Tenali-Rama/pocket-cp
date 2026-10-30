plugins {
    alias(libs.plugins.pocketcp.android.feature)
}

android {
    namespace = "app.pocketcp.feature.home"
}

dependencies {
    implementation(project(":core:adaptive"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    // Also brings navigation-compose, whose `api` includes lifecycle-viewmodel-compose (`viewModel { }`).
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
