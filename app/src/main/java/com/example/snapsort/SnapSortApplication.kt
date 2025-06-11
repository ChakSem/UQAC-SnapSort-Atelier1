// Application.kt
package com.example.snapsort

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SnapSortApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize any global configuration here

    }
}
    