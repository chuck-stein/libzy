package com.chuckstein.libzy.common

import android.app.Application
import com.chuckstein.libzy.di.ApplicationComponent
import com.chuckstein.libzy.di.DaggerApplicationComponent

class LibzyApplication : Application() {

    val appComponent: ApplicationComponent by lazy {
        DaggerApplicationComponent.factory().create(applicationContext)
    }

}