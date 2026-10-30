plugins {
    alias(libs.plugins.pocketcp.android.library)
    alias(libs.plugins.pocketcp.hilt)
}

android {
    namespace = "app.pocketcp.core.data"
}

dependencies {
    // Repositories return Flow<Cached<...>> of core/model types; Room entities stay internal.
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)
    implementation(project(":core:database"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
}
