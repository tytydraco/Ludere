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
import com.draco.ludere.viewmodels.GameActivityViewModel
import kotlinx.coroutines.*

class GameActivity : AppCompatActivity() {
    private val viewModel: GameActivityViewModel by viewModels()

    private lateinit var retroViewContainer: FrameLayout
    private lateinit var leftGamePadContainer: FrameLayout
    private lateinit var rightGamePadContainer: FrameLayout

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var menuDialog: AlertDialog

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

        menuDialog = AlertDialog.Builder(this)
            .setItems(viewModel.menuOnClickListener.menuOptions, viewModel.menuOnClickListener)
            .create()

        with (viewModel.retroView.view!!) {
            lifecycle.addObserver(this)
            retroViewContainer.addView(this)
            leftGamePadContainer.addView(viewModel.leftGamePad.pad)
            rightGamePadContainer.addView(viewModel.rightGamePad.pad)
            viewModel.subscribeGamePads()
        }

        viewModel.retroView.getFrameRendered().observe(this) {
            if (it != true)
                return@observe

            viewModel.restoreEmulatorState(sharedPreferences)
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

    override fun onBackPressed() {
        viewModel.preserveEmulatorState(sharedPreferences)
        menuDialog.show()
    }

    override fun onDestroy() {
        if (menuDialog.isShowing)
            menuDialog.dismiss()

        super.onDestroy()
    }

    override fun onPause() {
        if (viewModel.retroView.getFrameRendered().value == true)
            viewModel.preserveEmulatorState(sharedPreferences)

        super.onPause()
    }
}
