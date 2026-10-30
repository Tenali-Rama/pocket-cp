package app.pocketcp.judge

import app.pocketcp.core.codeforces.CodeforcesApi
import app.pocketcp.core.codeforces.judge.CodeforcesJudgeSource
import app.pocketcp.core.model.JudgeSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Live Codeforces as the app's [JudgeSource] (ADR 0015). `src/live/kotlin` is a source dir of `release` and of the
 * Baseline Profile build types derived from it; debug binds recorded responses instead (`src/debug/`).
 */
@Module
@InstallIn(SingletonComponent::class)
object LiveJudgeSourceModule {
    @Provides
    @Singleton
    fun provideJudgeSource(api: CodeforcesApi): JudgeSource = CodeforcesJudgeSource(api)
}
