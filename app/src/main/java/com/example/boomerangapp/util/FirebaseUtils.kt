package com.example.boomerangapp.util

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

object FirebaseUtils {
    fun initialize(context: Context) {
        FirebaseApp.initializeApp(context)
    }
    
    fun getCurrentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }
    
    fun isUserLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }
    
    fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }
}
