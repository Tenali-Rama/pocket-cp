package app.pocketcp.data

import app.pocketcp.core.data.ContestRepository
import app.pocketcp.core.data.ProblemRepository
import app.pocketcp.core.model.JudgeId
import app.pocketcp.core.model.JudgeSource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

/**
 * The app's real modules, unreplaced: this only compiles when the production graph reaches the repositories
 * from `JudgeSource` (bound per build type in `src/fixture` / `src/live`, ADR 0015) and the DAOs (core/database).
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class DataWiringTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Inject lateinit var source: JudgeSource

    @Inject lateinit var contests: ContestRepository

    @Inject lateinit var sameContests: ContestRepository

    @Inject lateinit var problems: ProblemRepository

    @Inject lateinit var sameProblems: ProblemRepository

    @Before
    fun inject() = hilt.inject()

    @Test
    fun `repositories are app-wide singletons on the Codeforces judge`() {
        assertEquals(JudgeId("codeforces"), source.judge)
        assertSame(contests, sameContests)
        assertSame(problems, sameProblems)
    }
}
