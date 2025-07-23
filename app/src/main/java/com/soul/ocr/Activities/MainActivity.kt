package com.soul.ocr.Activities

import android.app.Activity
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.soul.ocr.R
import com.soul.ocr.ViewModel.BatchScanningViewModel
import com.soul.ocr.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var scannerLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var viewModel: BatchScanningViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[BatchScanningViewModel::class.java]

        scannerLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            val resultCode = result.resultCode
            val data = result.data
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(data)

            if (resultCode == Activity.RESULT_OK && scanResult != null) {
                val pages = scanResult.pages

                if (!pages.isNullOrEmpty()) {
                    val bitmaps = pages.mapNotNull { page ->
                        val inputStream = this.contentResolver.openInputStream(page.imageUri)
                        BitmapFactory.decodeStream(inputStream)
                    }

                    viewModel.setImages(bitmaps)
                    findNavController(R.id.nav_host_fragment).navigate(R.id.batchExtractFragment)

                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.d("Scanner", "User cancelled scan")
            } else {
                Log.e("Scanner", "Scan failed or unknown error")
            }
        }
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
        onBackPressedDispatcher.addCallback(this /* lifecycleOwner */, true) {
            if (navController.currentDestination?.id != R.id.nav_home) {
                navController.popBackStack(R.id.nav_home, false)

                // 2) bottom nav  highlight
                binding.bottomNavigation.selectedItemId = R.id.nav_home
            } else {
                // Home  app close
                finishAffinity()              // ya super.onBackPressedDispatcher.onBackPressed()
            }
        }
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
            launchDocumentScanner()
        }
        
    }
    private fun launchDocumentScanner() {
        val scannerOptions = GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()

        val documentScanner = GmsDocumentScanning.getClient(scannerOptions)

        documentScanner.getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                val request = IntentSenderRequest.Builder(intentSender).build()
                scannerLauncher.launch(request)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error launching scanner: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("Scanner", "Failed to launch: ${e.message}")
            }
    }
}