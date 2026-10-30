plugins {
    alias(libs.plugins.pocketcp.android.library)
    alias(libs.plugins.pocketcp.hilt)
}

android {
    namespace = "app.pocketcp.core.datastore"
}

dependencies {
    // `UserSettings` holds `OptionalModule`s and `SettingsRepository.settings` is a Flow: both are public API.
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
}
