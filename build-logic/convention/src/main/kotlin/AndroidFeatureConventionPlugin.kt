import org.gradle.api.Plugin
import org.gradle.api.Project

/** `pocketcp.android.feature`: a `feature/<name>` module, i.e. an Android library with Compose and Hilt. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target.pluginManager) {
            apply("pocketcp.android.library")
            apply("pocketcp.android.compose")
            apply("pocketcp.hilt")
        }
    }
}
