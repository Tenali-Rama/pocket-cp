plugins {
    alias(libs.plugins.pocketcp.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // Annotations only (ADR 0017). The generated serializers are public API, so consumers get the runtime too.
    api(libs.kotlinx.serialization.core)

    testImplementation(libs.kotlinx.serialization.json)
}
