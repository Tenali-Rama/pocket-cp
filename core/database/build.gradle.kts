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

// TEMPORARY (removed before review): prints the generated schema so it can be committed.
val dumpSchema by tasks.registering {
    dependsOn("assembleDebug", "testDebugUnitTest")
    doLast {
        val found = projectDir.walkTopDown().filter { it.name == "1.json" && "PocketCpDatabase" in it.path }.toList()
        throw GradleException(found.joinToString("\n") { "SCHEMA_FILE ${it.path}\n" + it.readText() })
    }
}
tasks.named("ciCheck") { dependsOn(dumpSchema) }
