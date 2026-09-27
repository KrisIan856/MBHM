package com.example.mbhm

import android.app.Application
import com.example.mbhm.data.database.AppDatabase

class MBHMApplication : Application() {
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }
}