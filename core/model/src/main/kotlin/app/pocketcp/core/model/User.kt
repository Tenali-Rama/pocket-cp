package app.pocketcp.core.model

/** A judge user. Ratings are `null` for unrated users; [avatarUrl] is an absolute URL or `null`. */
data class User(
    val handle: String,
    val rating: Int?,
    val maxRating: Int?,
    val avatarUrl: String?,
)
