package com.draco.ludere.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.draco.ludere.R
import com.swordfish.libretrodroid.GLRetroView

class SettingsActivity(): AppCompatActivity() {
    companion object {
        const val ACTIVITY_REQUEST_CODE = 1

        const val RESULT_CODE_SAVE_STATE = 0
        const val RESULT_CODE_LOAD_STATE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val preferenceFragment = SettingsPreferenceFragment().apply {
            resultCallback = { setResult(it) }
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, preferenceFragment)
            .commit()
    }
}