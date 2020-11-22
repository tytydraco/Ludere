package com.draco.ludere.gamepad

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.draco.ludere.R
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.radialgamepad.library.config.*
import java.lang.Exception

class GamePadConfig(
    context: Context,
    private val resources: Resources
) {
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val vibrate = sharedPreferences.getBoolean(context.getString(R.string.settings_gamepad_vibrate_key), true)

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
        normalColor = try {
            val color = sharedPreferences.getString(context.getString(R.string.settings_button_color_key), "#88424242")!!
            Color.parseColor(color)
        } catch (_: Exception) {
            Color.parseColor("#88424242")
        },
        pressedColor = try {
            val color = sharedPreferences.getString(context.getString(R.string.settings_pressed_button_color_key), "#88212121")!!
            Color.parseColor(color)
        } catch (_: Exception) {
            Color.parseColor("#88212121")
        },
        textColor = try {
            val color = sharedPreferences.getString(context.getString(R.string.settings_button_icon_color_key), "#88ffffff")!!
            Color.parseColor(color)
        } catch (_: Exception) {
            Color.parseColor("#88ffffff")
        }
    )

    val left = RadialGamePadConfig(
        haptic = vibrate,
        theme = radialGamePadTheme,
        sockets = 12,
        primaryDial = PrimaryDialConfig.Cross(GLRetroView.MOTION_SOURCE_DPAD),
        secondaryDials = listOfNotNull(
            SecondaryDialConfig.SingleButton(2, 1, BUTTON_L2).takeIf { resources.getBoolean(R.bool.config_gamepad_l2) },
            SecondaryDialConfig.SingleButton(3, 1, BUTTON_L1).takeIf { resources.getBoolean(R.bool.config_gamepad_l1) },
            SecondaryDialConfig.SingleButton(4, 1, BUTTON_SELECT).takeIf { resources.getBoolean(R.bool.config_gamepad_select) },
            SecondaryDialConfig.Stick(9, 2f, GLRetroView.MOTION_SOURCE_ANALOG_LEFT, KeyEvent.KEYCODE_BUTTON_THUMBL).takeIf { resources.getBoolean(R.bool.config_gamepad_analog_left) }
        )
    )

    val right = RadialGamePadConfig(
        haptic = vibrate,
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
            SecondaryDialConfig.SingleButton(2, 1, BUTTON_START).takeIf { resources.getBoolean(R.bool.config_gamepad_start) },
            SecondaryDialConfig.SingleButton(3, 1, BUTTON_R1).takeIf { resources.getBoolean(R.bool.config_gamepad_r1) },
            SecondaryDialConfig.SingleButton(4, 1, BUTTON_R2).takeIf { resources.getBoolean(R.bool.config_gamepad_r2) },
            SecondaryDialConfig.Stick(8, 2f, GLRetroView.MOTION_SOURCE_ANALOG_RIGHT, KeyEvent.KEYCODE_BUTTON_THUMBR).takeIf { resources.getBoolean(R.bool.config_gamepad_analog_right) }
        )
    )
}