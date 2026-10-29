plugins {
    alias(libs.plugins.pocketcp.android.library)
}

android {
    namespace = "app.pocketcp.core.testing"
}

dependencies {
    // FixtureServer is an HttpTransport, so core/network types are part of this module's API.
    api(project(":core:network"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
