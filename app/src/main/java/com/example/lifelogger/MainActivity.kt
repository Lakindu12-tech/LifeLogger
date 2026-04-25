package com.example.lifelogger

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import com.example.lifelogger.databinding.ActivityMainBinding

/**
 * MEMBER 2 RESPONSIBILITY: UI Layer - MainActivity
 * 
 * Main activity that hosts all fragments
 * Uses Navigation Component to manage fragment transactions
 * 
 * Navigation is cleaner than manual fragment transactions:
 * - Automatic back stack management
 * - Safe argument passing via Directions
 * - Animation support between screens
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // View binding replaces findViewById
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        enableEdgeToEdge()
        
        // Setup window insets (padding for system bars)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    
    // Handle back navigation
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}