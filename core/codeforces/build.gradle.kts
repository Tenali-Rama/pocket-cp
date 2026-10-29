import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    alias(libs.plugins.pocketcp.android.library)
    alias(libs.plugins.pocketcp.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// The live canary (src/canary/) is the unit test source set of a `canary` build type. That variant exists only
// when liveCanary is requested, so ciCheck and the unit test tasks never compile or run live calls.
val liveCanaryRequested = gradle.startParameter.taskNames.any { it.substringAfterLast(':') == "liveCanary" }

android {
    namespace = "app.pocketcp.core.codeforces"

    buildTypes {
        create("canary") {
            initWith(getByName("debug"))
            matchingFallbacks += "debug"
        }
    }
    sourceSets.getByName("testCanary").kotlin.srcDir("src/canary/kotlin")
}

androidComponents {
    beforeVariants(selector().withBuildType("canary")) { it.enable = liveCanaryRequested }
}

dependencies {
    // CodeforcesApi is built on RequestQueue, which is part of this module's public API.
    api(project(":core:network"))
    // CodeforcesJudgeSource is a JudgeSource and returns core/model types.
    api(project(":core:model"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)

    testImplementation(project(":core:testing"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    "testCanaryImplementation"(libs.jsoup)
}

tasks.register("liveCanary") {
    group = "verification"
    description = "Checks the client and parsing assumptions against live Codeforces (canary.yml, nightly)."
    dependsOn("testCanaryUnitTest")
}

tasks.withType<Test>().matching { it.name == "testCanaryUnitTest" }.configureEach {
    // The variant's unit tests also include src/test/; run only the canary checks.
    filter.includeTestsMatching("app.pocketcp.core.codeforces.canary.*")
    // Live results: never up to date, never from the build cache.
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
    // canary.yml files the last 40 lines of output as the issue: print each failure's message, not its stack.
    testLogging {
        events(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = false
        showCauses = true
    }
}
