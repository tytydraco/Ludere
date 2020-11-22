package com.draco.ludere.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.draco.ludere.R

class SettingsActivity : AppCompatActivity() {
    companion object {
        const val PREFERENCE_KEY_ROM_URI = "rom_uri"
        const val PREFERENCE_KEY_CORE_URI = "core_uri"

        const val ACTIVITY_REQUEST_CODE = 1

        /* Work for the calling activity to do */
        const val RESULT_CODE_SAVE_STATE = 0
        const val RESULT_CODE_LOAD_STATE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preference_fragment)

        val preferenceFragment = SettingsPreferenceFragment().apply {
            /* Pipe the result code to the calling activity */
            resultCallback = { setResult(it) }
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, preferenceFragment)
            .commit()
    }
}