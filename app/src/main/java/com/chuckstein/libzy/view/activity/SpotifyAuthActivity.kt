package com.chuckstein.libzy.view.activity

import androidx.appcompat.app.AppCompatActivity

abstract class AbstractSpotifyAuthActivity : AppCompatActivity() {

    companion object {
        private val TAG = AbstractSpotifyAuthActivity::class.java.simpleName
        private const val SPOTIFY_AUTH_REQUEST_CODE = 1104
    }

    // TODO: move auth stuff here, determine what functions will be abstract
}