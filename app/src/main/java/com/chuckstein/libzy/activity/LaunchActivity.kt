package com.chuckstein.libzy.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.chuckstein.libzy.R
import com.chuckstein.libzy.extension.initializeBackground
import com.chuckstein.libzy.viewmodel.LaunchViewModel

class LaunchActivity : AppCompatActivity() {

    private val model : LaunchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)
        initializeBackground()

        model.spotifyConnected.observe(this, Observer<Boolean>{ spotifyConnected ->
            val targetActivity = if (spotifyConnected) ConnectSpotifyActivity::class.java else FilterActivity::class.java
            val intent = Intent(this, targetActivity)
            startActivity(intent)
        })

    }
}
