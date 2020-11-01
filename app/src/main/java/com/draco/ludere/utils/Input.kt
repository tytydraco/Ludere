package com.draco.ludere.utils

import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import com.draco.ludere.R
import com.swordfish.libretrodroid.GLRetroView

class Input(private val context: Context) {
    companion object {
        /* Custom keycodes */
        const val KEYCODE_LOAD_STATE = -1
        const val KEYCODE_SAVE_STATE = -2
        const val KEYCODE_MUTE = -3
        const val KEYCODE_FAST_FORWARD = -4

        /* List of valid keycodes that can be piped */
        val validKeyCodes = listOf(
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

        val keyComboLoadState = listOf(KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_L1)
        val keyComboSaveState = listOf(KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_R1)
        val keyComboMute = listOf(KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_L1)
        val keyComboFastForward = listOf(KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_R1)
    }

    /* Access ROM specific files */
    private val privateData = PrivateData(context)

    /* Keep track of all keys currently pressed down */
    private val pressedKeys = mutableSetOf<Int>()

    private fun keyCombo(keyCodes: List<Int>): Boolean {
        return pressedKeys.containsAll(keyCodes)
    }

    fun handleKeyEvent(retroView: GLRetroView?, keyCode: Int, event: KeyEvent): Boolean {
        if (retroView == null)
            return false

        /* Don't pass through invalid keycodes */
        if (keyCode !in validKeyCodes)
            return false

        /* Keep track of the modifier button states */
        when (event.action) {
            KeyEvent.ACTION_DOWN -> pressedKeys.add(keyCode)
            KeyEvent.ACTION_UP -> pressedKeys.remove(keyCode)
        }

        /* Controller numbers are [1, inf), we need [0, inf) */
        val port = ((event.device?.controllerNumber ?: 1) - 1).coerceAtLeast(0)

        /* Handler modifier keys */
        if (context.resources.getBoolean(R.bool.config_modifier_keys)) {
            when {
                keyCombo(keyComboLoadState) -> RetroViewUtils.loadState(retroView, privateData)
                keyCombo(keyComboSaveState) -> RetroViewUtils.saveState(retroView, privateData)
                keyCombo(keyComboMute) -> RetroViewUtils.toggleMute(retroView)
                keyCombo(keyComboFastForward) -> RetroViewUtils.toggleFastForward(retroView)
            }
        }

        /* Pipe events to the GLRetroView */
        retroView.sendKeyEvent(
            event.action,
            keyCode,
            port
        )

        return true
    }

    fun handleGenericMotionEvent(retroView: GLRetroView?, event: MotionEvent): Boolean {
        if (retroView == null)
            return false

        /* Controller numbers are [1, inf), we need [0, inf) */
        val port = ((event.device?.controllerNumber ?: 1) - 1).coerceAtLeast(0)

        /* Handle analog input events */
        with(retroView) {
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
    }
}