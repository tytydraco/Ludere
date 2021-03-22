package com.draco.ludere.viewmodels

import android.app.Application
import android.content.DialogInterface
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import com.draco.ludere.R
import com.draco.ludere.utils.GamePad
import com.draco.ludere.utils.GamePadConfig
import com.draco.ludere.models.Storage
import com.draco.ludere.utils.RetroView
import io.reactivex.disposables.CompositeDisposable
import java.io.File

class GameActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>().applicationContext

    val storage = Storage.getInstance(context)
    var retroView = RetroView(context)
    val menuOnClickListener = MenuOnClickListener()

    private val gamePadConfig = GamePadConfig(context, context.resources)
    val leftGamePad = GamePad(context, gamePadConfig.left)
    val rightGamePad = GamePad(context, gamePadConfig.right)

    private val compositeDisposable = CompositeDisposable()

    inner class MenuOnClickListener : DialogInterface.OnClickListener {
        val menuOptions = arrayOf(
            context.getString(R.string.menu_reset),
            context.getString(R.string.menu_save_state),
            context.getString(R.string.menu_load_state),
            context.getString(R.string.menu_mute),
            context.getString(R.string.menu_fast_forward)
        )

        override fun onClick(dialog: DialogInterface?, which: Int) {
            if (retroView.view != null) when (menuOptions[which]) {
                context.getString(R.string.menu_reset) -> retroView.view!!.reset()
                context.getString(R.string.menu_save_state) -> saveStateTo(storage.state)
                context.getString(R.string.menu_load_state) -> loadStateFrom(storage.state)
                context.getString(R.string.menu_mute) -> retroView.view!!.audioEnabled = !retroView.view!!.audioEnabled
                context.getString(R.string.menu_fast_forward) -> retroView.view!!.frameSpeed = if (retroView.view!!.frameSpeed == 1) 2 else 1
            }
        }
    }

    fun subscribeGamePads() {
        leftGamePad.subscribe(compositeDisposable, retroView.view!!)
        rightGamePad.subscribe(compositeDisposable, retroView.view!!)
    }

    fun restoreEmulatorState(sharedPreferences: SharedPreferences) {
        retroView.view?.frameSpeed = sharedPreferences.getInt(context.getString(R.string.pref_frame_speed), 1)
        retroView.view?.audioEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_audio_enabled), true)
        loadStateFrom(storage.tempState)
    }

    fun preserveEmulatorState(sharedPreferences: SharedPreferences) {
        saveSRAMTo(storage.sram)
        saveStateTo(storage.tempState)
        with (sharedPreferences.edit()) {
            putInt(context.getString(R.string.pref_frame_speed), retroView.view!!.frameSpeed)
            putBoolean(context.getString(R.string.pref_audio_enabled), retroView.view!!.audioEnabled)
            apply()
        }
    }

    fun saveSRAMTo(file: File) {
        file.outputStream().use {
            it.write(retroView.view!!.serializeSRAM())
        }
    }

    fun loadStateFrom(file: File) {
        if (!file.exists())
            return

        val stateBytes = file.inputStream().use {
            it.readBytes()
        }

        if (stateBytes.isEmpty())
            return

        retroView.view?.unserializeState(stateBytes)
    }

    fun saveStateTo(file: File) {
        file.outputStream().use {
            it.write(retroView.view!!.serializeState())
        }
    }
}