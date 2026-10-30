package app.pocketcp.core.codeforces.judge

import app.pocketcp.core.codeforces.CodeforcesModule
import app.pocketcp.core.codeforces.withApi
import app.pocketcp.core.model.JudgeSource
import app.pocketcp.core.testing.FixtureServer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.inject.Singleton

class CodeforcesModuleTest {
    @Test
    fun `the JudgeSource binding is a singleton CodeforcesJudgeSource`() =
        runTest {
            val source = withApi(FixtureServer()) { api -> CodeforcesModule.provideJudgeSource(api) }

            assertTrue("got $source", source is CodeforcesJudgeSource)
            val provider = CodeforcesModule::class.java.methods.single { it.returnType == JudgeSource::class.java }
            assertNotNull(provider.getAnnotation(Singleton::class.java))
        }
}
