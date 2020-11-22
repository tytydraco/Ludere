package com.draco.ludere.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.draco.ludere.R

class GamepadActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preference_fragment)

        val preferenceFragment = GamepadPreferenceFragment()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, preferenceFragment)
            .commit()
    }
}