package com.draco.ludere.activities

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.draco.ludere.R

class MainPreferenceFragment : PreferenceFragmentCompat() {
    companion object {
        const val REQUEST_CODE_CHOOSE_ROM = 1
    }

    private val chooseROMIntent = Intent()
        .setType("*/*")
        .setAction(Intent.ACTION_OPEN_DOCUMENT)

    private lateinit var gameActivityIntent: Intent

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main, rootKey)
        gameActivityIntent = Intent(context, GameActivity::class.java)
    }

    private fun chooseROM() {
        startActivityForResult(Intent.createChooser(chooseROMIntent, null), REQUEST_CODE_CHOOSE_ROM)
    }

    private fun start() {
        startActivity(gameActivityIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data?.data != null) {
            val path = data.data!!.toString()
            Log.d("path", path)
            gameActivityIntent.putExtra("rom_uri", path)
        }
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        if (preference != null) when (preference.key) {
            "choose_rom" -> chooseROM()
            "start" -> start()
        }

        return super.onPreferenceTreeClick(preference)
    }
}