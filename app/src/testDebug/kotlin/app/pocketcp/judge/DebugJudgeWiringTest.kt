package app.pocketcp.judge

import app.pocketcp.core.model.JudgeSource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

/** The debug graph serves recorded responses, so UI flows on the debug APK never reach Codeforces (ADR 0015). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class DebugJudgeWiringTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Inject lateinit var source: JudgeSource

    @Before
    fun inject() = hilt.inject()

    @Test
    fun `debug builds bind the fixture judge source`() {
        assertTrue("got $source", source is FixtureJudgeSource)
    }
}
