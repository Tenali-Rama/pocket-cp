package app.pocketcp.core.database

import androidx.room.Room
import org.robolectric.RuntimeEnvironment

/** A fresh in-memory database on Room's own executors, so DAO calls run off the test thread as in the app. */
internal fun inMemoryDatabase(): PocketCpDatabase =
    Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), PocketCpDatabase::class.java).build()
