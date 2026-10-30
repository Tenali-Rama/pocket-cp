package app.pocketcp.feature.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Immutable

/** The installed app as the About page names it: its launcher [name] and [versionName] (`null` when it has none). */
@Immutable
internal data class AppInfo(
    val name: String,
    val versionName: String?,
)

/** This app's label and `versionName`, read from the package manager rather than built in. */
internal fun Context.installedAppInfo(): AppInfo {
    val info =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
    return AppInfo(applicationInfo.loadLabel(packageManager).toString(), info.versionName)
}
