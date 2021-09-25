package com.draco.ludere.views

import android.app.Service
import android.content.DialogInterface
import android.hardware.input.InputManager
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.draco.ludere.R
import com.draco.ludere.databinding.ActivityGameBinding
import com.draco.ludere.gamepad.GamePad
import com.draco.ludere.gamepad.GamePadConfig
import com.draco.ludere.input.ControllerInput
import com.draco.ludere.retroview.RetroView
import com.draco.ludere.utils.RetroViewUtils
import io.reactivex.disposables.CompositeDisposable

class GameActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameBinding

    var retroView: RetroView? = null
    private var retroViewUtils: RetroViewUtils? = null

    private var leftGamePad: GamePad? = null
    private var rightGamePad: GamePad? = null

    private var menuDialog: AlertDialog? = null

    private var compositeDisposable = CompositeDisposable()
    private val controllerInput = ControllerInput().apply {
        menuCallback = { showMenu() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /* Use immersive mode when we change the window insets */
        window.decorView.setOnApplyWindowInsetsListener { view, windowInsets ->
            view.post { immersive() }
            return@setOnApplyWindowInsetsListener windowInsets
        }

        registerInputListener()
        updateGamePadVisibility()
        prepareMenu()
        setupRetroView()
        setupGamePads()
    }

    /**
     * Listen for new controller additions and removals
     */
    private fun registerInputListener() {
        val inputManager = getSystemService(Service.INPUT_SERVICE) as InputManager
        inputManager.registerInputDeviceListener(object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) {
                updateGamePadVisibility()
            }
            override fun onInputDeviceRemoved(deviceId: Int) {
                updateGamePadVisibility()
            }
            override fun onInputDeviceChanged(deviceId: Int) {
                updateGamePadVisibility()
            }
        }, null)
    }

    override fun onBackPressed() = showMenu()

    private fun showMenu() {
        if (retroView?.frameRendered?.value == true) {
            retroView?.let { retroViewUtils?.preserveEmulatorState(it) }
            menuDialog?.show()
        }
    }

    override fun onDestroy() {
        if (menuDialog?.isShowing == true)
            menuDialog?.dismiss()
        retroView?.view?.let { lifecycle.removeObserver(it) }
        retroView = null
        compositeDisposable.dispose()
        super.onDestroy()
    }

    override fun onPause() {
        preserveState()
        super.onPause()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return processKeyEvent(keyCode, event) ?: super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return processKeyEvent(keyCode, event) ?: super.onKeyUp(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        return processMotionEvent(event) ?: super.onGenericMotionEvent(event)
    }

    /**
     * Process a key event and return the result
     */
    private fun processKeyEvent(keyCode: Int, event: KeyEvent): Boolean? {
        retroView?.let {
            return controllerInput.processKeyEvent(keyCode, event, it)
        }

        return false
    }

    /**
     * Process a motion event and return the result
     */
    private fun processMotionEvent(event: MotionEvent): Boolean? {
        retroView?.let {
            return controllerInput.processMotionEvent(event, it)
        }

        return false
    }

    /**
     * Hide the system bars
     */
    private fun immersive() {
        /* Check if the config permits it */
        if (!resources.getBoolean(R.bool.config_fullscreen))
            return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            with (window.insetsController!!) {
                hide(WindowInsets.Type.systemBars())
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

    /**
     * Class to handle the menu dialog actions
     */
    inner class MenuOnClickListener : DialogInterface.OnClickListener {
        val menuOptions = arrayOf(
            getString(R.string.menu_reset),
            getString(R.string.menu_save_state),
            getString(R.string.menu_load_state),
            getString(R.string.menu_mute),
            getString(R.string.menu_fast_forward)
        )

        override fun onClick(dialog: DialogInterface?, which: Int) {
            when (menuOptions[which]) {
                getString(R.string.menu_reset) -> retroView?.view?.reset()
                getString(R.string.menu_save_state) -> retroView?.let {
                    retroViewUtils?.saveState(it)
                }
                getString(R.string.menu_load_state) -> retroView?.let{
                    retroViewUtils?.loadState(it)
                }
                getString(R.string.menu_mute) -> retroView?.let {
                    it.view.audioEnabled = !it.view.audioEnabled
                }
                getString(R.string.menu_fast_forward) -> retroView?.let {
                    retroViewUtils?.fastForward(it)
                }
            }
        }
    }

    /**
     * Hide the on-screen GamePads
     */
    private fun updateGamePadVisibility() {
        val visibility = if (GamePad.shouldShowGamePads(this))
            View.VISIBLE
        else
            View.GONE

        binding.leftContainer.visibility = visibility
        binding.rightContainer.visibility = visibility
    }

    /**
     * Create an instance of a menu dialog
     */
    private fun prepareMenu() {
        val menuOnClickListener = MenuOnClickListener()
        menuDialog = AlertDialog.Builder(this)
            .setItems(menuOnClickListener.menuOptions, menuOnClickListener)
            .create()
    }

    /**
     * Hook the RetroView with the GLRetroView instance
     */
    private fun setupRetroView() {
        retroView = RetroView(this, compositeDisposable)
        retroViewUtils = RetroViewUtils(this)

        retroView?.let { retroView ->
            binding.retroviewContainer.addView(retroView.view)
            lifecycle.addObserver(retroView.view)
            retroView.registerFrameRenderedListener()

            /* Restore state after first frame loaded */
            retroView.frameRendered.observe(this) {
                if (it != true)
                    return@observe

                retroViewUtils?.restoreEmulatorState(retroView)
            }
        }
    }

    /**
     * Subscribe the GamePads to the RetroView
     */
    private fun setupGamePads() {
        val gamePadConfig = GamePadConfig(this, resources)
        leftGamePad = GamePad(this, gamePadConfig.left)
        rightGamePad = GamePad(this, gamePadConfig.right)

        leftGamePad?.let {
            binding.leftContainer.addView(it.pad)
            retroView?.let { retroView -> it.subscribe(compositeDisposable, retroView.view) }
        }

        rightGamePad?.let {
            binding.rightContainer.addView(it.pad)
            retroView?.let { retroView -> it.subscribe(compositeDisposable, retroView.view) }
        }
    }

    /**
     * Save the state of the emulator
     */
    private fun preserveState() {
        if (retroView?.frameRendered?.value == true)
            retroView?.let { retroViewUtils?.preserveEmulatorState(it) }
    }
}
