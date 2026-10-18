package com.example.mbhm

import android.app.Application
import com.example.mbhm.data.database.AppDatabase
import com.example.mbhm.data.sync.SyncManager

class MBHMApplication : Application() {
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        SyncManager.getInstance(this).startAutoSync()
    }
}
