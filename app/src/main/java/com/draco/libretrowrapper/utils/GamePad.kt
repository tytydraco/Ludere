package com.draco.libretrowrapper.utils

import android.content.Context
import android.view.KeyEvent
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.radialgamepad.library.RadialGamePad
import com.swordfish.radialgamepad.library.config.RadialGamePadConfig
import com.swordfish.radialgamepad.library.event.Event
import io.reactivex.disposables.CompositeDisposable

class GamePad(
    context: Context,
    padConfig: RadialGamePadConfig,
    private val privateData: PrivateData
) {
    val pad: RadialGamePad = RadialGamePad(padConfig, 0f, context)
    private val compositeDisposable = CompositeDisposable()

    private fun eventHandler(event: Event, retroView: GLRetroView) {
        when (event) {
            is Event.Button -> {
                when (event.id) {
                    GamePadConfig.KEYCODE_SAVE_STATE -> {
                        if (event.action == KeyEvent.ACTION_DOWN)
                            RetroViewUtils.saveState(retroView, privateData)
                    }
                    GamePadConfig.KEYCODE_LOAD_STATE -> {
                        if (event.action == KeyEvent.ACTION_DOWN)
                            RetroViewUtils.loadState(retroView, privateData)
                    }
                    else -> retroView.sendKeyEvent(event.action, event.id)
                }
            }
            is Event.Direction -> {
                when (event.id) {
                    GLRetroView.MOTION_SOURCE_DPAD -> {
                        retroView.sendMotionEvent(GLRetroView.MOTION_SOURCE_DPAD, event.xAxis, event.yAxis)
                    }
                    GLRetroView.MOTION_SOURCE_ANALOG_LEFT -> {
                        retroView.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_LEFT, event.xAxis, event.yAxis)
                    }
                    GLRetroView.MOTION_SOURCE_ANALOG_RIGHT -> {
                        retroView.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_RIGHT, event.xAxis, event.yAxis)
                    }
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