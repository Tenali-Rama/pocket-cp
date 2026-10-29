package app.pocketcp.buildlogic

import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/** The module holding the `captureMatrix` helper; every other Compose module gets it on its test classpath. */
private const val SCREENSHOT_MODULE = ":core:screenshot"

/** Where a module's goldens live, `<name>_<device>_<theme>.png` (docs/decisions/0003). */
private const val GOLDEN_DIR = "src/test/screenshots"

/**
 * Roborazzi screenshot tests for a Compose module: goldens in [GOLDEN_DIR], Robolectric native graphics
 * for every unit test, and `ciCheck` running Roborazzi verify, which fails on a pixel diff or a missing golden.
 */
internal fun Project.configureScreenshots() {
    pluginManager.apply("io.github.takahirom.roborazzi")
    extensions.configure<RoborazziExtension> {
        outputDir.set(layout.projectDirectory.dir(GOLDEN_DIR))
    }
    tasks.withType<Test>().configureEach {
        systemProperty("robolectric.graphicsMode", "NATIVE")
        systemProperty("robolectric.pixelCopyRenderMode", "hardware")
    }
    if (path != SCREENSHOT_MODULE) {
        dependencies { add("testImplementation", project(SCREENSHOT_MODULE)) }
    }
    // Verify mode also runs the module's other unit tests; ciCheck already depends on them.
    tasks.matching { it.name == "ciCheck" }.configureEach { dependsOn("verifyRoborazziDebug") }
}
