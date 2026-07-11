package com.calenpen

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.calenpen.databinding.ActivityMainBinding

/**
 * Shell activity that hosts the bottom-navigation + NavHost fragment.
 *
 * The four main destinations are:
 *  1. Calendar  – monthly diary view
 *  2. Notes     – flat reverse-chronological list of all notes
 *  3. Templates – template picker
 *  4. Search    – full-text search across all notes and dates
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigationView.setupWithNavController(navController)
    }
}
