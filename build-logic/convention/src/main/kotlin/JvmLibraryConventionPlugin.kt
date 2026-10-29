import app.pocketcp.buildlogic.JVM_CI_TASKS
import app.pocketcp.buildlogic.configureKotlinJvm
import app.pocketcp.buildlogic.configureQuality
import app.pocketcp.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** `pocketcp.jvm.library`: a pure Kotlin `core/<name>` module (no Android framework). */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            // Android lint also checks plain JVM modules; it provides the `lint` task ciCheck runs.
            pluginManager.apply("com.android.lint")
            configureKotlinJvm()
            dependencies {
                add("testImplementation", libs.findLibrary("junit").get())
            }
            configureQuality(JVM_CI_TASKS)
        }
    }
}
