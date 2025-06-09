package com.example.boomerangapp

import android.app.Application
import com.example.boomerangapp.util.FirebaseUtils

class BoomerangApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseUtils.initialize(this)
    }
}
