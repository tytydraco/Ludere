package com.draco.ludere.ui

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.draco.ludere.R

class SettingsPreferenceFragment: PreferenceFragmentCompat() {
    lateinit var resultCallback: (Int) -> Unit

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            getString(R.string.settings_restart_key) -> {
                val intent = activity?.baseContext!!.packageManager.getLaunchIntentForPackage(activity?.baseContext!!.packageName)!!.apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                activity?.startActivity(intent)
                activity?.finishAfterTransition()
            }
            getString(R.string.settings_save_state_key) -> returnResult(SettingsActivity.RESULT_CODE_SAVE_STATE)
            getString(R.string.settings_load_state_key) -> returnResult(SettingsActivity.RESULT_CODE_LOAD_STATE)
            else -> return true
        }

        return super.onPreferenceTreeClick(preference)
    }

    private fun returnResult(result: Int) {
        resultCallback(result)
        activity?.finish()
    }
}