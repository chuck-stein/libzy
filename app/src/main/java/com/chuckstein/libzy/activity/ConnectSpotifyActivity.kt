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
}
