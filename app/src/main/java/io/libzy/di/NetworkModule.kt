package io.libzy.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import io.libzy.network.SpotifyApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class NetworkModule {

    @Singleton
    @Provides
    fun provideSpotifyClient(): OkHttpClient = OkHttpClient.Builder().addInterceptor().build()

    @Singleton
    @Provides
    fun provideSpotifyApi(spotifyClient: OkHttpClient): SpotifyApi = Retrofit.Builder()
        .baseUrl("https://api.spotify.com/v1")
        .client(spotifyClient)
        .addConverterFactory(Json.asConverterFactory(MediaType.get("application/json")))
        .build()
        .create(SpotifyApi::class.java)
}
