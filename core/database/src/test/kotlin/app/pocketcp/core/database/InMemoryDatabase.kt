package app.pocketcp.core.database

import androidx.room.Room
import org.robolectric.RuntimeEnvironment
import kotlin.time.Duration.Companion.seconds

/**
 * Wall-clock wait for a Room `Flow` emission. Turbine's 3 s default is too short for the first query in a
 * test JVM, which also loads Robolectric's native SQLite.
 */
internal val FLOW_TIMEOUT = 15.seconds

/** A fresh in-memory database on Room's own executors, so DAO calls run off the test thread as in the app. */
internal fun inMemoryDatabase(): PocketCpDatabase =
    Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), PocketCpDatabase::class.java).build()
