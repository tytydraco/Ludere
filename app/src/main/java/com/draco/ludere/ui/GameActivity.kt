package com.draco.ludere.ui

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.input.InputManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.draco.ludere.R
import com.draco.ludere.gamepad.GamePad
import com.draco.ludere.gamepad.GamePadConfig
import com.draco.ludere.utils.Input
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import com.swordfish.libretrodroid.Variable
import io.reactivex.disposables.CompositeDisposable
import java.io.File
import java.lang.Exception
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch

class GameActivity: AppCompatActivity() {
    /* UI components */
    private lateinit var progress: ProgressBar
    private lateinit var retroViewContainer: FrameLayout
    private lateinit var leftGamePadContainer: FrameLayout
    private lateinit var rightGamePadContainer: FrameLayout

    /* Essential objects */
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var input: Input

    /* Internal data */
    private lateinit var romStoragePath: String
    private lateinit var romFile: File
    private lateinit var coreFile: File
    private lateinit var save: File
    private lateinit var tempState: File
    private fun stateForSlot(slot: Int) = File("$romStoragePath/state-$slot")

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
        input = Input(this)

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
            .setNegativeButton(getString(R.string.button_settings)) { _, _ ->
                val settingsIntent = Intent(this, SettingsActivity::class.java)
                startActivityForResult(settingsIntent, SettingsActivity.ACTIVITY_REQUEST_CODE)
            }
            .create()

        /* Initialize internal data */
        val romUriString = sharedPreferences.getString(SettingsActivity.PREFERENCE_KEY_ROM_URI, "")!!
        val coreUriString = sharedPreferences.getString(SettingsActivity.PREFERENCE_KEY_CORE_URI, "")!!

        /* Specify a safe storage path for our internal and external data */
        val filesPath = (getExternalFilesDir(null) ?: filesDir).path

        /* Update our internal copy of the ROM */
        romFile = File("${filesDir.path}/rom")
        var romBytes = byteArrayOf()
        try {
            val romUri = Uri.parse(romUriString)
            val romInputStream = contentResolver.openInputStream(romUri)
            romBytes = romInputStream?.readBytes()!!
            romFile.outputStream().use {
                it.write(romBytes)
            }
            romInputStream.close()
        } catch (_: Exception) {
            /* Only panic if we don't have an internal copy of the ROM available */
            if (!romFile.exists()) {
                panic(R.string.panic_message_no_selected_rom)
                return
            }
        }

        /* Update our internal copy of the core */
        coreFile = File("${filesDir.path}/core")
        try {
            val coreUri = Uri.parse(coreUriString)
            val coreInputStream = contentResolver.openInputStream(coreUri)
            val coreBytes = coreInputStream?.readBytes()!!
            coreFile.outputStream().use {
                it.write(coreBytes)
            }
            coreInputStream.close()
        } catch (_: Exception) {
            /* Only panic if we don't have an internal copy of the core available */
            if (!coreFile.exists()) {
                panic(R.string.panic_message_no_selected_core)
                return
            }
        }

        /* Generate an MD5 of the ROM */
        val md5Hex = MessageDigest
            .getInstance("MD5")
            .digest(romBytes)
            .joinToString("") { "%02x".format(it) }

        /* Store ROM data in its own special folder */
        romStoragePath = "$filesPath/$md5Hex"

        /* Initialize the directory if it does not yet exist */
        File(romStoragePath).mkdirs()

        /* Initialize ROM file handles via ROM storage */
        save = File("$romStoragePath/sram")
        tempState = File("$romStoragePath/tempstate")

        /*
         * We have a progress spinner on the screen at this point until the GLRetroView
         * renders a frame. Let's setup our ROM, core, and GLRetroView in a background thread.
         */
        Thread {
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

            /* Restore emulator settings from last launch */
            val targetDisk = sharedPreferences.getInt(getString(R.string.settings_disk_key), 0)
            if (retroView?.getCurrentDisk() != targetDisk)
                retroView?.changeDisk(targetDisk)
            retroView?.frameSpeed = sharedPreferences.getInt(getString(R.string.settings_speed_key), 0) + 1
            retroView?.audioEnabled = !sharedPreferences.getBoolean(getString(R.string.settings_mute_key), false)

            /* Pick up where the user left off */
            if (tempState.exists()) {
                /* Fetch the state bytes */
                val stateBytes = tempState.inputStream().use {
                    it.readBytes()
                }

                /* Restore the temporary state */
                var remainingTries = 10
                while (!retroView!!.unserializeState(stateBytes) && remainingTries-- > 0)
                    Thread.sleep(50)
            }

            /* Setup the onscreen controls */
            runOnUiThread {
                setupGamePads()
                leftGamePad!!.subscribe(retroView!!)
                rightGamePad!!.subscribe(retroView!!)
            }
        }.start()
    }

    private fun setupRetroView() {
        /* Setup configuration for the GLRetroView */
        val retroViewData = GLRetroViewData(this).apply {
            coreFilePath = coreFile.path
            gameFilePath = romFile.path
            shader = GLRetroView.SHADER_SHARP
            variables = getCoreVariables()

            if (save.exists()) {
                save.inputStream().use {
                    saveRAMState = it.readBytes()
                }
            }
        }

        /* Create the GLRetroView */
        retroView = GLRetroView(this, retroViewData)

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
        val gamePadConfig = GamePadConfig(this)
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
        val rawVariablesString = sharedPreferences.getString(getString(R.string.settings_variables_key), "")!!
        val rawVariables = rawVariablesString.split(",")

        for (rawVariable in rawVariables) {
            val rawVariableSplit = rawVariable.split("=")
            if (rawVariableSplit.size != 2)
                continue

            variables.add(Variable(rawVariableSplit[0], rawVariableSplit[1]))
        }

        return variables.toTypedArray()
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
        if (!sharedPreferences.getBoolean(getString(R.string.settings_gamepad_visible_key), true))
            return false

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
        val settingsIntent = Intent(this, SettingsActivity::class.java)
        settingsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivityForResult(settingsIntent, SettingsActivity.ACTIVITY_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        /* Handle actions from settings page */
        val stateSlot = sharedPreferences.getInt(getString(R.string.settings_save_state_slot_key), 0)
        if (requestCode == SettingsActivity.ACTIVITY_REQUEST_CODE && retroViewReadyLatch.count == 0L) {
            when (resultCode) {
                SettingsActivity.RESULT_CODE_SAVE_STATE -> if (retroView != null) {
                    stateForSlot(stateSlot).outputStream().use {
                        it.write(retroView!!.serializeState())
                    }
                }
                SettingsActivity.RESULT_CODE_LOAD_STATE -> if (retroView != null) {
                    if (!stateForSlot(stateSlot).exists())
                        return

                    val bytes = stateForSlot(stateSlot).inputStream().use {
                        it.readBytes()
                    }
                    if (bytes.isNotEmpty())
                        retroView!!.unserializeState(bytes)
                }
                SettingsActivity.RESULT_CODE_RESET -> retroView?.reset()
            }
        }
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
            /* Save a temporary state */
            val savedInstanceStateBytes = retroView!!.serializeState()
            tempState.outputStream().use {
                it.write(savedInstanceStateBytes)
            }

            /* Save SRAM to disk */
            save.outputStream().use {
                it.write(retroView!!.serializeSRAM())
            }
        }

        super.onPause()
    }
}
