package app.pocketcp.core.model

/**
 * The optional modules the user can switch off (spec §15). [key] is persisted in settings,
 * so never change an existing one.
 */
enum class OptionalModule(
    val key: String,
) {
    LocalCompute("local_compute"),
    Ai("ai"),
    StressTester("stress_tester"),
    PracticeAndStreaks("practice_streaks"),
    Stats("stats"),
    OtherJudgesCalendar("other_judges_calendar"),
    FocusModeAutoStart("focus_mode_auto_start"),
    ;

    companion object {
        /** The module with this [key], or `null` for an unknown (e.g. removed) key. */
        fun fromKey(key: String): OptionalModule? = entries.firstOrNull { it.key == key }
    }
}
