package com.draco.libretrowrapper

import android.app.AlertDialog
import android.app.Service
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.Variable
import io.reactivex.disposables.CompositeDisposable
import java.io.File
import java.net.UnknownHostException
import java.util.concurrent.CountDownLatch

class GameActivity : AppCompatActivity() {
    /* Essential objects */
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var privateData: PrivateData
    private lateinit var coreUpdater: CoreUpdater

    /* UI components */
    private lateinit var retroViewContainer: FrameLayout
    private lateinit var leftGamePadContainer: FrameLayout
    private lateinit var rightGamePadContainer: FrameLayout
    private lateinit var progress: ProgressBar

    /* Emulator objects */
    private var retroView: GLRetroView? = null
    private var leftGamePad: GamePad? = null
    private var rightGamePad: GamePad? = null

    /* Latch that gets decremented when the GLRetroView renders a frame */
    private val retroViewReadyLatch = CountDownLatch(1)

    /* Input handler for GLRetroView */
    private val input = Input()

    /* Shared preference keys */
    private val fastForwardEnabledString = "fast_forward_enabled"
    private val audioEnabledString = "audio_enabled"

    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        /* Setup essential objects */
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        privateData = PrivateData(this)
        coreUpdater = CoreUpdater(this)

        /* Initialize UI components */
        retroViewContainer = findViewById(R.id.retroview_container)
        leftGamePadContainer = findViewById(R.id.left_container)
        rightGamePadContainer = findViewById(R.id.right_container)
        progress = findViewById(R.id.progress)

        /* Make sure we reapply immersive mode on rotate */
        window.decorView.setOnApplyWindowInsetsListener { _, windowInsets ->
            immersive()
            return@setOnApplyWindowInsetsListener windowInsets
        }

