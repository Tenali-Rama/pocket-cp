plugins {
    alias(libs.plugins.pocketcp.android.library)
    alias(libs.plugins.pocketcp.android.compose)
    alias(libs.plugins.pocketcp.hilt)
}

android {
    namespace = "app.pocketcp.core.problemview"
}

dependencies {
    // `ProblemViewModelFactory.create` takes a `ProblemId`; features see the domain through this module.
    api(project(":core:model"))
    // `ProblemViewModel` is a lifecycle `ViewModel`; features build it with `viewModel(key) { factory.create(id) }`.
    api(libs.androidx.lifecycle.viewmodel.compose)
    implementation(project(":core:data"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:statement"))
    implementation(project(":core:ui"))

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    // Screenshots show real statements: recorded pages from testdata/ through the Codeforces parser.
    testImplementation(project(":core:parser"))
    testImplementation(project(":core:testing"))
    testImplementation(libs.coil)
}
