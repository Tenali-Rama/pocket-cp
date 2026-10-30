plugins {
    alias(libs.plugins.pocketcp.android.feature)
}

android {
    namespace = "app.pocketcp.feature.settings"
}

dependencies {
    implementation(project(":core:adaptive"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    // Also brings navigation-compose, whose `api` includes lifecycle-viewmodel-compose (`viewModel { }`).
    implementation(project(":core:navigation"))

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
