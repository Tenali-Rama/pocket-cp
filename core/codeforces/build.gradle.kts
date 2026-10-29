plugins {
    alias(libs.plugins.pocketcp.android.library)
    alias(libs.plugins.pocketcp.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.pocketcp.core.codeforces"
}

dependencies {
    // CodeforcesApi is built on RequestQueue, which is part of this module's public API.
    api(project(":core:network"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)

    testImplementation(project(":core:testing"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
