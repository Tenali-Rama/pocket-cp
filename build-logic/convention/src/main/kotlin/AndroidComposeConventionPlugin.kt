import app.pocketcp.buildlogic.configureScreenshots
import app.pocketcp.buildlogic.libs
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * `pocketcp.android.compose`: Compose compiler, BOM and Material 3 for an app or library module,
 * plus Roborazzi screenshot tests (see [configureScreenshots]).
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.withPlugin("com.android.application") {
                extensions.configure<ApplicationExtension> { buildFeatures.compose = true }
            }
            pluginManager.withPlugin("com.android.library") {
                extensions.configure<LibraryExtension> { buildFeatures.compose = true }
            }
            // Our Android conventions apply Kotlin after AGP, so the debug configurations exist by then.
            pluginManager.withPlugin("org.jetbrains.kotlin.android") {
                pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
                dependencies {
                    val bom = platform(libs.findLibrary("androidx-compose-bom").get())
                    add("implementation", bom)
                    add("implementation", libs.findLibrary("androidx-compose-ui").get())
                    add("implementation", libs.findLibrary("androidx-compose-material3").get())
                    add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
                    add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
                    add("debugImplementation", libs.findLibrary("androidx-compose-ui-test-manifest").get())
                    add("testImplementation", bom)
                    add("testImplementation", libs.findLibrary("androidx-compose-ui-test-junit4").get())
                }
                configureScreenshots()
            }
        }
    }
}
