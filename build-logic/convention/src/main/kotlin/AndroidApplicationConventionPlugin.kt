import app.pocketcp.buildlogic.ANDROID_CI_TASKS
import app.pocketcp.buildlogic.TARGET_SDK
import app.pocketcp.buildlogic.configureKotlinAndroid
import app.pocketcp.buildlogic.configureQuality
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** `pocketcp.android.application`: the phone/tablet app module. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.android")
            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = TARGET_SDK
            }
            configureQuality(ANDROID_CI_TASKS)
        }
    }
}
