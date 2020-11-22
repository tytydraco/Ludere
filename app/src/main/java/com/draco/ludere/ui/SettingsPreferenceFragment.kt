package com.draco.ludere.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.draco.ludere.R

class SettingsPreferenceFragment: PreferenceFragmentCompat() {
    companion object {
        const val REQUEST_CODE_CHOOSE_ROM = 1
        const val REQUEST_CODE_CHOOSE_CORE = 2
    }

    lateinit var sharedPreferences: SharedPreferences

    /* Calling activity must set a work callback */
    lateinit var resultCallback: (Int) -> Unit

    private val chooseFileIntent = Intent()
        .setType("*/*")
        .setAction(Intent.ACTION_OPEN_DOCUMENT)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

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
            getString(R.string.settings_resume_key) -> activity?.finish()
            getString(R.string.settings_select_rom_key) -> {
                startActivityForResult(Intent.createChooser(chooseFileIntent, null), REQUEST_CODE_CHOOSE_ROM)
            }
            getString(R.string.settings_select_core_key) -> {
                startActivityForResult(Intent.createChooser(chooseFileIntent, null), REQUEST_CODE_CHOOSE_CORE)
            }
            getString(R.string.settings_manage_gamepad_key) -> {
                val intent = Intent(activity, GamepadActivity::class.java)
                activity?.startActivity(intent)
            }
            getString(R.string.settings_save_state_key) -> returnResult(SettingsActivity.RESULT_CODE_SAVE_STATE)
            getString(R.string.settings_load_state_key) -> returnResult(SettingsActivity.RESULT_CODE_LOAD_STATE)
            else -> return true
        }

        return super.onPreferenceTreeClick(preference)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data?.data != null) {
            val key = when (requestCode) {
                REQUEST_CODE_CHOOSE_ROM -> SettingsActivity.PREFERENCE_KEY_ROM_URI
                REQUEST_CODE_CHOOSE_CORE -> SettingsActivity.PREFERENCE_KEY_CORE_URI
                else -> return
            }

            val path = data.data!!.toString()
            sharedPreferences.edit().apply {
                putString(key, path)
                apply()
            }
        }
    }

    private fun returnResult(result: Int) {
        /* Finish the settings page and give the calling activity work */
        resultCallback(result)
        activity?.finish()
    }
}