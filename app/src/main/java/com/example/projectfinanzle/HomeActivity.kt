package com.example.projectfinanzle

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.projectfinanzle.databinding.ActivityHomeBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ใช้ View Binding
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // กำหนด Toolbar เป็น ActionBar
        setSupportActionBar(binding.toolbar)  // เชื่อมโยง Toolbar
        supportActionBar?.setDisplayShowTitleEnabled(false)
        val navView: BottomNavigationView = binding.navView

        // ใช้ supportFragmentManager เพื่อหา NavController
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_home) as NavHostFragment
        navController = navHostFragment.findNavController()

        // กำหนดให้ Bottom Navigation ทำงานร่วมกับ NavController
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_history,
                R.id.navigation_Create,
                R.id.navigation_Transfer,
                R.id.navigation_Profile
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)


        // ให้เปิด HomeFragment เป็นหน้าแรก
        navController.navigate(R.id.navigation_home)

        val bottomNav = findViewById<BottomNavigationView>(R.id.nav_view)

        window.decorView.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            window.decorView.getWindowVisibleDisplayFrame(r)
            val screenHeight = window.decorView.height
            val keypadHeight = screenHeight - r.bottom

            if (keypadHeight > screenHeight * 0.15) {
                bottomNav.visibility = View.GONE  // ซ่อน Navbar เมื่อคีย์บอร์ดเปิด
            } else {
                bottomNav.visibility = View.VISIBLE // แสดง Navbar เมื่อปิดคีย์บอร์ด
            }
        }
    }
}

