package com.example.projektna.data.api

import android.content.Context
import com.example.projektna.data.PreferencesManager

/**
 * Singleton za upravljanje JWT tokenov.
 * Inicializira se ob zagonu aplikacije v MainActivity.
 */
object AuthTokenProvider {

    private var preferencesManager: PreferencesManager? = null

    /**
     * Inicializacija s kontekstom aplikacije.
     * Kličemo v MainActivity.onCreate().
     */
    fun init(context: Context) {
        if (preferencesManager == null) {
            preferencesManager = PreferencesManager(context.applicationContext)
        }
    }

    /**
     * Vrne trenutni JWT token ali null, če uporabnik ni prijavljen.
     */
    fun getToken(): String? {
        return preferencesManager?.authToken
    }

    /**
     * Preveri, ali je uporabnik prijavljen.
     */
    fun isLoggedIn(): Boolean {
        return preferencesManager?.isLoggedIn == true
    }
}
