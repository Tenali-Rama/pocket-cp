package app.pocketcp.judge

import android.content.Context
import app.pocketcp.core.model.JudgeSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import javax.inject.Singleton

/**
 * Debug builds bind recorded Codeforces responses as the app's [JudgeSource] (ADR 0015). The files are the debug
 * assets, i.e. `testdata/codeforces/api/`. Release-like build types bind the live source from `src/live/`.
 */
@Module
@InstallIn(SingletonComponent::class)
object FixtureJudgeSourceModule {
    @Provides
    @Singleton
    fun provideJudgeSource(
        @ApplicationContext context: Context,
    ): JudgeSource {
        val assets = context.assets
        return fixtureJudgeSource { name -> assets.open(name).bufferedReader().use { it.readText() } }
    }

    /** The dispatcher is a defaulted parameter, as detekt's InjectDispatcher asks. */
    private fun fixtureJudgeSource(
        assets: FixtureAssets,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): JudgeSource {
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        return FixtureJudgeSource(assets, Clock.systemUTC(), scope, dispatcher)
    }
}
