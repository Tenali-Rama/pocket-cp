package app.pocketcp.core.codeforces.judge

import app.pocketcp.core.codeforces.CodeforcesApi
import app.pocketcp.core.codeforces.CodeforcesModule
import app.pocketcp.core.model.JudgeSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import javax.inject.Singleton

class CodeforcesModuleTest {
    private val providers = CodeforcesModule::class.java.methods

    @Test
    fun `the module provides a singleton CodeforcesApi`() {
        val provider = providers.single { it.returnType == CodeforcesApi::class.java }

        assertNotNull(provider.getAnnotation(Singleton::class.java))
    }

    @Test
    fun `the JudgeSource binding is left to the app's build types`() {
        assertEquals(emptyList<String>(), providers.filter { it.returnType == JudgeSource::class.java }.map { it.name })
    }
}
