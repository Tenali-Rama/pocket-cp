package app.pocketcp.core.database

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Stores a tag list as a JSON array, so tags with spaces or commas, and the empty list, round-trip exactly. */
internal class TagsConverter {
    private val serializer = ListSerializer(String.serializer())

    @TypeConverter
    fun fromTags(tags: List<String>): String = Json.encodeToString(serializer, tags)

    @TypeConverter
    fun toTags(json: String): List<String> = Json.decodeFromString(serializer, json)
}
