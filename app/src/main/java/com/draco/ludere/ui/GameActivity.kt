package com.draco.ludere.ui

import android.app.Service
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.input.InputManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.draco.ludere.R
import com.draco.ludere.assets.PrivateData
import com.draco.ludere.assets.System
import com.draco.ludere.gamepad.GamePad
import com.draco.ludere.gamepad.GamePadConfig
import com.draco.ludere.utils.Input
import com.draco.ludere.utils.RetroViewUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import com.swordfish.libretrodroid.Variable
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.CountDownLatch

class GameActivity : AppCompatActivity() {
    /* UI components */
    private lateinit var progress: ProgressBar
    private lateinit var retroViewContainer: FrameLayout
    private lateinit var leftGamePadContainer: FrameLayout
    private lateinit var rightGamePadContainer: FrameLayout

    /* Essential objects */
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var privateData: PrivateData
    private lateinit var retroViewUtils: RetroViewUtils
    private lateinit var input: Input
    private lateinit var system: System

    /* Emulator objects */
    private var retroView: GLRetroView? = null
    private var leftGamePad: GamePad? = null
    private var rightGamePad: GamePad? = null

    /* Alert dialogs*/
    private lateinit var panicDialog: AlertDialog

    /* Latch that gets decremented when the GLRetroView renders a frame */
    private val retroViewReadyLatch = CountDownLatch(1)

    /* Store all observable subscriptions */
    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        /* Initialize UI components */
        progress = findViewById(R.id.progress)
        retroViewContainer = findViewById(R.id.retroview_container)
        leftGamePadContainer = findViewById(R.id.left_container)
        rightGamePadContainer = findViewById(R.id.right_container)

        /* Setup essential objects */
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        privateData = PrivateData(this)
        input = Input(this)
        system = System(this)

        /* Make sure we reapply immersive mode on rotate */
        window.decorView.setOnApplyWindowInsetsListener { view, windowInsets ->
            view.post { immersive() }
            return@setOnApplyWindowInsetsListener windowInsets
        }

