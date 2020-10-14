package com.draco.libretrowrapper

import android.graphics.Color
import android.view.KeyEvent
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.radialgamepad.library.config.*

class GamePadConfig {
    companion object {
        const val KEYCODE_LOAD_STATE = -1
        const val KEYCODE_SAVE_STATE = -2

        private val radialGamePadTheme = RadialGamePadTheme(
            primaryDialBackground = Color.TRANSPARENT,
            textColor = Color.WHITE
        )

        private val BUTTON_START = ButtonConfig(
            id = KeyEvent.KEYCODE_BUTTON_START,
            iconId = R.drawable.ic_baseline_play_arrow_24
        )

        private val BUTTON_SELECT = ButtonConfig(
            id = KeyEvent.KEYCODE_BUTTON_SELECT,
            iconId = R.drawable.ic_baseline_stop_24
        )

        private val BUTTON_SAVE_STATE = ButtonConfig(
            id = KEYCODE_SAVE_STATE,
            iconId = R.drawable.ic_baseline_save_24
        )

        private val BUTTON_LOAD_STATE = ButtonConfig(
            id = KEYCODE_LOAD_STATE,
            iconId = R.drawable.ic_baseline_get_app_24
        )

        private val BUTTON_L1 = ButtonConfig(
            id = KeyEvent.KEYCODE_BUTTON_L1,
            label = "L"
        )

        private val BUTTON_R1 = ButtonConfig(
            id = KeyEvent.KEYCODE_BUTTON_R1,
            label = "R"
        )

        private val BUTTON_L2 = ButtonConfig(
            id = KeyEvent.KEYCODE_BUTTON_L2,
            label = "L2"
        )

        private val BUTTON_R2 = ButtonConfig(
            id = KeyEvent.KEYCODE_BUTTON_R2,
            label = "R2"
        )

        private val BUTTON_A = ButtonConfig(
            id = KeyEvent.KEYCODE_BUTTON_A,
            label = "A"
        )

        private val BUTTON_B = ButtonConfig(
            id = KeyEvent.KEYCODE_BUTTON_B,
            label = "B"
        )

        private val BUTTON_X = ButtonConfig(
            id = KeyEvent.KEYCODE_BUTTON_X,
            label = "X"
        )

        private val BUTTON_Y = ButtonConfig(
            id = KeyEvent.KEYCODE_BUTTON_Y,
            label = "Y"
        )

        val Type1Left = RadialGamePadConfig(
            theme = radialGamePadTheme,
            sockets = 12,
            primaryDial = PrimaryDialConfig.Cross(GLRetroView.MOTION_SOURCE_DPAD),
            secondaryDials = listOf(
                SecondaryDialConfig.SingleButton(3, 1, BUTTON_SAVE_STATE),
                SecondaryDialConfig.SingleButton(4, 1, BUTTON_SELECT)
            )
        )

        val Type1Right = RadialGamePadConfig(
            theme = radialGamePadTheme,
            sockets = 12,
            primaryDial = PrimaryDialConfig.PrimaryButtons(
                dials = listOf(
                    BUTTON_A,
                    BUTTON_B
                )
            ),
            secondaryDials = listOf(
                SecondaryDialConfig.SingleButton(2, 1, BUTTON_START),
                SecondaryDialConfig.SingleButton(3, 1, BUTTON_LOAD_STATE)
            )
        )

        val Type2Left = RadialGamePadConfig(
            theme = radialGamePadTheme,
            sockets = 12,
            primaryDial = PrimaryDialConfig.Cross(GLRetroView.MOTION_SOURCE_DPAD),
            secondaryDials = listOf(
                SecondaryDialConfig.SingleButton(2, 1, BUTTON_L1),
                SecondaryDialConfig.SingleButton(3, 1, BUTTON_SAVE_STATE),
                SecondaryDialConfig.SingleButton(4, 1, BUTTON_SELECT)
            )
        )

        val Type2Right = RadialGamePadConfig(
            theme = radialGamePadTheme,
            sockets = 12,
            primaryDial = PrimaryDialConfig.PrimaryButtons(
                dials = listOf(
                    BUTTON_A,
                    BUTTON_X,
                    BUTTON_Y,
                    BUTTON_B
                )
            ),
            secondaryDials = listOf(
                SecondaryDialConfig.SingleButton(2, 1, BUTTON_START),
                SecondaryDialConfig.SingleButton(3, 1, BUTTON_LOAD_STATE),
                SecondaryDialConfig.SingleButton(4, 1, BUTTON_R1)
            )
        )

        val Type3Left = RadialGamePadConfig(
            theme = radialGamePadTheme,
            sockets = 12,
            primaryDial = PrimaryDialConfig.Cross(GLRetroView.MOTION_SOURCE_DPAD),
            secondaryDials = listOf(
                SecondaryDialConfig.SingleButton(2, 1, BUTTON_L2),
                SecondaryDialConfig.SingleButton(3, 1, BUTTON_L1),
                SecondaryDialConfig.SingleButton(4, 1, BUTTON_SELECT),
                SecondaryDialConfig.SingleButton(8, 1, BUTTON_SAVE_STATE),
                SecondaryDialConfig.Stick(9, 2f, GLRetroView.MOTION_SOURCE_ANALOG_LEFT, KeyEvent.KEYCODE_BUTTON_THUMBL)
            )
        )

        val Type3Right = RadialGamePadConfig(
            theme = radialGamePadTheme,
            sockets = 12,
            primaryDial = PrimaryDialConfig.PrimaryButtons(
                dials = listOf(
                    BUTTON_A,
                    BUTTON_X,
                    BUTTON_Y,
                    BUTTON_B
                )
            ),
            secondaryDials = listOf(
                SecondaryDialConfig.SingleButton(2, 1, BUTTON_START),
                SecondaryDialConfig.SingleButton(3, 1, BUTTON_R1),
                SecondaryDialConfig.SingleButton(4, 1, BUTTON_R2),
                SecondaryDialConfig.SingleButton(10, 1, BUTTON_LOAD_STATE),
                SecondaryDialConfig.Stick(8, 2f, GLRetroView.MOTION_SOURCE_ANALOG_RIGHT, KeyEvent.KEYCODE_BUTTON_THUMBR)
            )
        )
    }
}