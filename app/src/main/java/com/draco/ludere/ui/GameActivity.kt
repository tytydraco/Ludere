package com.draco.ludere.ui

import android.app.Service
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.input.InputManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.draco.ludere.R
import com.draco.ludere.gamepad.GamePad
import com.draco.ludere.gamepad.GamePadConfig
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import com.swordfish.libretrodroid.Variable
import io.reactivex.disposables.CompositeDisposable
import java.io.File
import java.util.concurrent.CountDownLatch

class GameActivity : AppCompatActivity() {
    private lateinit var retroViewContainer: FrameLayout
    private lateinit var leftGamePadContainer: FrameLayout
    private lateinit var rightGamePadContainer: FrameLayout

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var storage: Storage
    private lateinit var menuOnClickListener: MenuOnClickListener

    private var retroView: GLRetroView? = null
    private var leftGamePad: GamePad? = null
    private var rightGamePad: GamePad? = null

    private lateinit var panicDialog: AlertDialog
    private val pressedKeys = mutableSetOf<Int>()
    private val retroViewReadyLatch = CountDownLatch(1)
    private val compositeDisposable = CompositeDisposable()

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

    private val keyComboMenu = setOf(
        KeyEvent.KEYCODE_BUTTON_START,
        KeyEvent.KEYCODE_BUTTON_SELECT,
        KeyEvent.KEYCODE_BUTTON_L1,
        KeyEvent.KEYCODE_BUTTON_R1
    )

    private inner class Storage {
        private val storagePath: String = (getExternalFilesDir(null) ?: filesDir).path
        val romBytes = resources.openRawResource(R.raw.rom).use { it.readBytes() }
        val sram = File("$storagePath/sram")
        val state = File("$storagePath/state")
        val tempState = File("$storagePath/tempstate")
    }

