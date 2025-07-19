package com.databoss.aag

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.databoss.aag.fragments.HomeFragment
import com.databoss.aag.fragments.VADFragment
import com.databoss.aag.fragments.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    // promise that init the toggle var later
    // do this bcuz we need toggle in global
    // instead, make "toggle" nullable
    // but then need to check null every time
//    lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val homeFragment = HomeFragment()
        val VADFragment = VADFragment()
        val profileFragment = ProfileFragment()

        setCurrentFragment(homeFragment)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.miHome -> setCurrentFragment(homeFragment)
                R.id.miVAD -> setCurrentFragment(VADFragment)
                R.id.miProfile -> setCurrentFragment(profileFragment)
            }
            true // lambda returns last line of the function
        }

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val btnDrawerToggle = findViewById<ImageButton>(R.id.btnDrawerToggle)
        val navView = findViewById<NavigationView>(R.id.navView)

        btnDrawerToggle.bringToFront()

        // hook toggle button to drawer
        btnDrawerToggle.setOnClickListener {
            Log.d("MainActivity", "Toggle button clicked")
//            if (drawerLayout.isDrawerOpen(navView)) {
//                drawerLayout.closeDrawer(navView)
//            } else {
//                drawerLayout.openDrawer(navView)
//            }
            drawerLayout.openDrawer(navView)
        }

        navView.setNavigationItemSelectedListener {
            when (it.itemId) {
//                R.id.miItem1 -> Toast.makeText(applicationContext,
//                    "Clicked Item 1", Toast.LENGTH_SHORT).show()
//                R.id.miItem2 -> Toast.makeText(applicationContext,
//                    "Clicked Item 2", Toast.LENGTH_SHORT).show()
//                R.id.miItem3 -> Toast.makeText(applicationContext,
//                    "Clicked Item 3", Toast.LENGTH_SHORT).show()
                R.id.miItem1 -> {
                    setCurrentFragment(homeFragment)
                    bottomNavigationView.selectedItemId = R.id.miHome
                }
                R.id.miItem2 -> {
                    setCurrentFragment(VADFragment)
                    bottomNavigationView.selectedItemId = R.id.miVAD
                }
                R.id.miItem3 -> {
                    setCurrentFragment(profileFragment)
                    bottomNavigationView.selectedItemId = R.id.miProfile
                }
            }
            drawerLayout.closeDrawer(navView) // close after selection
            true
        }

//        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
//        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
//        drawerLayout.addDrawerListener(toggle)
//        toggle.syncState() // make toggle ready to be used

        // able to open toggle
        // when its opened toggle moves to back arrow
        // and with back arrow drawer closes
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//
//        val navView = findViewById<NavigationView>(R.id.navView)
//        navView.setNavigationItemSelectedListener {
//            when (it.itemId) {
//                R.id.miItem1 -> Toast.makeText(applicationContext,
//                    "Clicked Item 1", Toast.LENGTH_SHORT).show()
//                R.id.miItem2 -> Toast.makeText(applicationContext,
//                    "Clicked Item 2", Toast.LENGTH_SHORT).show()
//                R.id.miItem3 -> Toast.makeText(applicationContext,
//                    "Clicked Item 3", Toast.LENGTH_SHORT).show()
//            }
//            true
//        }
    }

    private fun setCurrentFragment(fragment: Fragment) = supportFragmentManager.beginTransaction().apply {
        replace(R.id.flFragment, fragment)
        commit()
    }
}

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        // need toggle to be global to access also here
//        if (toggle.onOptionsItemSelected(item)) {
//            return true
//        }
//        return super.onOptionsItemSelected(item)
//    }

// INSIDE MAIN
//        val firstFragment = HomeFragment()
//        val secondFragment = MessageFragment()
//
//
//        val button1 = findViewById<Button>(R.id.btnFragment1)
//        val button2 = findViewById<Button>(R.id.btnFragment2)
//
//        // to replace the content of frame layout with fragments
//        // need to use fragment transaction
//        supportFragmentManager.beginTransaction().apply {
//            replace(R.id.flFragment, firstFragment)
//            commit()
//        }
//
//        button1.setOnClickListener {
//            supportFragmentManager.beginTransaction().apply {
//                replace(R.id.flFragment, firstFragment)
//                addToBackStack(null)
//                commit()
//            }
//        }
//
//        button2.setOnClickListener {
//            supportFragmentManager.beginTransaction().apply {
//                replace(R.id.flFragment, secondFragment)
//                addToBackStack(null)
//                commit()
//            }
//        }