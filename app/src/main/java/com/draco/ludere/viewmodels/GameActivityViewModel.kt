package com.draco.ludere.viewmodels

import android.app.Application
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.view.*
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.AndroidViewModel
import com.draco.ludere.R
import com.draco.ludere.gamepad.GamePad
import com.draco.ludere.gamepad.GamePadConfig
import com.draco.ludere.input.ControllerInput
import com.draco.ludere.retroview.RetroView
import com.draco.ludere.utils.RetroViewUtils
import io.reactivex.disposables.CompositeDisposable

class GameActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val resources = application.resources

    var retroView: RetroView? = null
    private var retroViewUtils: RetroViewUtils? = null

    private var leftGamePad: GamePad? = null
    private var rightGamePad: GamePad? = null

    private var menuDialog: AlertDialog? = null

    private var compositeDisposable = CompositeDisposable()
    private val controllerInput = ControllerInput()

    init {
        controllerInput.menuCallback = { showMenu() }
    }

    /**
     * Create an instance of a menu dialog
     */
    fun prepareMenu(context: Context) {
        if (menuDialog != null)
            return

        val menuOnClickListener = MenuOnClickListener()
        menuDialog = AlertDialog.Builder(context)
            .setItems(menuOnClickListener.menuOptions, menuOnClickListener)
            .create()
    }

    /**
     * Show the menu
     */
    fun showMenu() {
        if (retroView?.frameRendered?.value == true) {
            retroView?.let { retroViewUtils?.preserveEmulatorState(it) }
            menuDialog?.show()
        }
    }

    /**
     * Dismiss the menu
     */
    fun dismissMenu() {
        if (menuDialog?.isShowing == true)
            menuDialog?.dismiss()
    }

    /**
     * Save the state of the emulator
     */
    fun preserveState() {
        if (retroView?.frameRendered?.value == true)
            retroView?.let { retroViewUtils?.preserveEmulatorState(it) }
    }

    /**
     * Hide the system bars
     */
    fun immersive(window: Window) {
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
     * Hook the RetroView with the GLRetroView instance
     */
    fun setupRetroView(activity: ComponentActivity, container: FrameLayout) {
        retroView = RetroView(activity, compositeDisposable)
        if (retroViewUtils == null)
            retroViewUtils = RetroViewUtils(activity)

        retroView?.let { retroView ->
            container.addView(retroView.view)
            activity.lifecycle.addObserver(retroView.view)
            retroView.registerFrameRenderedListener()

            /* Restore state after first frame loaded */
            retroView.frameRendered.observe(activity) {
                if (it != true)
                    return@observe

                retroViewUtils?.restoreEmulatorState(retroView)
            }
        }
    }

    /**
     * Subscribe the GamePads to the RetroView
     */
    fun setupGamePads(leftContainer: FrameLayout, rightContainer: FrameLayout) {
        val context = getApplication<Application>().applicationContext

        val gamePadConfig = GamePadConfig(context, resources)
        leftGamePad = GamePad(context, gamePadConfig.left)
        rightGamePad = GamePad(context, gamePadConfig.right)

        leftGamePad?.let {
            leftContainer.addView(it.pad)
            retroView?.let { retroView -> it.subscribe(compositeDisposable, retroView.view) }
        }

        rightGamePad?.let {
            rightContainer.addView(it.pad)
            retroView?.let { retroView -> it.subscribe(compositeDisposable, retroView.view) }
        }
    }

    /**
     * Hide the on-screen GamePads
     */
    fun updateGamePadVisibility(leftContainer: FrameLayout, rightContainer: FrameLayout) {
        val context = getApplication<Application>().applicationContext

        val visibility = if (GamePad.shouldShowGamePads(context))
            View.VISIBLE
        else
            View.GONE

        leftContainer.visibility = visibility
        rightContainer.visibility = visibility
    }

    /**
     * Process a key event and return the result
     */
    fun processKeyEvent(keyCode: Int, event: KeyEvent): Boolean? {
        retroView?.let {
            return controllerInput.processKeyEvent(keyCode, event, it)
        }

        return false
    }

    /**
     * Process a motion event and return the result
     */
    fun processMotionEvent(event: MotionEvent): Boolean? {
        retroView?.let {
            return controllerInput.processMotionEvent(event, it)
        }

        return false
    }

    /**
     * Deallocate the old RetroView
     */
    fun detachRetroView() {
        retroView = null
    }

    /**
     * Dispose the composite disposable; call on onDestroy
     */
    fun dispose() {
        compositeDisposable.dispose()
        compositeDisposable = CompositeDisposable()
    }

    /**
     * Class to handle the menu dialog actions
     */
    inner class MenuOnClickListener : DialogInterface.OnClickListener {
        private val context = getApplication<Application>().applicationContext

        val menuOptions = arrayOf(
            context.getString(R.string.menu_reset),
            context.getString(R.string.menu_save_state),
            context.getString(R.string.menu_load_state),
            context.getString(R.string.menu_mute),
            context.getString(R.string.menu_fast_forward)
        )

        override fun onClick(dialog: DialogInterface?, which: Int) {
            when (menuOptions[which]) {
                context.getString(R.string.menu_reset) -> retroView?.view?.reset()
                context.getString(R.string.menu_save_state) -> retroView?.let {
                    retroViewUtils?.saveState(it)
                }
                context.getString(R.string.menu_load_state) -> retroView?.let{
                    retroViewUtils?.loadState(it)
                }
                context.getString(R.string.menu_mute) -> retroView?.let {
                    it.view.audioEnabled = !it.view.audioEnabled
                }
                context.getString(R.string.menu_fast_forward) -> retroView?.let {
                    it.view.frameSpeed = if (it.view.frameSpeed == 1) 2 else 1
                }
            }
        }
    }
}