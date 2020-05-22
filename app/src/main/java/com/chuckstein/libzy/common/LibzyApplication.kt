package com.chuckstein.libzy.common

import android.app.Application
import com.chuckstein.libzy.di.AppComponent
import com.chuckstein.libzy.di.DaggerAppComponent

class LibzyApplication : Application() {

    val appComponent: AppComponent by lazy {
        DaggerAppComponent.factory().create(applicationContext)
    }

}