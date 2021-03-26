package com.draco.ludere.input

import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import com.draco.ludere.retroview.RetroView
import com.swordfish.libretrodroid.GLRetroView

class ControllerInput {
    companion object {
        /**
         * Combination to open the menu
         */
        val KEYCOMBO_MENU = setOf(
            KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_BUTTON_SELECT
        )

        /**
         * Any of these keys will not be piped to the RetroView
         */
        val EXCLUDED_KEYS = setOf(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP
        )
    }
    /**
     * Set of keys currently being held by the user
     */
    private val keyLog = mutableSetOf<Int>()

    /**
     * The callback for when the user inputs the menu key-combination
     */
    var menuCallback: () -> Unit = {}

    /**
     *  Controller numbers are [1, inf), we need [0, inf)
     */
    private fun getPort(event: InputEvent): Int =
        ((event.device?.controllerNumber ?: 1) - 1).coerceAtLeast(0)

    /**
     * Check if we should be showing the user the menu
     */
    private fun checkMenuKeyCombo() {
        if (keyLog == KEYCOMBO_MENU)
            menuCallback()
    }

    fun processKeyEvent(keyCode: Int, event: KeyEvent, retroView: RetroView): Boolean {
        /* Block these keys! */
        if (EXCLUDED_KEYS.contains(keyCode))
            return false

        /* We're not ready yet! */
        if (retroView.frameRendered.value == false)
            return true

        val port = getPort(event)
        retroView.view.sendKeyEvent(event.action, keyCode, port)

        /* Keep track of user input events */
        when (event.action) {
            KeyEvent.ACTION_DOWN -> keyLog.add(keyCode)
            KeyEvent.ACTION_UP -> keyLog.remove(keyCode)
        }

        checkMenuKeyCombo()

        return true
    }

    fun processMotionEvent(event: MotionEvent, retroView: RetroView) {
        /* We're not ready yet! */
        if (retroView.frameRendered.value == false)
            return

        val port = getPort(event)
        retroView.view.apply {
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
        }
    }
}