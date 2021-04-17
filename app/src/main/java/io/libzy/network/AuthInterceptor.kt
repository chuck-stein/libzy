package io.libzy.network

import android.content.Context
import android.content.SharedPreferences
import io.libzy.R
import io.libzy.spotify.auth.LegacySpotifyAccessToken
import io.libzy.util.currentTimeSeconds
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(context: Context) : Interceptor {

    var accessToken: SpotifyAccessToken? = null
        private set

    init {
        val spotifyPrefs: SharedPreferences = context.getSharedPreferences(
            context.getString(R.string.spotify_prefs_name),
            Context.MODE_PRIVATE
        )
        val accessTokenKey = context.getString(R.string.spotify_access_token_key)
        val expirationKey = context.getString(R.string.spotify_token_expiration_key)
        val savedAccessToken = spotifyPrefs.getString(accessTokenKey, null)
        val savedTokenExpiration = spotifyPrefs.getInt(expirationKey, 0)
        if (savedAccessToken != null && currentTimeSeconds() < savedTokenExpiration) {
            accessToken = SpotifyAccessToken(savedAccessToken, savedTokenExpiration)
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
        return chain.proceed(request)
    }

}
