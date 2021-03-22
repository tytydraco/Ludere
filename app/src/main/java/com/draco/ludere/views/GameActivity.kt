package com.draco.ludere.views

import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.draco.ludere.R
import com.draco.ludere.utils.GamePad
import com.draco.ludere.utils.RetroView
import com.draco.ludere.utils.RetroViewUtils
import com.draco.ludere.viewmodels.GameActivityViewModel
import kotlinx.coroutines.*

class GameActivity : AppCompatActivity() {
    private val viewModel: GameActivityViewModel by viewModels()

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

        leftGamePad = GamePad(this, viewModel.gamePadConfig.left)
        rightGamePad = GamePad(this, viewModel.gamePadConfig.right)

        retroViewContainer = findViewById(R.id.retroview_container)
        leftGamePadContainer = findViewById(R.id.left_container)
        rightGamePadContainer = findViewById(R.id.right_container)

        window.decorView.setOnApplyWindowInsetsListener { view, windowInsets ->
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
        retroView = RetroView(this)
        retroViewUtils = RetroViewUtils(this, retroView)

        lifecycle.addObserver(retroView.view)
        retroViewContainer.addView(retroView.view)

        retroView.getFrameRendered().observe(this) {
            if (it != true)
                return@observe

            retroViewUtils.restoreEmulatorState()
        }
    }

    private fun setupGamePads() {
        leftGamePadContainer.addView(leftGamePad.pad)
        rightGamePadContainer.addView(rightGamePad.pad)

        viewModel.subscribeGamePad(leftGamePad, retroView)
        viewModel.subscribeGamePad(rightGamePad, retroView)
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

    override fun onBackPressed() {
        retroViewUtils.preserveEmulatorState()
        menuDialog.show()
    }

    override fun onDestroy() {
        if (menuDialog.isShowing)
            menuDialog.dismiss()

        viewModel.dispose()
        super.onDestroy()
    }

    override fun onPause() {
        if (retroView.getFrameRendered().value == true)
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