    private inner class MenuOnClickListener : DialogInterface.OnClickListener {
        val menuOptions = arrayOf(
            getString(R.string.menu_reset),
            getString(R.string.menu_save_state),
            getString(R.string.menu_load_state),
            getString(R.string.menu_mute),
            getString(R.string.menu_fast_forward)
        )

        override fun onClick(dialog: DialogInterface?, which: Int) {
            if (retroView != null) when (menuOptions[which]) {
                getString(R.string.menu_reset) -> retroView!!.reset()
                getString(R.string.menu_save_state) -> saveStateTo(storage.state)
                getString(R.string.menu_load_state) -> loadStateFrom(storage.state)
                getString(R.string.menu_mute) -> retroView!!.audioEnabled = !retroView!!.audioEnabled
                getString(R.string.menu_fast_forward) -> retroView!!.frameSpeed = if (retroView!!.frameSpeed == 1) 2 else 1
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        retroViewContainer = findViewById(R.id.retroview_container)
        leftGamePadContainer = findViewById(R.id.left_container)
        rightGamePadContainer = findViewById(R.id.right_container)
        sharedPreferences = getPreferences(MODE_PRIVATE)
        storage = Storage()
        menuOnClickListener = MenuOnClickListener()

        window.decorView.setOnApplyWindowInsetsListener { view, windowInsets ->
            view.post { immersive() }
            return@setOnApplyWindowInsetsListener windowInsets
        }

        panicDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.panic_title))
            .setMessage(getString(R.string.panic_message))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.button_exit)) { _, _ -> finishAffinity() }
            .create()

        Thread {
            runOnUiThread {
                setupRetroView()
            }

            retroViewReadyLatch.await()
            restoreEmulatorState()

            if (resources.getBoolean(R.bool.config_gamepad_visible)) {
                runOnUiThread {
                    setupGamePads()
                    leftGamePad!!.subscribe(compositeDisposable, retroView!!)
                    rightGamePad!!.subscribe(compositeDisposable, retroView!!)
                }
            }
        }.start()
    }

    private fun setupRetroView() {
        val retroViewData = GLRetroViewData(this).apply {
            coreFilePath = "libcore.so"
            gameFileBytes = storage.romBytes
            shader = GLRetroView.SHADER_SHARP
            variables = getCoreVariables()

            if (storage.sram.exists()) {
                storage.sram.inputStream().use {
                    saveRAMState = it.readBytes()
                }
            }
        }

        retroView = GLRetroView(this, retroViewData)
        lifecycle.addObserver(retroView!!)

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        )
        params.gravity = Gravity.CENTER
        retroView!!.layoutParams = params
        retroViewContainer.addView(retroView!!)

        val renderDisposable = retroView!!
            .getGLRetroEvents()
            .takeUntil { retroViewReadyLatch.count == 0L }
            .subscribe {
                if (it == GLRetroView.GLRetroEvents.FrameRendered)
                    retroViewReadyLatch.countDown()
            }
        compositeDisposable.add(renderDisposable)

        val errorDisposable = retroView!!
            .getGLRetroErrors()
            .subscribe {
                runOnUiThread {
                    retroView = null
                    panicDialog.show()
                }
            }
        compositeDisposable.add(errorDisposable)
    }

    private fun setupGamePads() {
        val gamePadConfig = GamePadConfig(this, resources)
        leftGamePad = GamePad(this, gamePadConfig.left)
        rightGamePad = GamePad(this, gamePadConfig.right)

        val inputManager = getSystemService(Service.INPUT_SERVICE) as InputManager
        inputManager.registerInputDeviceListener(object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) { updateVisibility() }
            override fun onInputDeviceRemoved(deviceId: Int) { updateVisibility() }
            override fun onInputDeviceChanged(deviceId: Int) { updateVisibility() }
        }, null)
        updateVisibility()

        leftGamePadContainer.addView(leftGamePad!!.pad)
        rightGamePadContainer.addView(rightGamePad!!.pad)
    }

    private fun getCoreVariables(): Array<Variable> {
        val variables = arrayListOf<Variable>()
        val rawVariablesString = getString(R.string.config_variables)
        val rawVariables = rawVariablesString.split(",")

        for (rawVariable in rawVariables) {
            val rawVariableSplit = rawVariable.split("=")
            if (rawVariableSplit.size != 2)
                continue

            variables.add(Variable(rawVariableSplit[0], rawVariableSplit[1]))
        }

        return variables.toTypedArray()
    }

    private fun restoreEmulatorState() {
        retroView?.frameSpeed = sharedPreferences.getInt(getString(R.string.pref_frame_speed), 1)
        retroView?.audioEnabled = sharedPreferences.getBoolean(getString(R.string.pref_audio_enabled), true)
        loadStateFrom(storage.tempState)
    }

    private fun preserveEmulatorState() {
        saveSRAMTo(storage.sram)
        saveStateTo(storage.tempState)
        with (sharedPreferences.edit()) {
            putInt(getString(R.string.pref_frame_speed), retroView!!.frameSpeed)
            putBoolean(getString(R.string.pref_audio_enabled), retroView!!.audioEnabled)
            apply()
        }
    }

    private fun updateVisibility() {
        val visibility = if (shouldShowGamePads())
            View.VISIBLE
        else
            View.GONE

        leftGamePadContainer.visibility = visibility
        rightGamePadContainer.visibility = visibility
    }

    private fun shouldShowGamePads(): Boolean {
        val hasTouchScreen = packageManager?.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
        if (hasTouchScreen == null || hasTouchScreen == false)
            return false

        val currentDisplayId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            display!!.displayId
        else {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            wm.defaultDisplay.displayId
        }

        val dm = getSystemService(Service.DISPLAY_SERVICE) as DisplayManager
        if (dm.getDisplay(currentDisplayId).flags and Display.FLAG_PRESENTATION == Display.FLAG_PRESENTATION)
            return false

        for (id in InputDevice.getDeviceIds()) {
            InputDevice.getDevice(id).apply {
                if (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD)
                    return false
            }
        }

        return true
    }

    private fun immersive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            with (window.insetsController!!) {
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }

    private fun showMenu() {
        AlertDialog.Builder(this)
            .setItems(menuOnClickListener.menuOptions, menuOnClickListener)
            .show()
    }

    private fun saveSRAMTo(file: File) {
        file.outputStream().use {
            it.write(retroView!!.serializeSRAM())
        }
    }

    private fun loadStateFrom(file: File) {
        if (!file.exists())
            return

        val stateBytes = file.inputStream().use {
            it.readBytes()
        }

        if (stateBytes.isEmpty())
            return

        retroView!!.unserializeState(stateBytes)
    }

    private fun saveStateTo(file: File) {
        file.outputStream().use {
            it.write(retroView!!.serializeState())
        }
    }

    override fun onBackPressed() {
        preserveEmulatorState()
        showMenu()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (retroView == null || keyCode !in validKeyCodes)
            return super.onKeyDown(keyCode, event)

        /* Controller numbers are [1, inf), we need [0, inf) */
        val port = ((event.device?.controllerNumber ?: 1) - 1).coerceAtLeast(0)

        pressedKeys.add(keyCode)
        retroView!!.sendKeyEvent(
            event.action,
            keyCode,
            port
        )

        if (pressedKeys == keyComboMenu)
            showMenu()

        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (retroView == null || keyCode !in validKeyCodes)
            return super.onKeyUp(keyCode, event)

        /* Controller numbers are [1, inf), we need [0, inf) */
        val port = ((event.device?.controllerNumber ?: 1) - 1).coerceAtLeast(0)

        pressedKeys.remove(keyCode)
        retroView!!.sendKeyEvent(
            event.action,
            keyCode,
            port
        )

        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (retroView == null)
            return super.onGenericMotionEvent(event)

        /* Controller numbers are [1, inf), we need [0, inf) */
        val port = ((event.device?.controllerNumber ?: 1) - 1).coerceAtLeast(0)

        retroView!!.apply {
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

        return true
    }

    override fun onDestroy() {
        if (panicDialog.isShowing)
            panicDialog.dismiss()
        compositeDisposable.dispose()

        super.onDestroy()
    }

    override fun onPause() {
        if (retroViewReadyLatch.count == 0L)
            preserveEmulatorState()

        super.onPause()
    }
}
