package com.draco.ludere.utils

import android.app.Activity
import android.content.Context
import com.draco.ludere.R
import com.draco.ludere.repositories.Storage
import com.draco.ludere.retroview.RetroView

class RetroViewUtils(private val activity: Activity) {
    private val storage = Storage.getInstance(activity)
    private val sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE)
    private val fastForwardSpeed = activity.resources.getInteger(R.integer.config_fast_forward_multiplier)

    fun restoreEmulatorState(retroView: RetroView) {
        retroView.view.frameSpeed = sharedPreferences.getInt(activity.getString(R.string.pref_frame_speed), 1)
        retroView.view.audioEnabled = sharedPreferences.getBoolean(activity.getString(R.string.pref_audio_enabled), true)
        loadTempState(retroView)
    }

    fun preserveEmulatorState(retroView: RetroView) {
        saveSRAM(retroView)
        saveTempState(retroView)

        with (sharedPreferences.edit()) {
            putInt(activity.getString(R.string.pref_frame_speed), retroView.view.frameSpeed)
            putBoolean(activity.getString(R.string.pref_audio_enabled), retroView.view.audioEnabled)
            apply()
        }
    }

    fun saveSRAM(retroView: RetroView) {
        storage.sram.outputStream().use {
            it.write(retroView.view.serializeSRAM())
        }
    }

    fun loadState(retroView: RetroView) {
        if (!storage.state.exists())
            return

        val stateBytes = storage.state.inputStream().use {
            it.readBytes()
        }

        if (stateBytes.isEmpty())
            return

        retroView.view.unserializeState(stateBytes)
    }

    fun loadTempState(retroView: RetroView) {
        if (!storage.tempState.exists())
            return

        val stateBytes = storage.tempState.inputStream().use {
            it.readBytes()
        }

        if (stateBytes.isEmpty())
            return

        retroView.view.unserializeState(stateBytes)
    }

    fun saveState(retroView: RetroView) {
        storage.state.outputStream().use {
            it.write(retroView.view.serializeState())
        }
    }

    fun saveTempState(retroView: RetroView) {
        storage.tempState.outputStream().use {
            it.write(retroView.view.serializeState())
        }
    }

    fun fastForward(retroView: RetroView) {
        retroView.view.frameSpeed = if (retroView.view.frameSpeed == 1) fastForwardSpeed else 1
    }
}