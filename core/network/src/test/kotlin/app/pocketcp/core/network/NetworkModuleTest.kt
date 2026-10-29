package app.pocketcp.core.network

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import okhttp3.OkHttpClient
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class NetworkModuleTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Inject lateinit var client: OkHttpClient

    @Inject lateinit var sameClient: OkHttpClient

    @Inject lateinit var cookieJar: SharedCookieJar

    @Inject lateinit var queue: RequestQueue

    @Inject lateinit var sameQueue: RequestQueue

    @Before
    fun inject() = hilt.inject()

    @Test
    fun `client, cookie jar and queue are app-wide singletons`() {
        assertSame(client, sameClient)
        assertSame(queue, sameQueue)
        assertSame(cookieJar, client.cookieJar)
        assertNotNull(client.cache)
    }
}
