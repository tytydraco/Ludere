package com.draco.ludere.gamepad

import android.app.Service
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.InputDevice
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.radialgamepad.library.RadialGamePad
import com.swordfish.radialgamepad.library.config.RadialGamePadConfig
import com.swordfish.radialgamepad.library.event.Event
import io.reactivex.disposables.CompositeDisposable

class GamePad(
    context: Context,
    padConfig: RadialGamePadConfig,
) {
    val pad = RadialGamePad(padConfig, 0f, context)

    companion object {
        /**
         * Should the user see the on-screen controls?
         */
        fun shouldShowGamePads(context: Context): Boolean {
            /* Devices without a touchscreen don't need a GamePad */
            val hasTouchScreen = context.packageManager?.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
            if (hasTouchScreen == null || hasTouchScreen == false)
                return false

            /* Fetch the current display that the game is running on */
            val currentDisplayId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                context.display!!.displayId
            else {
                val wm = context.getSystemService(AppCompatActivity.WINDOW_SERVICE) as WindowManager
                wm.defaultDisplay.displayId
            }

            /* Are we presenting this screen on a TV or display? */
            val dm = context.getSystemService(Service.DISPLAY_SERVICE) as DisplayManager
            if (dm.getDisplay(currentDisplayId).flags and Display.FLAG_PRESENTATION == Display.FLAG_PRESENTATION)
                return false

            /* If a GamePad is connected, we definitely don't need touch controls */
            for (id in InputDevice.getDeviceIds()) {
                InputDevice.getDevice(id).apply {
                    if (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD)
                        return false
                }
            }

            return true
        }
    }

    private fun eventHandler(event: Event, retroView: GLRetroView) {
        when (event) {
            is Event.Button -> retroView.sendKeyEvent(event.action, event.id)
            is Event.Direction -> when (event.id) {
                GLRetroView.MOTION_SOURCE_DPAD -> retroView.sendMotionEvent(GLRetroView.MOTION_SOURCE_DPAD, event.xAxis, event.yAxis)
                GLRetroView.MOTION_SOURCE_ANALOG_LEFT -> retroView.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_LEFT, event.xAxis, event.yAxis)
                GLRetroView.MOTION_SOURCE_ANALOG_RIGHT -> retroView.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_RIGHT, event.xAxis, event.yAxis)
            }
        }
    }

    fun subscribe(compositeDisposable: CompositeDisposable, retroView: GLRetroView) {
        val inputDisposable = pad.events().subscribe {
            eventHandler(it, retroView)
        }
        compositeDisposable.add(inputDisposable)
    }
}