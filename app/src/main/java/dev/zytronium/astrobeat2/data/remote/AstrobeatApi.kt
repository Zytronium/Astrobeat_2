package dev.zytronium.astrobeat2.data.remote

import dev.zytronium.astrobeat2.data.remote.dto.PlayRequest
import dev.zytronium.astrobeat2.data.remote.dto.PlayResponse
import dev.zytronium.astrobeat2.data.remote.dto.StreamUrlResponse
import dev.zytronium.astrobeat2.data.remote.dto.SyncResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AstrobeatApi {

    // -------- sync - called on launch and by background worker --------
    @GET("api/sync")
    suspend fun sync(): SyncResponse

    // -------- stream - get a presigned R2 URL for a track --------
    @GET("api/tracks/{id}/stream")
    suspend fun getStreamUrl(@Path("id") id: String): StreamUrlResponse

    // -------- play - log a play event --------
    @POST("api/tracks/{id}/play")
    suspend fun logPlay(
        @Path("id") id: String,
        @Body body: PlayRequest = PlayRequest(),
    ): PlayResponse
}
