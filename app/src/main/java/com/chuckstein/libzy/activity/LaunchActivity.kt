package com.chuckstein.libzy.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.chuckstein.libzy.R
import com.chuckstein.libzy.extension.initializeBackground

class LaunchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)
        initializeBackground()

        // TODO: only show ConnectSpotifyActivity the first time, otherwise do auth here (first check if the current access code hasn't expired yet)
//        val targetActivity = if (spotifyConnected()) FilterActivity::class.java else ConnectSpotifyActivity::class.java
        val targetActivity = ConnectSpotifyActivity::class.java // TODO: if it's always the same activity, don't need this variable

        val intent = Intent(this, targetActivity)
        startActivity(intent)
    }

    // TODO: determine whether this is necessary
    private fun spotifyConnected(): Boolean {
        val sharedPref =
            getSharedPreferences(getString(R.string.spotify_prefs_name), Context.MODE_PRIVATE)
        return sharedPref.getBoolean(getString(R.string.spotify_connected_key), false)
    }

}
