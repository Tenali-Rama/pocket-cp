package app.pocketcp.core.datastore

import app.pocketcp.core.model.OptionalModule

/** The user's settings (spec §15). A field never set, or stored with an unknown key, has its default. */
data class UserSettings(
    val theme: ThemePreference = ThemePreference.System,
    val appTextSize: TextSizePreference = TextSizePreference.Default,
    val codeTextSize: TextSizePreference = TextSizePreference.Default,
    val codeLigatures: Boolean = true,
    val density: DensityPreference = DensityPreference.Comfortable,
    val disabledModules: Set<OptionalModule> = emptySet(),
)

/** A settings enum stored by [key], never by name or ordinal. Never change or reuse a key. */
interface StoredPreference {
    val key: String
}

enum class ThemePreference(
    override val key: String,
) : StoredPreference {
    System("system"),
    Dark("dark"),
    Light("light"),
    Amoled("amoled"),
}

enum class TextSizePreference(
    override val key: String,
) : StoredPreference {
    Small("small"),
    Default("default"),
    Large("large"),
    Largest("largest"),
}

enum class DensityPreference(
    override val key: String,
) : StoredPreference {
    Comfortable("comfortable"),
    Compact("compact"),
}
