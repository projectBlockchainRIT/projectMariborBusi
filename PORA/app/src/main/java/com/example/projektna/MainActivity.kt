package com.example.projektna

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.projektna.data.PreferencesManager
import com.example.projektna.data.api.AuthTokenProvider
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Inicializiraj AuthTokenProvider
        AuthTokenProvider.init(this)
        preferencesManager = PreferencesManager(this)

        setupNavigation()
        setupEdgeToEdge()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.setupWithNavController(navController)

        // Če je uporabnik že prijavljen, preskoči na glavni zaslon
        if (preferencesManager.isLoggedIn) {
            navController.navigate(R.id.navigation_home)
        }

        // Spremljaj navigacijo za skrivanje/prikazovanje bottom nav
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_login, R.id.navigation_register -> {
                    // Skrij bottom nav na auth zaslonih
                    bottomNav.visibility = View.GONE
                }
                else -> {
                    // Prikaži bottom nav na glavnih zaslonih
                    bottomNav.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
    }
}