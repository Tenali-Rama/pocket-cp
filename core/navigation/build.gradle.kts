plugins {
    alias(libs.plugins.pocketcp.android.library)
    alias(libs.plugins.pocketcp.android.compose)
    alias(libs.plugins.pocketcp.hilt)
}

android {
    namespace = "app.pocketcp.core.navigation"
}

dependencies {
    // `FeatureEntry.register` takes a `NavGraphBuilder`; features add screens with navigation-compose's `composable`.
    api(libs.androidx.navigation.compose)
    // `FeatureEntry.module` is an `OptionalModule`.
    api(project(":core:model"))

    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
