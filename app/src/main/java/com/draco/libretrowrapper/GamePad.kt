package com.draco.libretrowrapper

import android.content.Context
import android.view.KeyEvent
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.radialgamepad.library.RadialGamePad
import com.swordfish.radialgamepad.library.config.RadialGamePadConfig
import com.swordfish.radialgamepad.library.event.Event
import io.reactivex.disposables.CompositeDisposable
import java.io.File

class GamePad(
    context: Context,
    padConfig: RadialGamePadConfig,
    private val retroView: GLRetroView
) {
    val pad: RadialGamePad = RadialGamePad(padConfig, 32f, context)
    private val state = File("${context.filesDir.absolutePath}/state")
    private val compositeDisposable = CompositeDisposable()

    fun resume() {
        compositeDisposable.add(pad.events().subscribe {
            eventHandler(it, retroView)
        })
    }

    fun pause() {
        compositeDisposable.clear()
    }

    private fun eventHandler(event: Event, retroView: GLRetroView) {
        when (event) {
            is Event.Button -> {
                when (event.id) {
                    GamePadConfig.KEYCODE_SAVE_STATE -> {
                        if (event.action == KeyEvent.ACTION_DOWN)
                            state.writeBytes(retroView.serializeState())
                    }
                    GamePadConfig.KEYCODE_LOAD_STATE -> {
                        if (event.action == KeyEvent.ACTION_DOWN) {
                            if (state.exists()) {
                                val bytes = state.readBytes()
                                if (bytes.isNotEmpty())
                                    retroView.unserializeState(bytes)
                            }
                        }
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
}