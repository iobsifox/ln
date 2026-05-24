package com.example

import android.app.Application
import com.example.logs.LogRepository

class LastNightApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LogRepository.i("Application", "Last Night application initializing...")
    }
}