        /* Prepare skeleton of dialogs */
        panicDialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.panic_title))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.button_exit)) { _, _ -> finishAffinity() }
            .create()

        /*
         * We have a progress spinner on the screen at this point until the GLRetroView
         * renders a frame. Let's setup our ROM, core, and GLRetroView in a background thread.
         */
        Thread {
            /* Extract all essential assets here */
            system.extractToFilesDir()

            /* Add the GLRetroView to the screen */
            runOnUiThread {
                setupRetroView()
                progress.visibility = View.GONE
            }

            /*
             * The GLRetroView will take a while to load up the ROM and core, so before we
             * finish up, we should wait for the GLRetroView to become usable.
             */
            retroViewReadyLatch.await()

            /* Restore emulator instance from last launch */
            restoreEmulatorState()

            /* Initialize the GamePads if they are enabled in the config */
            if (resources.getBoolean(R.bool.config_gamepad_visible)) {
                runOnUiThread {
                    setupGamePads()
                    leftGamePad!!.subscribe(retroView!!)
                    rightGamePad!!.subscribe(retroView!!)
                }
            }
        }.start()
    }

    private fun setupRetroView() {
        /* Setup configuration for the GLRetroView */
        val retroViewData = GLRetroViewData(this).apply {
            coreFilePath = "libcore.so"
            gameFileBytes = privateData.romBytes
            shader = GLRetroView.SHADER_SHARP
            variables = getCoreVariables()

            if (privateData.save.exists()) {
                privateData.save.inputStream().use {
                    saveRAMState = it.readBytes()
                }
            }
        }

        /* Create the GLRetroView */
        retroView = GLRetroView(this, retroViewData)

        /* Register RetroViewUtils class */
        retroViewUtils = RetroViewUtils(retroView!!)

        /* Hook the GLRetroView to the fragment lifecycle */
        lifecycle.addObserver(retroView!!)

        /* Center the view in the parent container */
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        )
        params.gravity = Gravity.CENTER
        retroView!!.layoutParams = params

        retroViewContainer.addView(retroView!!)

        /* Start tracking the frame state of the GLRetroView */
        val renderDisposable = retroView!!
            .getGLRetroEvents()
            .takeUntil { retroViewReadyLatch.count == 0L }
            .subscribe {
                if (it == GLRetroView.GLRetroEvents.FrameRendered)
                    retroViewReadyLatch.countDown()
            }
        compositeDisposable.add(renderDisposable)

        /* Also start tracking any errors we come across */
        val errorDisposable = retroView!!
            .getGLRetroErrors()
            .subscribe {
                val errorMessage = when (it) {
                    GLRetroView.ERROR_LOAD_LIBRARY -> R.string.panic_message_load_core
                    GLRetroView.ERROR_LOAD_GAME -> R.string.panic_message_load_game
                    GLRetroView.ERROR_GL_NOT_COMPATIBLE -> R.string.panic_message_gles
                    else -> null
                }

                /* Fatal error, panic accordingly */
                if (errorMessage != null)
                    runOnUiThread { panic(errorMessage) }
            }
        compositeDisposable.add(errorDisposable)
    }

    private fun setupGamePads() {
        /* Initialize the GamePads */
        val gamePadConfig = GamePadConfig(this, resources)
        leftGamePad = GamePad(this, gamePadConfig.left)
        rightGamePad = GamePad(this, gamePadConfig.right)

        /* Configure GamePad size and position */
        val density = resources.displayMetrics.density
        val gamePadSize = resources.getDimension(R.dimen.config_gamepad_size) / density
        leftGamePad!!.pad.primaryDialMaxSizeDp = gamePadSize
        rightGamePad!!.pad.primaryDialMaxSizeDp = gamePadSize

        /* Detect when controllers are added so we can disable or enable the GamePads */
        val inputManager = getSystemService(Service.INPUT_SERVICE) as InputManager
        inputManager.registerInputDeviceListener(object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) { updateVisibility() }
            override fun onInputDeviceRemoved(deviceId: Int) { updateVisibility() }
            override fun onInputDeviceChanged(deviceId: Int) { updateVisibility() }
        }, null)

        /* Perform this check right now */
        updateVisibility()

        /* Add to layout */
        leftGamePadContainer.addView(leftGamePad!!.pad)
        rightGamePadContainer.addView(rightGamePad!!.pad)
    }

    private fun panic(errorResId: Int) {
        /* Invalidate GLRetroView */
        retroView = null

        /* Show the error to the user */
        with (panicDialog) {
            setMessage(getString(errorResId))
            show()
        }
    }

    private fun getCoreVariables(): Array<Variable> {
        /* Parse the variables string into a Variable array */
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

        val targetDisk = sharedPreferences.getInt(getString(R.string.pref_current_disk), 0)
        if (retroView?.getCurrentDisk() != targetDisk)
            retroView?.changeDisk(targetDisk)

        requestedOrientation = sharedPreferences.getInt(getString(R.string.pref_rotation_lock), ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)

        retroViewUtils.loadStateFrom(privateData.tempState)
    }

    private fun preserveEmulatorState() {
        if (retroView != null) with (sharedPreferences.edit()) {
            putInt(getString(R.string.pref_frame_speed), retroView!!.frameSpeed)
            putBoolean(getString(R.string.pref_audio_enabled), retroView!!.audioEnabled)
            putInt(getString(R.string.pref_current_disk), retroView!!.getCurrentDisk())
            putInt(getString(R.string.pref_rotation_lock), requestedOrientation)
            apply()
        }
    }

    private fun updateVisibility() {
        /* Check if we should show or hide controls */
        val visibility = if (shouldShowGamePads())
            View.VISIBLE
        else
            View.GONE

        /* Apply the new visibility state to the containers */
        leftGamePadContainer.visibility = visibility
        rightGamePadContainer.visibility = visibility
    }

    private fun shouldShowGamePads(): Boolean {
        /* Do not show if the device lacks a touch screen */
        val hasTouchScreen = packageManager?.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
        if (hasTouchScreen == null || hasTouchScreen == false)
            return false

        /* Do not show if the current display is external (i.e. wireless cast) */
        val dm = getSystemService(Service.DISPLAY_SERVICE) as DisplayManager
        if (dm.getDisplay(getCurrentDisplayId()).flags and Display.FLAG_PRESENTATION == Display.FLAG_PRESENTATION)
            return false

        /* Do not show if the device has a controller connected */
        for (id in InputDevice.getDeviceIds()) {
            InputDevice.getDevice(id).apply {
                if (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD)
                    return false
            }
        }

        /* Otherwise, show */
        return true
    }

    private fun getCurrentDisplayId(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            display!!.displayId
        else {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            wm.defaultDisplay.displayId
        }
    }

    private fun immersive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            with (window.insetsController!!) {
                hide(
                    WindowInsets.Type.statusBars() or
                    WindowInsets.Type.navigationBars()
                )
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }

    override fun onBackPressed() {
        if (retroView != null)
            Menu(this, retroView!!).show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return input.handleKeyEvent(retroView, keyCode, event) || super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return input.handleKeyEvent(retroView, keyCode, event) || super.onKeyUp(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (input.handleGenericMotionEvent(retroView, event))
            return true

        return super.onGenericMotionEvent(event)
    }

    override fun onDestroy() {
        leftGamePad?.unsubscribe()
        rightGamePad?.unsubscribe()

        /* Dismiss dialogs to avoid leaking the window */
        if (panicDialog.isShowing) panicDialog.dismiss()

        compositeDisposable.dispose()
        super.onDestroy()
    }

    override fun onPause() {
        /* This method is unkillable, save essential variables now */
        if (retroViewReadyLatch.count == 0L) {
            /* Save emulator settings for next launch */
            preserveEmulatorState()

            /* Save a temporary state */
            retroViewUtils.saveStateTo(privateData.tempState)

            /* Save SRAM to disk */
            retroViewUtils.saveSRAMTo(privateData.save)
        }

        super.onPause()
    }
}
