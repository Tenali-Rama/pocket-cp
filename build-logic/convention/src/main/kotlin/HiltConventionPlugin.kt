import app.pocketcp.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** `pocketcp.hilt`: Hilt with KSP for an Android app or library module. */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Hilt and KSP need the Android and Kotlin plugins in place first.
            pluginManager.withPlugin("org.jetbrains.kotlin.android") {
                pluginManager.apply("com.google.devtools.ksp")
                pluginManager.apply("com.google.dagger.hilt.android")
                dependencies {
                    add("implementation", libs.findLibrary("hilt-android").get())
                    add("ksp", libs.findLibrary("hilt-compiler").get())
                }
            }
        }
    }
}
