import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.pocketcp.android.application)
    alias(libs.plugins.pocketcp.android.compose)
    alias(libs.plugins.pocketcp.hilt)
    // Not a convention plugin: only the app consumes a Baseline Profile (ADR 0013).
    alias(libs.plugins.baselineprofile)
}

// The only place the app version lives. app/CLAUDE.md says when it changes.
val appVersionName = "0.1.0"
val appVersionCode = 2

// Where the live JudgeSource binding lives; debug builds bind recorded responses instead (ADR 0015).
val liveSources = "src/live/kotlin"

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

    // The JudgeSource binding is per build type (ADR 0015): live Codeforces from src/live/kotlin, recorded
    // responses in debug, which packages testdata/codeforces/api/ as its assets. Release-like types get src/live
    // below; a new build type adds one of the two explicitly (app/CLAUDE.md).
    sourceSets {
        getByName("release").kotlin.srcDir(liveSources)
        getByName("debug").assets.srcDir(rootProject.file("testdata/codeforces/api"))
    }
}

// The baselineprofile plugin derives benchmarkRelease and nonMinifiedRelease from release, which has no signing
// config without -Pkeystore. The perf CI job installs them on an emulator, so they use the debug key (ADR 0013).
// They also bind the live JudgeSource like release, so benchmarks measure the real mapping path.
androidComponents {
    finalizeDsl { android ->
        val debugKey = android.signingConfigs.getByName("debug")
        val derived = android.buildTypes.filter { it.name.startsWith("benchmark") || it.name.startsWith("nonMinified") }
        derived.forEach { buildType ->
            buildType.signingConfig = debugKey
            val sources = android.sourceSets.maybeCreate(buildType.name)
            sources.kotlin.srcDir(liveSources)
        }
        val bound = derived.map { it.name } + "debug" + "release"
        val unbound = android.buildTypes.map { it.name } - bound.toSet()
        check(unbound.isEmpty()) { "Build types $unbound bind no JudgeSource: see app/CLAUDE.md (ADR 0015)." }
    }
}

// The release APK must ship a compiled Baseline Profile, and the merged profile behind it must hold the app's own
// rules, i.e. the committed app/src/main/baseline-prof.txt (library profiles alone also yield a baseline.prof).
val checkBaselineProfile by tasks.registering {
    description = "Fails when the release APK has no assets/dexopt/baseline.prof or its profile lacks the app's rules."
    dependsOn("assembleRelease")
    val apkDir = layout.buildDirectory.dir("outputs/apk/release")
    val mergedDir = layout.buildDirectory.dir("intermediates/merged_art_profile/release")
    doLast {
        val dir = apkDir.get().asFile
        val apks = dir.listFiles().orEmpty().filter { it.extension == "apk" }
        check(apks.isNotEmpty()) { "No release APK in $dir" }
        apks.forEach { apk ->
            ZipFile(apk).use { zip ->
                check(zip.getEntry("assets/dexopt/baseline.prof") != null) {
                    "${apk.name} has no assets/dexopt/baseline.prof: is the Baseline Profile set up?"
                }
            }
        }
        val merged =
            mergedDir
                .get()
                .asFile
                .walk()
                .filter { it.isFile && it.extension == "txt" }
                .toList()
        check(merged.any { file -> file.useLines { lines -> lines.any { "Lapp/pocketcp/" in it } } }) {
            "No app.pocketcp rules in the merged release profile $merged: is app/src/main/baseline-prof.txt there?"
        }
    }
}

// CI also builds the (unsigned) release APK, so R8 and resource-shrinking problems fail the PR,
// and checks that it ships the Baseline Profile. ciCheck's unit tests cover debug only, so it also compiles the
// release-like build types with their Hilt graph (no R8): a broken live JudgeSource binding fails CI there too.
tasks.named("ciCheck") {
    dependsOn("assembleRelease", checkBaselineProfile)
    dependsOn(
        listOf("Release", "BenchmarkRelease", "NonMinifiedRelease").flatMap {
            listOf("compile${it}JavaWithJavac", "hiltJavaCompile$it")
        },
    )
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(project(":core:adaptive"))
    // CodeforcesApi and CodeforcesJudgeSource behind the JudgeSource bindings in src/live and src/debug;
    // DataWiringTest checks the binding reaches the core/data repositories.
    implementation(project(":core:codeforces"))
    implementation(project(":core:data"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:network"))
    implementation(project(":feature:contests"))
    implementation(project(":feature:settings"))
    // Installs the Baseline Profile on devices where the store does not.
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":benchmark"))

    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.hilt.android.testing)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    kspTest(libs.hilt.compiler)
    // FixtureJudgeSourceTest reads testdata/ like the module tests do.
    testDebugImplementation(project(":core:testing"))
    testDebugImplementation(libs.kotlinx.coroutines.test)
}
