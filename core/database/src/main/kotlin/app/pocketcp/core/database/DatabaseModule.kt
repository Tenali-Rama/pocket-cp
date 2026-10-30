package app.pocketcp.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** One database for the whole app, and its DAOs. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): PocketCpDatabase = Room.databaseBuilder(context, PocketCpDatabase::class.java, PocketCpDatabase.NAME).build()

    @Provides
    fun provideContestDao(database: PocketCpDatabase): ContestDao = database.contestDao()

    @Provides
    fun provideProblemDao(database: PocketCpDatabase): ProblemDao = database.problemDao()

    @Provides
    fun provideCacheMetaDao(database: PocketCpDatabase): CacheMetaDao = database.cacheMetaDao()
}
