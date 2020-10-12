package com.draco.libretrowrapper

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.radialgamepad.library.RadialGamePad
import com.swordfish.radialgamepad.library.config.RadialGamePadConfig
import com.swordfish.radialgamepad.library.event.Event
import io.reactivex.disposables.CompositeDisposable
import java.io.File

class GamePad(
    context: Context,
    padConfig: RadialGamePadConfig,
    private val parent: FrameLayout,
    private val safeGLRV: SafeGLRV
): Fragment() {
    val pad: RadialGamePad = RadialGamePad(padConfig, 32f, context)

    private val state = File("${context.filesDir.absolutePath}/state")
    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parent.addView(pad)
    }

    private fun save() {
        safeGLRV.safe {
            state.writeBytes(it.serializeState())
        }
    }

    private fun load() {
        if (!state.exists())
            return

        safeGLRV.safe {
            val bytes = state.readBytes()
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

    override fun onResume() {
        super.onResume()
        safeGLRV.safe {
            compositeDisposable.add(pad.events().subscribe {
                eventHandler(it, safeGLRV.unsafeGLRetroView)
            })
        }
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }
}