package com.draco.libretrowrapper

import android.app.UiModeManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.swordfish.libretrodroid.GLRetroView
import java.io.File

class GameActivity : AppCompatActivity() {
    private lateinit var parent: FrameLayout
    private lateinit var retroView: GLRetroView
    private lateinit var rom: File

    private lateinit var leftGamePadContainer: FrameLayout
    private lateinit var rightGamePadContainer: FrameLayout
    private lateinit var leftGamePad: GamePad
    private lateinit var rightGamePad: GamePad

    private val validKeyCodes = arrayListOf(
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
        KeyEvent.KEYCODE_BUTTON_START,
        KeyEvent.KEYCODE_BUTTON_SELECT
    )

    private fun initRom() {
        /* If missing, copy to data directory */
        val rawInputStream = resources.openRawResource(R.raw.rom)
        val newRomBytes = rawInputStream.readBytes()

        /* Update the rom if it's either our first run, or if the contents have changed */
        if (!rom.exists() || (rom.exists() && !rom.readBytes().contentEquals(newRomBytes)))
            rom.writeBytes(newRomBytes)
    }

    private fun isControllerConnected(): Boolean {
        /* Consider all TVs to be controllers */
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
        if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION)
            return true

        for (id in InputDevice.getDeviceIds()) {
            InputDevice.getDevice(id).apply {
                if (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                    sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK)
                    return true
            }
        }

        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        /* Initialize layout variables */
        parent = findViewById(R.id.parent)
        leftGamePadContainer = findViewById(R.id.left_container)
        rightGamePadContainer = findViewById(R.id.right_container)

        /* Setup rom path */
        rom = File("${filesDir.absolutePath}/rom")
        initRom()

        /* Create GLRetroView */
        retroView = GLRetroView(
            this,
            "${getString(R.string.rom_core)}_libretro_android.so",
            rom.absolutePath
        )
        lifecycle.addObserver(retroView)
        parent.addView(retroView)

        /* Initialize GamePads */
        leftGamePad = GamePad(this, GamePadConfig.LeftGamePad, retroView)
        rightGamePad = GamePad(this, GamePadConfig.RightGamePad, retroView)
        leftGamePadContainer.addView(leftGamePad.pad)
        rightGamePadContainer.addView(rightGamePad.pad)

        leftGamePad.pad.offsetX = -1f
        leftGamePad.pad.primaryDialMaxSizeDp = 200f
        rightGamePad.pad.offsetX = 1f
        rightGamePad.pad.primaryDialMaxSizeDp = 200f

        /* Center view */
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        )
        params.gravity = Gravity.CENTER
        retroView.layoutParams = params

        /* Decide to mute the audio */
        Thread {
            /* Wait for ROM to load */
            while(retroView.getVariables().isEmpty())
                Thread.sleep(50)
            retroView.audioEnabled = resources.getBoolean(R.bool.rom_audio)
        }.start()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus)
            return

        /* Apply immersive mode */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            with (window.insetsController!!) {
                hide(
                    WindowInsets.Type.statusBars() or
                    WindowInsets.Type.navigationBars()
                )
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }

    private fun handleKeyEvent(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode !in validKeyCodes)
            return false

        retroView.sendKeyEvent(event.action, keyCode)
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return handleKeyEvent(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return handleKeyEvent(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        with (retroView) {
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
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onResume() {
        super.onResume()
        leftGamePad.resume()
        rightGamePad.resume()

        val visibility = if (isControllerConnected())
            View.GONE
        else
            View.VISIBLE
        
        leftGamePadContainer.visibility = visibility
        rightGamePadContainer.visibility = visibility
    }

    override fun onPause() {
        leftGamePad.pause()
        rightGamePad.pause()
        super.onPause()
    }
}