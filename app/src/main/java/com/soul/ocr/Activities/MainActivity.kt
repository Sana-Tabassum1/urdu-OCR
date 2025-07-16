package com.soul.ocr.Activities

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.soul.ocr.R
import com.soul.ocr.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, R.color.green1)
        // 2️⃣ Set icons to white
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.bottomNavigation.itemIconTintList = null


        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Setup BottomNavigationView with NavController
        binding.bottomNavigation.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.nav_home, R.id.nav_pinned, R.id.nav_settings, R.id.nav_library -> {
                       binding.relativeLayout.visibility = View.VISIBLE
                }
                else -> {
                    binding.relativeLayout.visibility = View.GONE

                }
            }
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    navController.navigate(R.id.nav_home)
                    binding.bottomNavigation.menu.findItem(R.id.nav_home).isChecked = true
                    true
                }
                R.id.nav_pinned -> {
                    navController.navigate(R.id.nav_pinned)
                    binding.bottomNavigation.menu.findItem(R.id.nav_pinned).isChecked = true
                    true
                }
                R.id.nav_library -> {
                    navController.navigate(R.id.nav_library)
                    binding.bottomNavigation.menu.findItem(R.id.nav_library).isChecked = true
                    true
                }
                R.id.nav_settings -> {
                    navController.navigate(R.id.nav_settings)
                    binding.bottomNavigation.menu.findItem(R.id.nav_settings).isChecked = true
                    true
                }
                else -> false
            }
        }

        binding.btncamera.setOnClickListener {
            navController.navigate(R.id.batchScanningFragment)
        }
        
    }

}