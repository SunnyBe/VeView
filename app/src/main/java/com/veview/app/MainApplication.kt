package com.veview.app

import android.app.Application
import timber.log.Timber

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.tag("MainApplication").d("onCreate called")
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant()
        }
    }
}
