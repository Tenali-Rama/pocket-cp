import app.pocketcp.buildlogic.ANDROID_CI_TASKS
import app.pocketcp.buildlogic.TARGET_SDK
import app.pocketcp.buildlogic.configureKotlinAndroid
import app.pocketcp.buildlogic.configureQuality
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** `pocketcp.android.library`: an Android `core/<name>` module (also the base of feature modules). */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("org.jetbrains.kotlin.android")
            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                testOptions.targetSdk = TARGET_SDK
                lint.targetSdk = TARGET_SDK
            }
            configureQuality(ANDROID_CI_TASKS)
        }
    }
}
