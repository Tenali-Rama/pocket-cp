plugins {
    alias(libs.plugins.pocketcp.android.application)
    alias(libs.plugins.pocketcp.android.compose)
    alias(libs.plugins.pocketcp.hilt)
}

// The only place the app version lives. app/CLAUDE.md says when it changes.
val appVersionName = "0.0.0"
val appVersionCode = 1

// release.yml passes -Pkeystore=<path> plus the env vars below. Without it the release APK is unsigned.
val releaseKeystore = providers.gradleProperty("keystore").orNull?.let { file(it) }

android {
    namespace = "app.pocketcp"

    defaultConfig {
        applicationId = "app.pocketcp"
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = providers.environmentVariable("KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }
}

// CI also builds the (unsigned) release APK, so R8 and resource-shrinking problems fail the PR.
tasks.named("ciCheck") {
    dependsOn("assembleRelease")
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(project(":core:adaptive"))
    // The JudgeSource binding; DataWiringTest checks it reaches the core/data repositories.
    implementation(project(":core:codeforces"))
    implementation(project(":core:data"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:settings"))

    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.hilt.android.testing)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    kspTest(libs.hilt.compiler)
}
