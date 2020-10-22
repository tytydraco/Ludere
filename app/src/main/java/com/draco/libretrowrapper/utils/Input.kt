package com.draco.libretrowrapper.utils

import android.view.KeyEvent
import android.view.MotionEvent
import com.swordfish.libretrodroid.GLRetroView

class Input {
    companion object {
        /* Custom keycodes */
        const val KEYCODE_LOAD_STATE = -1
        const val KEYCODE_SAVE_STATE = -2

        /* List of valid keycodes that can be piped */
        private val validKeyCodes = listOf(
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_X,
            KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_L2,
            KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_BUTTON_THUMBL,
            KeyEvent.KEYCODE_BUTTON_THUMBR,
            KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_BUTTON_SELECT
        )

        fun handleKeyEvent(retroView: GLRetroView?, keyCode: Int, event: KeyEvent): Boolean {
            /* Don't pass through invalid keycodes */
            if (keyCode !in validKeyCodes)
                return false

            /* Controller numbers are [1, inf), we need [0, inf) */
            val port = ((event.device?.controllerNumber ?: 1) - 1).coerceAtLeast(0)

            /* Pipe the keycode to the GLRetroView */
            retroView?.sendKeyEvent(
                event.action,
                keyCode,
                port
            )
            return true
        }

        fun handleGenericMotionEvent(retroView: GLRetroView?, event: MotionEvent): Boolean {
            /* Controller numbers are [1, inf), we need [0, inf) */
            val port = ((event.device?.controllerNumber ?: 1) - 1).coerceAtLeast(0)

            /* Handle analog input events */
            if (retroView != null) with(retroView) {
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
                return true
            }

            return false
        }
    }
}