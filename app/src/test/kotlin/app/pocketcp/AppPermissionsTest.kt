package app.pocketcp

import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The merged app manifest requests what `core/network` needs: the API client and `NetworkMonitor`. */
@RunWith(AndroidJUnit4::class)
class AppPermissionsTest {
    @Test
    fun mergedManifest_requestsInternetAndNetworkState() {
        val context = ApplicationProvider.getApplicationContext<PocketCpApplication>()
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val requested = info.requestedPermissions.orEmpty().toSet()

        assertTrue(requested.toString(), "android.permission.INTERNET" in requested)
        assertTrue(requested.toString(), "android.permission.ACCESS_NETWORK_STATE" in requested)
    }
}
