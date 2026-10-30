package app.pocketcp.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OptionalModuleTest {
    @Test
    fun keysAreUniqueAndLowercase() {
        val keys = OptionalModule.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        assertTrue(keys.all { it.matches(Regex("[a-z_]+")) })
    }

    @Test
    fun listsExactlyTheSpecModules() {
        assertEquals(7, OptionalModule.entries.size)
    }

    @Test
    fun fromKeyRoundTripsEveryModule() {
        OptionalModule.entries.forEach { assertEquals(it, OptionalModule.fromKey(it.key)) }
    }

    @Test
    fun fromKeyOfUnknownKeyIsNull() {
        assertNull(OptionalModule.fromKey("unknown"))
        assertNull(OptionalModule.fromKey(""))
        assertNull(OptionalModule.fromKey("AI"))
    }
}
