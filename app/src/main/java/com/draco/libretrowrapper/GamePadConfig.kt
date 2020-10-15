package com.draco.libretrowrapper

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.radialgamepad.library.config.*

class GamePadConfig(
    context: Context,
    private val resources: Resources
) {
    companion object {
        const val KEYCODE_LOAD_STATE = -1
        const val KEYCODE_SAVE_STATE = -2
        const val KEYCODE_FAST_FORWARD = -3
        const val KEYCODE_MUTE = -4

        val BUTTON_START = ButtonConfig(
            id = KeyEvent.KEYCODE_BUTTON_START,
            iconId = R.drawable.ic_baseline_play_arrow_24
        )

        val BUTTON_SELECT = ButtonConfig(
            id = KeyEvent.KEYCODE_BUTTON_SELECT,
            iconId = R.drawable.ic_baseline_stop_24
        )

        val BUTTON_SAVE_STATE = ButtonConfig(
            id = KEYCODE_SAVE_STATE,
            iconId = R.drawable.ic_baseline_save_24
        )

        val BUTTON_LOAD_STATE = ButtonConfig(
            id = KEYCODE_LOAD_STATE,
            iconId = R.drawable.ic_baseline_get_app_24
        )

        val BUTTON_FAST_FORWARD = ButtonConfig(
            id = KEYCODE_FAST_FORWARD,
            iconId = R.drawable.ic_baseline_fast_forward_24
        )

        val BUTTON_MUTE = ButtonConfig(
            id = KEYCODE_MUTE,
            iconId = R.drawable.ic_baseline_volume_up_24
        )

        val BUTTON_L1 = ButtonConfig(
            id = KeyEvent.KEYCODE_BUTTON_L1,
            label = "L"
        )

        val BUTTON_R1 = ButtonConfig(
            id = KeyEvent.KEYCODE_BUTTON_R1,
            label = "R"
        )

        val BUTTON_L2 = ButtonConfig(
            id = KeyEvent.KEYCODE_BUTTON_L2,
            label = "L2"
        )

        val BUTTON_R2 = ButtonConfig(
            id = KeyEvent.KEYCODE_BUTTON_R2,
            label = "R2"
        )

        val BUTTON_A = ButtonConfig(
            id = KeyEvent.KEYCODE_BUTTON_A,
            label = "A"
        )

        val BUTTON_B = ButtonConfig(
            id = KeyEvent.KEYCODE_BUTTON_B,
            label = "B"
        )

        val BUTTON_X = ButtonConfig(
            id = KeyEvent.KEYCODE_BUTTON_X,
            label = "X"
        )

        val BUTTON_Y = ButtonConfig(
            id = KeyEvent.KEYCODE_BUTTON_Y,
            label = "Y"
        )
    }

    private val radialGamePadTheme = RadialGamePadTheme(
        primaryDialBackground = Color.TRANSPARENT,
        textColor = ContextCompat.getColor(context, R.color.rom_gamepad_icon_color),
        normalColor = ContextCompat.getColor(context, R.color.rom_gamepad_button_color),
        pressedColor = ContextCompat.getColor(context, R.color.rom_gamepad_pressed_color)
    )

    val left = RadialGamePadConfig(
        theme = radialGamePadTheme,
        sockets = 12,
        primaryDial = PrimaryDialConfig.Cross(GLRetroView.MOTION_SOURCE_DPAD),
        secondaryDials = listOfNotNull(
            SecondaryDialConfig.SingleButton(1, 1, BUTTON_MUTE).takeIf { resources.getBoolean(R.bool.rom_gamepad_mute) },
            SecondaryDialConfig.SingleButton(2, 1, BUTTON_L2).takeIf { resources.getBoolean(R.bool.rom_gamepad_l2) },
            SecondaryDialConfig.SingleButton(3, 1, BUTTON_L1).takeIf { resources.getBoolean(R.bool.rom_gamepad_l1) },
            SecondaryDialConfig.SingleButton(4, 1, BUTTON_SELECT).takeIf { resources.getBoolean(R.bool.rom_gamepad_select) },
            SecondaryDialConfig.SingleButton(8, 1, BUTTON_SAVE_STATE).takeIf { resources.getBoolean(R.bool.rom_gamepad_save_state) },
            SecondaryDialConfig.Stick(9, 2f, GLRetroView.MOTION_SOURCE_ANALOG_LEFT, KeyEvent.KEYCODE_BUTTON_THUMBL).takeIf { resources.getBoolean(R.bool.rom_gamepad_analog_left) }
        )
    )

    val right = RadialGamePadConfig(
        theme = radialGamePadTheme,
        sockets = 12,
        primaryDial = PrimaryDialConfig.PrimaryButtons(
            dials = listOfNotNull(
                BUTTON_A.takeIf { resources.getBoolean(R.bool.rom_gamepad_a) },
                BUTTON_X.takeIf { resources.getBoolean(R.bool.rom_gamepad_x) },
                BUTTON_Y.takeIf { resources.getBoolean(R.bool.rom_gamepad_y) },
                BUTTON_B.takeIf { resources.getBoolean(R.bool.rom_gamepad_b) }
            )
        ),
        secondaryDials = listOfNotNull(
            SecondaryDialConfig.SingleButton(2, 1, BUTTON_START).takeIf { resources.getBoolean(R.bool.rom_gamepad_start) },
            SecondaryDialConfig.SingleButton(3, 1, BUTTON_R1).takeIf { resources.getBoolean(R.bool.rom_gamepad_r1) },
            SecondaryDialConfig.SingleButton(4, 1, BUTTON_R2).takeIf { resources.getBoolean(R.bool.rom_gamepad_r2) },
            SecondaryDialConfig.SingleButton(5, 1, BUTTON_FAST_FORWARD).takeIf { resources.getBoolean(R.bool.rom_gamepad_fast_forward) },
            SecondaryDialConfig.SingleButton(10, 1, BUTTON_LOAD_STATE).takeIf { resources.getBoolean(R.bool.rom_gamepad_load_state) },
            SecondaryDialConfig.Stick(8, 2f, GLRetroView.MOTION_SOURCE_ANALOG_RIGHT, KeyEvent.KEYCODE_BUTTON_THUMBR).takeIf { resources.getBoolean(R.bool.rom_gamepad_analog_right) }
        )
    )
}