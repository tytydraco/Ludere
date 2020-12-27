package com.draco.ludere.views

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.draco.ludere.R
import com.draco.ludere.utils.KeyCodes
import com.draco.ludere.viewmodels.GameActivityViewModel
import kotlinx.coroutines.*

class GameActivity : AppCompatActivity() {
    private val viewModel: GameActivityViewModel by viewModels()

    private lateinit var retroViewContainer: FrameLayout
    private lateinit var leftGamePadContainer: FrameLayout
    private lateinit var rightGamePadContainer: FrameLayout

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var panicDialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        lifecycle.addObserver(viewModel.retroView.view!!)

        retroViewContainer = findViewById(R.id.retroview_container)
        leftGamePadContainer = findViewById(R.id.left_container)
        rightGamePadContainer = findViewById(R.id.right_container)
        sharedPreferences = getPreferences(MODE_PRIVATE)

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

        viewModel.retroView.getInit().observe(this) {
            if (it == true) {
                with (viewModel.retroView.view!!) {
                    if (parent != null)
                        (parent as ViewGroup).removeView(this)
                    lifecycle.addObserver(this)
                    retroViewContainer.addView(this)
                }
            }
        }

        viewModel.retroView.getReady().observe(this) {
            if (it == true) {
                viewModel.restoreEmulatorState(sharedPreferences)

                if (resources.getBoolean(R.bool.config_gamepad_visible)) {
                    with (viewModel.leftGamePad.pad) {
                        if (parent != null)
                            (parent as ViewGroup).removeView(this)
                        leftGamePadContainer.addView(this)
                    }

                    with (viewModel.rightGamePad.pad) {
                        if (parent != null)
                            (parent as ViewGroup).removeView(this)
                        rightGamePadContainer.addView(this)
                    }

                    viewModel.subscribeGamePads()
                }
            }
        }

        viewModel.retroView.getError().observe(this) {
            if (it == true)
                panicDialog.show()
        }

        viewModel.updateVisibility.observe(this) {
            val visibility = if (viewModel.shouldShowGamePads())
                View.VISIBLE
            else
                View.GONE

            leftGamePadContainer.visibility = visibility
            rightGamePadContainer.visibility = visibility
        }

        viewModel.showMenu.observe(this) {
            showMenu()
        }
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
        viewModel.preserveEmulatorState(sharedPreferences)
        AlertDialog.Builder(this)
            .setItems(viewModel.menuOnClickListener.menuOptions, viewModel.menuOnClickListener)
            .show()
    }

    override fun onBackPressed() {
        showMenu()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (viewModel.retroView.view == null || keyCode !in KeyCodes.ValidKeyCodes)
            return super.onKeyDown(keyCode, event)

        viewModel.handleKeyDown(keyCode, event)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (viewModel.retroView.view == null || keyCode !in KeyCodes.ValidKeyCodes)
            return super.onKeyUp(keyCode, event)

        viewModel.handleKeyUp(keyCode, event)
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (viewModel.retroView.view == null)
            return super.onGenericMotionEvent(event)

        viewModel.handleGenericMotionEvent(event)
        return true
    }

    override fun onDestroy() {
        if (panicDialog.isShowing)
            panicDialog.dismiss()

        super.onDestroy()
    }

    override fun onPause() {
        if (viewModel.retroView.getReady().value == true)
            viewModel.preserveEmulatorState(sharedPreferences)

        super.onPause()
    }
}
