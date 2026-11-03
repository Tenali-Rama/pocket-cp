plugins {
    alias(libs.plugins.pocketcp.android.library)
    alias(libs.plugins.pocketcp.android.compose)
}

android {
    namespace = "app.pocketcp.core.statement"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:network"))
    implementation(libs.coil)
    implementation(libs.coil.compose)
}
