package com.draco.libretrowrapper.utils

import android.view.KeyEvent
import android.view.MotionEvent
import com.swordfish.libretrodroid.GLRetroView

class Input {
    fun handleKeyEvent(retroView: GLRetroView?, keyCode: Int, event: KeyEvent): Boolean {
        /* Pipe the keycode to the GLRetroView */
        retroView?.sendKeyEvent(event.action, keyCode)
        return true
    }

    fun handleGenericMotionEvent(retroView: GLRetroView?, event: MotionEvent): Boolean {
        /* Handle analog input events */
        if (retroView != null) with(retroView) {
            sendMotionEvent(
                GLRetroView.MOTION_SOURCE_DPAD,
                event.getAxisValue(MotionEvent.AXIS_HAT_X),
                event.getAxisValue(MotionEvent.AXIS_HAT_Y)
            )
            sendMotionEvent(
                GLRetroView.MOTION_SOURCE_ANALOG_LEFT,
                event.getAxisValue(MotionEvent.AXIS_X),
                event.getAxisValue(MotionEvent.AXIS_Y)
            )
            sendMotionEvent(
                GLRetroView.MOTION_SOURCE_ANALOG_RIGHT,
                event.getAxisValue(MotionEvent.AXIS_Z),
                event.getAxisValue(MotionEvent.AXIS_RZ)
            )
            return true
        }

        return false
    }
}