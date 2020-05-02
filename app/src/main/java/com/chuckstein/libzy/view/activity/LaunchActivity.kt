package com.chuckstein.libzy.view.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.chuckstein.libzy.R
import com.chuckstein.libzy.view.activity.common.GradientBackgroundActivity

class LaunchActivity : GradientBackgroundActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        // TODO: only show ConnectSpotifyActivity the first time, otherwise do auth here (first check if the current access code hasn't expired yet)
//        val targetActivity = if (spotifyConnected()) FilterActivity::class.java else ConnectSpotifyActivity::class.java
        val targetActivity = ConnectSpotifyActivity::class.java // TODO: if it's always the same activity, don't need this variable

        val intent = Intent(this, targetActivity)
        startActivity(intent)
    }

    // TODO: determine whether this is necessary
    private fun spotifyConnected() = getSharedPreferences(getString(R.string.spotify_prefs_name), Context.MODE_PRIVATE)
        .getBoolean(getString(R.string.spotify_connected_key), false)

}
