plugins {
    alias(libs.plugins.pocketcp.android.library)
    alias(libs.plugins.pocketcp.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "app.pocketcp.core.database"
}

room {
    // Committed: each version's schema is the baseline for its migration test (CLAUDE.md).
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // DAOs return Flow, so Room and coroutine types are part of this module's public API.
    api(libs.room.runtime)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.room.ktx)
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
}
