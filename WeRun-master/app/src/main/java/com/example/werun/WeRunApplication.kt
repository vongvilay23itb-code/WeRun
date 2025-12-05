package com.example.werun

import android.app.Application
import com.mapbox.common.MapboxOptions
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WeRunApplication : Application() {
    override fun onCreate() {
        MapboxOptions.accessToken = getString(R.string.mapbox_access_token)
        super.onCreate()
    }
}