package app.pocketcp.buildlogic

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import io.gitlab.arturbosch.detekt.getSupportedKotlinVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import java.io.File

/** The tasks a module's `ciCheck` runs besides ktlint and detekt. */
internal class CiTasks(
    val lint: String,
    val unitTest: String,
    val assemble: String,
)

internal val ANDROID_CI_TASKS = CiTasks(lint = "lintDebug", unitTest = "testDebugUnitTest", assemble = "assembleDebug")
internal val JVM_CI_TASKS = CiTasks(lint = "lint", unitTest = "test", assemble = "assemble")

private val BUILD_DIR_SEGMENT = "${File.separator}build${File.separator}"

/**
 * Applies ktlint and detekt and registers this module's `ciCheck` task.
 * `./gradlew ciCheck` at the root runs it in every module, so new modules join automatically.
 */
internal fun Project.configureQuality(ciTasks: CiTasks) {
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    pluginManager.apply("io.gitlab.arturbosch.detekt")

    extensions.configure<KtlintExtension> {
        version.set(libs.findVersion("ktlint").get().requiredVersion)
        filter { exclude { it.file.path.contains(BUILD_DIR_SEGMENT) } }
    }

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        parallel = true
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    }
    tasks.withType<Detekt>().configureEach {
        jvmTarget = "17"
        exclude { it.file.path.contains(BUILD_DIR_SEGMENT) }
    }
    // detekt runs on its own embedded Kotlin compiler; keep the project's Kotlin version off its classpath.
    configurations.matching { it.name == "detekt" }.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") useVersion(getSupportedKotlinVersion())
        }
    }

    tasks.register("ciCheck") {
        group = "verification"
        description = "Everything CI gates on for this module: ktlint, detekt, lint, unit tests, assemble."
        // detektMain uses type resolution, which rules like UnsafeCallOnNullableType (no `!!`) need.
        dependsOn("ktlintCheck", "detektMain", ciTasks.lint, ciTasks.unitTest, ciTasks.assemble)
    }
}
