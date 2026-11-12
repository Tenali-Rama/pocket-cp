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
    // CacheWarmer watches the connection; core/network has no judge in it (ADR 0009 holds).
    implementation(project(":core:network"))
    // Statements are cached as JSON (ADR 0017); core/model ships only the serializers.
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
}
