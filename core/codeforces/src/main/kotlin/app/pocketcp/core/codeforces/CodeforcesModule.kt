package app.pocketcp.core.codeforces

import app.pocketcp.core.codeforces.judge.CodeforcesJudgeSource
import app.pocketcp.core.model.JudgeSource
import app.pocketcp.core.network.RequestQueue
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/** One [CodeforcesApi] on the app-wide [RequestQueue], and Codeforces as the app's [JudgeSource]. */
@Module
@InstallIn(SingletonComponent::class)
object CodeforcesModule {
    @Provides
    @Singleton
    fun provideCodeforcesApi(queue: RequestQueue): CodeforcesApi = CodeforcesApi(queue, callScope())

    @Provides
    @Singleton
    fun provideJudgeSource(api: CodeforcesApi): JudgeSource = CodeforcesJudgeSource(api)

    /**
     * The app-lifetime scope queued calls run in. Responses are decoded there, so it is off the main thread.
     * The dispatcher is a defaulted parameter, as detekt's InjectDispatcher asks.
     */
    private fun callScope(dispatcher: CoroutineDispatcher = Dispatchers.Default): CoroutineScope {
        val job = SupervisorJob()
        return CoroutineScope(job + dispatcher)
    }
}
