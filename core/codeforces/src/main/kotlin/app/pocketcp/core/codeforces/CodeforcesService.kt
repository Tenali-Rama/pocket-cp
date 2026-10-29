package app.pocketcp.core.codeforces

import retrofit2.http.GET
import retrofit2.http.Query

/** The public API methods, relative to `https://codeforces.com/api/`. List parameters are `;`-separated. */
internal interface CodeforcesService {
    /** [gym] is sent only when true; Codeforces then lists Gym contests instead of regular ones. */
    @GET("contest.list")
    suspend fun contestList(
        @Query("gym") gym: Boolean?,
    ): Envelope<List<ContestDto>>

    @GET("problemset.problems")
    suspend fun problemsetProblems(
        @Query("tags") tags: String?,
    ): Envelope<ProblemsetDto>

    @GET("user.info")
    suspend fun userInfo(
        @Query("handles") handles: String,
    ): Envelope<List<UserDto>>
}
