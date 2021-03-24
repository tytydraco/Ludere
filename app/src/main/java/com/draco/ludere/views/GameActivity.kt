package com.draco.ludere.views

import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.draco.ludere.R
import com.draco.ludere.utils.GamePad
import com.draco.ludere.utils.GamePadConfig
import com.draco.ludere.utils.RetroView
import com.draco.ludere.utils.RetroViewUtils
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.*

class GameActivity : AppCompatActivity() {
    private val compositeDisposable = CompositeDisposable()

    private lateinit var retroViewContainer: FrameLayout
    private lateinit var leftGamePadContainer: FrameLayout
    private lateinit var rightGamePadContainer: FrameLayout

    private lateinit var retroView: RetroView
    private lateinit var retroViewUtils: RetroViewUtils
    private lateinit var leftGamePad: GamePad
    private lateinit var rightGamePad: GamePad

    private lateinit var menuDialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        retroViewContainer = findViewById(R.id.retroview_container)
        leftGamePadContainer = findViewById(R.id.left_container)
        rightGamePadContainer = findViewById(R.id.right_container)

        window.decorView.setOnApplyWindowInsetsListener { view, windowInsets ->
            if (resources.getBoolean(R.bool.config_fullscreen))
                view.post { immersive() }
            return@setOnApplyWindowInsetsListener windowInsets
        }

        val menuOnClickListener = MenuOnClickListener()
        menuDialog = AlertDialog.Builder(this)
            .setItems(menuOnClickListener.menuOptions, menuOnClickListener)
            .create()

        setupRetroView()
        setupGamePads()
    }

    private fun setupRetroView() {
        retroView = RetroView(this, compositeDisposable)
        retroViewUtils = RetroViewUtils(this, retroView)

        retroViewContainer.addView(retroView.view)
        lifecycle.addObserver(retroView.view)

        retroView.frameRendered.observe(this) {
            if (it != true)
                return@observe

            retroViewUtils.restoreEmulatorState()
        }
    }

    private fun setupGamePads() {
        val gamePadConfig = GamePadConfig(this, resources)
        leftGamePad = GamePad(this, gamePadConfig.left)
        rightGamePad = GamePad(this, gamePadConfig.right)

        leftGamePadContainer.addView(leftGamePad.pad)
        rightGamePadContainer.addView(rightGamePad.pad)

        leftGamePad.subscribe(compositeDisposable, retroView.view)
        rightGamePad.subscribe(compositeDisposable, retroView.view)
    }

    private fun immersive() {
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

    override fun onBackPressed() {
        if (retroView.frameRendered.value == true) {
            retroViewUtils.preserveEmulatorState()
            menuDialog.show()
        }
    }

    override fun onDestroy() {
        if (menuDialog.isShowing)
            menuDialog.dismiss()

        compositeDisposable.dispose()
        super.onDestroy()
    }

    override fun onPause() {
        if (retroView.frameRendered.value == true)
            retroViewUtils.preserveEmulatorState()

        super.onPause()
    }

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
                getString(R.string.menu_reset) -> retroView.view.reset()
                getString(R.string.menu_save_state) -> retroViewUtils.saveState()
                getString(R.string.menu_load_state) -> retroViewUtils.loadState()
                getString(R.string.menu_mute) -> retroView.view.audioEnabled = !retroView.view.audioEnabled
                getString(R.string.menu_fast_forward) -> retroView.view.frameSpeed = if (retroView.view.frameSpeed == 1) 2 else 1
            }
        }
    }
}
