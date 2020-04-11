package com.chuckstein.libzy.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.chuckstein.libzy.R
import com.chuckstein.libzy.extension.initializeBackground

class ConnectSpotifyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect_spotify)
        initializeBackground()
    }

    // TODO: delete what's not needed here
    override fun onStart() {
        super.onStart()
        // We will start writing our code here.
    }

    private fun connected() {
        // Then we will write some more code here.
    }

    override fun onStop() {
        super.onStop()
        // Aaand we will finish off here.
    }
}
