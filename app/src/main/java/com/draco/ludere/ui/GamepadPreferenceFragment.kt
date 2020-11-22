package com.draco.ludere.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.draco.ludere.R

class GamepadPreferenceFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.gamepad, rootKey)
    }
}