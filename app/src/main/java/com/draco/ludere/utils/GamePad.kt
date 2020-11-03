package com.draco.ludere.utils

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
    private val compositeDisposable = CompositeDisposable()

    private fun eventHandler(event: Event, retroView: GLRetroView) {
        when (event) {
            is Event.Button -> {
                /*
                 * Attempt to override the event to handle unorthodox keycodes, such as save
                 * and load states. If the override handler returns false, the event was not
                 * overridden, so we should pass the event to the GLRetroView as-is.
                 */
                retroView.sendKeyEvent(event.action, event.id)
            }
            is Event.Direction -> {
                when (event.id) {
                    GLRetroView.MOTION_SOURCE_DPAD -> retroView.sendMotionEvent(GLRetroView.MOTION_SOURCE_DPAD, event.xAxis, event.yAxis)
                    GLRetroView.MOTION_SOURCE_ANALOG_LEFT -> retroView.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_LEFT, event.xAxis, event.yAxis)
                    GLRetroView.MOTION_SOURCE_ANALOG_RIGHT -> retroView.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_RIGHT, event.xAxis, event.yAxis)
                }
            }
        }
    }

    fun subscribe(retroView: GLRetroView) {
        /* Clear any existing subscriptions */
        unsubscribe()

        /* Register the observable */
        val inputDisposable = pad.events().subscribe {
            eventHandler(it, retroView)
        }
        compositeDisposable.add(inputDisposable)
    }

    fun unsubscribe() {
        compositeDisposable.clear()
    }
}