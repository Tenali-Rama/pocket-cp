package app.pocketcp.core.data

import app.pocketcp.core.database.CacheMetaDao
import app.pocketcp.core.database.ContestDao
import app.pocketcp.core.database.ProblemDao
import app.pocketcp.core.model.JudgeSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import javax.inject.Singleton

/** The cache-first repositories on the app's [JudgeSource]. Singletons, so their refreshes are shared. */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideContestRepository(
        source: JudgeSource,
        dao: ContestDao,
        meta: CacheMetaDao,
    ): ContestRepository = CachedContestRepository(source, dao, meta, Clock.systemUTC(), refreshScope())

    @Provides
    @Singleton
    fun provideProblemRepository(
        source: JudgeSource,
        dao: ProblemDao,
        meta: CacheMetaDao,
    ): ProblemRepository = CachedProblemRepository(source, dao, meta, Clock.systemUTC(), refreshScope())

    /**
     * The app-lifetime scope refreshes run in, so leaving a screen doesn't cancel a fetch others wait for.
     * The dispatcher is a defaulted parameter, as detekt's InjectDispatcher asks.
     */
    private fun refreshScope(dispatcher: CoroutineDispatcher = Dispatchers.Default): CoroutineScope {
        val job = SupervisorJob()
        return CoroutineScope(job + dispatcher)
    }
}