        /*
         * We have a progress spinner on the screen at this point until the GLRetroView
         * renders a frame. Let's setup our ROM, core, and GLRetroView in a background thread.
         */
        Thread {
            /* Setup ROM and core if we haven't already */
            try {
                initAssets()
            } catch (e: UnknownHostException) {
                runOnUiThread { showFetchError(e) }
                return@Thread
            }

            /* Instantiate our GLRetroView */
            try {
                initRetroView()
            } catch (e: Exception) {
                runOnUiThread { showLoadError(e) }
                return@Thread
            }
            
            /* Add the GLRetroView to main layout and hide the progress spinner */
            runOnUiThread {
                retroViewContainer.addView(retroView)
                progress.visibility = View.GONE
            }

            /*
             * The GLRetroView will take a while to load up the ROM and core, so before we
             * finish up, we should wait for the GLRetroView to become usable.
             */
            retroViewReadyLatch.await()

            /*
             * If we initialize the GamePads too early, the user could load a state before the
             * emulator is ready, causing a crash. We MUST wait for the GLRetroView to render
             * a frame first.
             */
            runOnUiThread {
                initGamePads()
            }

            /* Restore emulator settings from last launch */
            restoreSettings()
        }.start()
    }

    private fun showFetchError(e: Exception) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.fetch_error_dialog_title))
            .setMessage("${getString(R.string.fetch_error_dialog_message)}\n\n${e.message}")
            .setPositiveButton(getString(R.string.fetch_error_dialog_exit)) { _, _ -> finishAffinity() }
            .setCancelable(false)
            .show()
    }

    private fun showLoadError(e: Exception) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.load_error_dialog_title))
            .setMessage("${getString(R.string.load_error_dialog_message)}\n\n${e.cause}")
            .setPositiveButton(getString(R.string.load_error_dialog_exit)) { _, _ -> finishAffinity() }
            .setCancelable(false)
            .show()
    }

    private fun initAssets() {
        /* Copy over our ROM from the internal assets directory */
        val assetFile = File("${filesDir.absolutePath}/${privateData.rom.name}")
        if (!assetFile.exists()) try {
            val assetInputStream = assets.open(privateData.rom.name)
            val assetOutputStream = assetFile.outputStream()

            assetInputStream.copyTo(assetOutputStream)

            assetOutputStream.close()
            assetInputStream.close()
        } catch (_: Exception) {}

        /* Update the core from the internet if possible */
        if (!privateData.core.exists())
            coreUpdater.update()
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

    private fun initRetroView() {
        /* Prepare the SRAM bytes if the file exists */
        var saveBytes = byteArrayOf()
        if (privateData.save.exists()) {
            val saveInputStream = privateData.save.inputStream()
            saveBytes = saveInputStream.readBytes()
            saveInputStream.close()
        }

        /* Create the GLRetroView */
        retroView = GLRetroView(
            this,
            privateData.core.absolutePath,
            privateData.rom.absolutePath,
            saveRAMState = saveBytes,
            shader = GLRetroView.SHADER_SHARP,
            variables = getCoreVariables()
        )
        lifecycle.addObserver(retroView!!)

        /* Center the view in the parent container */
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        )
        params.gravity = Gravity.CENTER
        retroView!!.layoutParams = params

        /* Start tracking the frame state of the GLRetroView */
        val renderDisposable = retroView!!
            .getGLRetroEvents()
            .subscribe {
                if (it == GLRetroView.GLRetroEvents.FrameRendered)
                    retroViewReadyLatch.countDown()
            }
        compositeDisposable.add(renderDisposable)
    }

    private fun initGamePads() {
        /* Check if we should show or hide controls */
        val visibility = if (shouldShowGamePads())
            View.VISIBLE
        else
            View.GONE

        /* Apply the new visibility state to the containers */
        leftGamePadContainer.visibility = visibility
        rightGamePadContainer.visibility = visibility

        /* Initialize the GamePads */
        val gamePadConfig = GamePadConfig(this, resources)
        leftGamePad = GamePad(this, gamePadConfig.left, retroView, privateData)
        rightGamePad = GamePad(this, gamePadConfig.right, retroView, privateData)

        /* Subscribe the input handler observables */
        leftGamePad!!.subscribe()
        rightGamePad!!.subscribe()

        /* Configure GamePad size and position */
        val density = resources.displayMetrics.density
        val gamePadSize = resources.getDimension(R.dimen.config_gamepad_size) / density
        leftGamePad!!.pad.offsetX = -1f
        leftGamePad!!.pad.offsetY = 1f
        rightGamePad!!.pad.offsetX = 1f
        rightGamePad!!.pad.offsetY = 1f
        leftGamePad!!.pad.primaryDialMaxSizeDp = gamePadSize
        rightGamePad!!.pad.primaryDialMaxSizeDp = gamePadSize

        /* Add to layout */
        leftGamePadContainer.addView(leftGamePad!!.pad)
        rightGamePadContainer.addView(rightGamePad!!.pad)
    }

    private fun restoreSettings() {
        retroView!!.fastForwardEnabled = sharedPreferences.getBoolean(fastForwardEnabledString, false)
        retroView!!.audioEnabled = sharedPreferences.getBoolean(audioEnabledString, true)
    }

    private fun saveSettings() {
        with (sharedPreferences.edit()) {
            putBoolean(fastForwardEnabledString, retroView!!.fastForwardEnabled)
            putBoolean(audioEnabledString, retroView!!.audioEnabled)
            apply()
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        /* Reapply our immersive mode on focus gain */
        if (hasFocus)
            immersive()
    }

    private fun shouldShowGamePads(): Boolean {
        /* Do not show if we hardcoded the boolean */
        if (!resources.getBoolean(R.bool.config_gamepad_visible))
            return false

        /* Do not show if the device lacks a touch screen */
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN))
            return false

        /* Do not show if the current display is external (i.e. wireless cast) */
        val dm = getSystemService(Service.DISPLAY_SERVICE) as DisplayManager
        if (dm.getDisplay(getCurrentDisplayId()).flags and Display.FLAG_PRESENTATION == Display.FLAG_PRESENTATION)
            return false

        /* Do not show if the device has a controller connected */
        for (id in InputDevice.getDeviceIds()) {
            InputDevice.getDevice(id).apply {
                if (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                    sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        /* Save a temporary state since Android killed the activity */
        val savedInstanceStateBytes = retroView?.serializeState()
        if (savedInstanceStateBytes != null)
            with (privateData.savedInstanceState.outputStream()) {
                write(savedInstanceStateBytes)
                close()
            }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        /* Don't bother restoring a temporary state if it doesn't exist */
        if (!privateData.savedInstanceState.exists())
            return

        Thread {
            /* Wait for the GLRetroView to become usable */
            retroViewReadyLatch.await()

            /* Restore the temporary state */
            val stateInputStream = privateData.savedInstanceState.inputStream()
            val stateBytes = stateInputStream.readBytes()
            stateInputStream.close()
            retroView!!.unserializeState(stateBytes)
        }.start()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return input.handleKeyEvent(retroView, keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return input.handleKeyEvent(retroView, keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (input.handleGenericMotionEvent(retroView, event))
            return true

        return super.onGenericMotionEvent(event)
    }

    override fun onStop() {
        /* Save emulator settings for next launch */
        saveSettings()

        /* Save SRAM to the disk only if the emulator was able to render a frame */
        if (retroViewReadyLatch.count == 0L) {
            with(privateData.save.outputStream()) {
                write(retroView?.serializeSRAM())
                close()
            }
        }
        super.onStop()
    }

    override fun onDestroy() {
        leftGamePad?.unsubscribe()
        rightGamePad?.unsubscribe()
        compositeDisposable.dispose()
        super.onDestroy()
    }
}