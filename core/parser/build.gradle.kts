plugins {
    alias(libs.plugins.pocketcp.jvm.library)
}

val testingClasses =
    project(":core:testing").layout.buildDirectory.file(
        "intermediates/aar_main_jar/debug/syncDebugLibJars/classes.jar",
    )

tasks.configureEach {
    // The fixture helper lives in the Android test-support module; consume only its plain classes jar so this
    // module's JVM test and lint classpaths do not need to resolve Android variants or transitive dependencies.
    if (name.contains("Test") || name.contains("Lint")) dependsOn(":core:testing:assembleDebug")
}

dependencies {
    api(project(":core:model"))
    implementation(libs.jsoup)

    testImplementation(files(testingClasses))
}
