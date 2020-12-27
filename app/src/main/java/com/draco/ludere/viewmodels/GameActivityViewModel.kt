package com.draco.ludere.viewmodels

import android.app.Application
import android.app.Service
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.input.InputManager
import android.os.Build
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import com.draco.ludere.R
import com.draco.ludere.gamepad.GamePad
import com.draco.ludere.gamepad.GamePadConfig
import com.draco.ludere.models.Storage
import com.draco.ludere.utils.KeyCodes
import com.draco.ludere.utils.RetroView
import com.draco.ludere.utils.SingleLiveEvent
import com.swordfish.libretrodroid.GLRetroView
import io.reactivex.disposables.CompositeDisposable
import java.io.File

class GameActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>().applicationContext

    val storage = Storage.getInstance(context)
    var retroView = RetroView(context)
    val menuOnClickListener = MenuOnClickListener()

    val updateVisibility = SingleLiveEvent<Boolean>()
    val showMenu = SingleLiveEvent<Boolean>()

    private val gamePadConfig = GamePadConfig(context, context.resources)
    val leftGamePad = GamePad(context, gamePadConfig.left)
    val rightGamePad = GamePad(context, gamePadConfig.right)

    private val inputManager = context.getSystemService(Service.INPUT_SERVICE) as InputManager
    private val pressedKeys = mutableSetOf<Int>()
    private val compositeDisposable = CompositeDisposable()

    init {
        inputManager.registerInputDeviceListener(object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) { updateVisibility.call() }
            override fun onInputDeviceRemoved(deviceId: Int) { updateVisibility.call() }
            override fun onInputDeviceChanged(deviceId: Int) { updateVisibility.call() }
        }, null)
    }

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

    fun shouldShowGamePads(): Boolean {
        val hasTouchScreen = context.packageManager?.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
        if (hasTouchScreen == null || hasTouchScreen == false)
            return false

        val currentDisplayId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            context.display!!.displayId
        else {
            val wm = context.getSystemService(AppCompatActivity.WINDOW_SERVICE) as WindowManager
            wm.defaultDisplay.displayId
        }

        val dm = context.getSystemService(Service.DISPLAY_SERVICE) as DisplayManager
        if (dm.getDisplay(currentDisplayId).flags and Display.FLAG_PRESENTATION == Display.FLAG_PRESENTATION)
            return false

        for (id in InputDevice.getDeviceIds()) {
            InputDevice.getDevice(id).apply {
                if (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD)
                    return false
            }
        }

        return true
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

    fun handleKeyDown(keyCode: Int, event: KeyEvent) {
        /* Controller numbers are [1, inf), we need [0, inf) */
        val port = ((event.device?.controllerNumber ?: 1) - 1).coerceAtLeast(0)

        pressedKeys.add(keyCode)
        retroView.view!!.sendKeyEvent(
            event.action,
            keyCode,
            port
        )

        if (pressedKeys == KeyCodes.KeyComboMenu)
            showMenu.call()
    }

    fun handleKeyUp(keyCode: Int, event: KeyEvent) {
        /* Controller numbers are [1, inf), we need [0, inf) */
        val port = ((event.device?.controllerNumber ?: 1) - 1).coerceAtLeast(0)

        pressedKeys.remove(keyCode)
        retroView.view!!.sendKeyEvent(
            event.action,
            keyCode,
            port
        )
    }

    fun handleGenericMotionEvent(event: MotionEvent) {
        /* Controller numbers are [1, inf), we need [0, inf) */
        val port = ((event.device?.controllerNumber ?: 1) - 1).coerceAtLeast(0)

        retroView.view!!.apply {
            sendMotionEvent(
                GLRetroView.MOTION_SOURCE_DPAD,
                event.getAxisValue(MotionEvent.AXIS_HAT_X),
                event.getAxisValue(MotionEvent.AXIS_HAT_Y),
                port
            )
            sendMotionEvent(
                GLRetroView.MOTION_SOURCE_ANALOG_LEFT,
                event.getAxisValue(MotionEvent.AXIS_X),
                event.getAxisValue(MotionEvent.AXIS_Y),
                port
            )
            sendMotionEvent(
                GLRetroView.MOTION_SOURCE_ANALOG_RIGHT,
                event.getAxisValue(MotionEvent.AXIS_Z),
                event.getAxisValue(MotionEvent.AXIS_RZ),
                port
            )
        }
    }
}