package app.pocketcp.core.datastore

import org.junit.Assert.assertEquals
import org.junit.Test

class TextSizeScaleTest {
    @Test
    fun textSizes_scaleAsSettingsOffers() {
        val scales = TextSizePreference.entries.associateWith { it.scale }

        assertEquals(
            mapOf(
                TextSizePreference.Small to 0.9f,
                TextSizePreference.Default to 1f,
                TextSizePreference.Large to 1.15f,
                TextSizePreference.Largest to 1.3f,
            ),
            scales,
        )
    }
}
