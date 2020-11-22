package com.draco.ludere.ui

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.draco.ludere.R

class SettingsPreferenceFragment: PreferenceFragmentCompat() {
    /* Calling activity must set a work callback */
    lateinit var resultCallback: (Int) -> Unit

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            getString(R.string.settings_restart_key) -> {
                val intent = activity?.baseContext!!.packageManager.getLaunchIntentForPackage(activity?.baseContext!!.packageName)!!
                activity?.startActivity(intent)
                activity?.finishAfterTransition()
            }
            getString(R.string.settings_resume_key) -> activity?.finish()
            getString(R.string.settings_save_state_key) -> returnResult(SettingsActivity.RESULT_CODE_SAVE_STATE)
            getString(R.string.settings_load_state_key) -> returnResult(SettingsActivity.RESULT_CODE_LOAD_STATE)
            else -> return true
        }

        return super.onPreferenceTreeClick(preference)
    }

    private fun returnResult(result: Int) {
        /* Finish the settings page and give the calling activity work */
        resultCallback(result)
        activity?.finish()
    }
}