package com.draco.ludere.gamepad

import android.content.Context
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.radialgamepad.library.RadialGamePad
import com.swordfish.radialgamepad.library.config.RadialGamePadConfig
import com.swordfish.radialgamepad.library.event.Event
import io.reactivex.disposables.CompositeDisposable

class GamePad(
    context: Context,
    padConfig: RadialGamePadConfig,
) {
    val pad: RadialGamePad = RadialGamePad(padConfig, 0f, context)

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