package com.draco.libretrowrapper

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
    private val safeGLRV: SafeGLRV,
    private val privateData: PrivateData
) {
    val pad: RadialGamePad = RadialGamePad(padConfig, 16f, context)
    private val compositeDisposable = CompositeDisposable()

    private fun save() {
        safeGLRV.safe {
            privateData.state.writeBytes(it.serializeState())
        }
    }

    private fun load() {
        if (!privateData.state.exists())
            return

        safeGLRV.safe {
            val bytes = privateData.state.readBytes()
            if (bytes.isNotEmpty())
                it.unserializeState(bytes)
        }
    }

    private fun eventHandler(event: Event, retroView: GLRetroView) {
        when (event) {
            is Event.Button -> {
                when (event.id) {
                    GamePadConfig.KEYCODE_SAVE_STATE -> {
                        if (event.action == KeyEvent.ACTION_DOWN)
                            save()
                    }
                    GamePadConfig.KEYCODE_LOAD_STATE -> {
                        if (event.action == KeyEvent.ACTION_DOWN)
                            load()
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

    fun resume() {
        safeGLRV.safe {
            compositeDisposable.add(pad.events().subscribe {
                eventHandler(it, safeGLRV.unsafeGLRetroView)
            })
        }
    }

    fun pause() {
        compositeDisposable.clear()
    }
}