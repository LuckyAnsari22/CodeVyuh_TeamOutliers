package com.greeniq.app

import android.app.Application

class GreenIQApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: GreenIQApp
            private set
    }
}
