package com.draco.ludere.gamepad

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.draco.ludere.R
import com.swordfish.radialgamepad.library.config.*
import com.swordfish.radialgamepad.library.haptics.HapticConfig

class GamePadConfig(
    context: Context,
    private val resources: Resources
) {
    companion object {
        val BUTTON_START = ButtonConfig(
            id = KeyEvent.KEYCODE_BUTTON_START,
            label = "+"
        )

        val BUTTON_SELECT = ButtonConfig(
            id = KeyEvent.KEYCODE_BUTTON_SELECT,
            label = "-"
        )

        val BUTTON_L1 = ButtonConfig(
            id = KeyEvent.KEYCODE_BUTTON_L1,
            label = "L"
        )

        val BUTTON_R1 = ButtonConfig(
            id = KeyEvent.KEYCODE_BUTTON_R1,
            label = "R"
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

        val LEFT_DPAD = PrimaryDialConfig.Cross(CrossConfig(0))
        val LEFT_ANALOG = PrimaryDialConfig.Stick(0)
    }

    private val radialGamePadTheme = RadialGamePadTheme(
        primaryDialBackground = Color.TRANSPARENT,
        textColor = ContextCompat.getColor(context, R.color.gamepad_icon_color),
        normalColor = ContextCompat.getColor(context, R.color.gamepad_button_color),
        pressedColor = ContextCompat.getColor(context, R.color.gamepad_pressed_color)
    )

    val left = RadialGamePadConfig(
        haptic = if (resources.getBoolean(R.bool.config_gamepad_haptic)) HapticConfig.PRESS else HapticConfig.OFF,
        theme = radialGamePadTheme,
        sockets = 12,
        primaryDial = if (resources.getBoolean(R.bool.config_left_analog)) LEFT_ANALOG else LEFT_DPAD,
        secondaryDials = listOfNotNull(
            SecondaryDialConfig.SingleButton(4, 1, BUTTON_L1).takeIf { resources.getBoolean(R.bool.config_gamepad_l1) },
            SecondaryDialConfig.SingleButton(10, 1, BUTTON_SELECT).takeIf { resources.getBoolean(R.bool.config_gamepad_select) },
        )
    )

    val right = RadialGamePadConfig(
        haptic = if (resources.getBoolean(R.bool.config_gamepad_haptic)) HapticConfig.PRESS else HapticConfig.OFF,
        theme = radialGamePadTheme,
        sockets = 12,
        primaryDial = PrimaryDialConfig.PrimaryButtons(
            dials = listOfNotNull(
                BUTTON_A.takeIf { resources.getBoolean(R.bool.config_gamepad_a) },
                BUTTON_X.takeIf { resources.getBoolean(R.bool.config_gamepad_x) },
                BUTTON_Y.takeIf { resources.getBoolean(R.bool.config_gamepad_y) },
                BUTTON_B.takeIf { resources.getBoolean(R.bool.config_gamepad_b) }
            )
        ),
        secondaryDials = listOfNotNull(
            SecondaryDialConfig.SingleButton(2, 1, BUTTON_R1).takeIf { resources.getBoolean(R.bool.config_gamepad_r1) },
            SecondaryDialConfig.SingleButton(8, 1, BUTTON_START).takeIf { resources.getBoolean(R.bool.config_gamepad_start) },
        )
    )
}