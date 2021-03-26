package com.draco.ludere.utils

import android.app.Activity
import android.content.Context
import com.draco.ludere.R
import com.draco.ludere.repositories.Storage

class RetroViewUtils(
    private val activity: Activity,
    private val retroView: RetroView
) {
    private val storage = Storage.getInstance(activity)
    private val sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE)

    fun restoreEmulatorState() {
        retroView.view.frameSpeed = sharedPreferences.getInt(activity.getString(R.string.pref_frame_speed), 1)
        retroView.view.audioEnabled = sharedPreferences.getBoolean(activity.getString(R.string.pref_audio_enabled), true)
        loadState()
    }

    fun preserveEmulatorState() {
        saveSRAM()
        saveState()

        with (sharedPreferences.edit()) {
            putInt(activity.getString(R.string.pref_frame_speed), retroView.view.frameSpeed)
            putBoolean(activity.getString(R.string.pref_audio_enabled), retroView.view.audioEnabled)
            apply()
        }
    }

    fun saveSRAM() {
        storage.sram.outputStream().use {
            it.write(retroView.view.serializeSRAM())
        }
    }

    fun loadState() {
        if (!storage.state.exists())
            return

        val stateBytes = storage.state.inputStream().use {
            it.readBytes()
        }

        if (stateBytes.isEmpty())
            return

        retroView.view.unserializeState(stateBytes)
    }

    fun saveState() {
        storage.state.outputStream().use {
            it.write(retroView.view.serializeState())
        }
    }
}