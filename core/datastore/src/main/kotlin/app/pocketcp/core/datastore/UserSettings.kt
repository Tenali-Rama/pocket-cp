package app.pocketcp.core.datastore

import app.pocketcp.core.model.OptionalModule
import app.pocketcp.core.model.StatementLanguage

/** The user's settings (spec §15). A field never set, or stored with an unknown key, has its default. */
data class UserSettings(
    val theme: ThemePreference = ThemePreference.System,
    val appTextSize: TextSizePreference = TextSizePreference.Default,
    val codeTextSize: TextSizePreference = TextSizePreference.Default,
    val codeLigatures: Boolean = true,
    val density: DensityPreference = DensityPreference.Comfortable,
    val disabledModules: Set<OptionalModule> = emptySet(),
    /** Hide problem tags in the problemset, so practice isn't spoiled (spec §7.3). */
    val hideProblemTags: Boolean = false,
    /** Hide problem ratings in the problemset, so practice isn't spoiled (spec §7.3). */
    val hideProblemRatings: Boolean = false,
    /** The language problem statements are shown in (spec §7.4), stored by [StatementLanguage.key]. */
    val statementLanguage: StatementLanguage = StatementLanguage.En,
    /** The text size of problem statements (spec §7.4). */
    val statementTextSize: TextSizePreference = TextSizePreference.Default,
    /** How wide a problem statement's text may run on wide windows (spec §7.4). */
    val statementWidth: StatementWidthPreference = StatementWidthPreference.Comfortable,
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

enum class StatementWidthPreference(
    override val key: String,
) : StoredPreference {
    Narrow("narrow"),
    Comfortable("comfortable"),
    Full("full"),
}
