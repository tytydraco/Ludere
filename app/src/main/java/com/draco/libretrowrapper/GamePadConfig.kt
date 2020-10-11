package com.draco.libretrowrapper

import android.graphics.Color
import android.view.KeyEvent
import com.swordfish.radialgamepad.library.config.*

class GamePadConfig {
    companion object {
        const val KEYCODE_LOAD_STATE = -1
        const val KEYCODE_SAVE_STATE = -2
        private val radialGamePadTheme = RadialGamePadTheme(primaryDialBackground = Color.TRANSPARENT)
        val LeftGamePad = RadialGamePadConfig(
            theme = radialGamePadTheme,
            sockets = 12,
            primaryDial = PrimaryDialConfig.Cross(0),
            secondaryDials = listOf(
                SecondaryDialConfig.SingleButton(
                    4, 1, ButtonConfig(
                        id = KeyEvent.KEYCODE_BUTTON_L1,
                        label = "L"
                    )
                ),
                SecondaryDialConfig.SingleButton(
                    3, 1, ButtonConfig(
                        id = KEYCODE_SAVE_STATE,
                        iconId = R.drawable.ic_baseline_save_24
                    )
                ),
                SecondaryDialConfig.SingleButton(
                    8, 1, ButtonConfig(
                        id = KeyEvent.KEYCODE_BUTTON_SELECT,
                        label = "-"
                    )
                )
            )
        )

        val RightGamePad = RadialGamePadConfig(
            theme = radialGamePadTheme,
            sockets = 12,
            primaryDial = PrimaryDialConfig.PrimaryButtons(
                dials = listOf(
                    ButtonConfig(
                        id = KeyEvent.KEYCODE_BUTTON_A,
                        label = "A"
                    ),
                    ButtonConfig(
                        id = KeyEvent.KEYCODE_BUTTON_X,
                        label = "X"
                    ),
                    ButtonConfig(
                        id = KeyEvent.KEYCODE_BUTTON_Y,
                        label = "Y"
                    ),
                    ButtonConfig(
                        id = KeyEvent.KEYCODE_BUTTON_B,
                        label = "B"
                    ),
                ),
                rotationInDegrees = 0f
            ),
            secondaryDials = listOf(
                SecondaryDialConfig.SingleButton(
                    2, 1, ButtonConfig(
                        id = KeyEvent.KEYCODE_BUTTON_R1,
                        label = "R"
                    )
                ),
                SecondaryDialConfig.SingleButton(
                    3, 1, ButtonConfig(
                        id = KEYCODE_LOAD_STATE,
                        iconId = R.drawable.ic_baseline_get_app_24
                    )
                ),
                SecondaryDialConfig.SingleButton(
                    10, 1, ButtonConfig(
                        id = KeyEvent.KEYCODE_BUTTON_START,
                        label = "+"
                    )
                ),
            )
        )
    }
}