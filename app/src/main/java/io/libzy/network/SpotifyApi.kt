package io.libzy.network

import retrofit2.http.GET

private const val AUTHENTICATED_PATH = "/me"
private const val PLAYER_API = "$AUTHENTICATED_PATH/player"

interface SpotifyApi {

    @GET("$PLAYER_API/recently-played")
    suspend fun fetchPlayHistory()

}
