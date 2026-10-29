package app.pocketcp.core.codeforces

import kotlinx.serialization.Serializable

/*
 * Wire types of the public Codeforces API (https://codeforces.com/apiHelp/objects), decoded with unknown keys
 * ignored. Enum-like fields (`type`, `phase`) stay strings so a new value never breaks decoding; the M1 domain
 * layer maps them.
 */

/** A contest from `contest.list`. Gym contests also fill the gym-only fields. */
@Serializable
data class ContestDto(
    val id: Int,
    val name: String,
    val type: String,
    val phase: String,
    val frozen: Boolean,
    val durationSeconds: Long,
    val startTimeSeconds: Long? = null,
    val relativeTimeSeconds: Long? = null,
    val preparedBy: String? = null,
    val websiteUrl: String? = null,
    val description: String? = null,
    val difficulty: Int? = null,
    val kind: String? = null,
    val icpcRegion: String? = null,
    val country: String? = null,
    val city: String? = null,
    val season: String? = null,
)

/** The result of `problemset.problems`. */
@Serializable
data class ProblemsetDto(
    val problems: List<ProblemDto>,
    val problemStatistics: List<ProblemStatisticsDto>,
)

@Serializable
data class ProblemDto(
    val contestId: Int? = null,
    val problemsetName: String? = null,
    val index: String,
    val name: String,
    val type: String,
    val points: Double? = null,
    val rating: Int? = null,
    val tags: List<String> = emptyList(),
)

@Serializable
data class ProblemStatisticsDto(
    val contestId: Int? = null,
    val index: String,
    val solvedCount: Int,
)

/** A public profile from `user.info`. Unrated users have no rank or rating. */
@Serializable
data class UserDto(
    val handle: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val country: String? = null,
    val organization: String? = null,
    val contribution: Int = 0,
    val rank: String? = null,
    val rating: Int? = null,
    val maxRank: String? = null,
    val maxRating: Int? = null,
    val lastOnlineTimeSeconds: Long,
    val registrationTimeSeconds: Long,
    val friendOfCount: Int = 0,
    val avatar: String,
    val titlePhoto: String,
)
