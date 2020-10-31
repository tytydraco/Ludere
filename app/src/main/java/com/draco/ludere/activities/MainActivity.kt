package com.draco.ludere.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.draco.ludere.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /* Setup preference screen */
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_layout, MainPreferenceFragment())
            .commit()
    }
}