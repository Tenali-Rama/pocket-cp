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
 * Live Codeforces as the app's [JudgeSource] (ADR 0015). `src/live/kotlin` is a source dir of `release` only; debug
 * and the benchmark / Baseline Profile build types bind recorded responses instead (`src/fixture/`).
 */
@Module
@InstallIn(SingletonComponent::class)
object LiveJudgeSourceModule {
    @Provides
    @Singleton
    fun provideJudgeSource(api: CodeforcesApi): JudgeSource = CodeforcesJudgeSource(api)
}
