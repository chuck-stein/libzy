package com.chuckstein.libzy.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LaunchViewModel : ViewModel() {
    val spotifyConnected : LiveData<Boolean> = MutableLiveData(true) // TODO: call SharedPreferences or delegate further down the app architecture
}