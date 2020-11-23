package com.draco.ludere.ui

import android.app.Activity
import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import com.draco.ludere.R
import com.draco.ludere.assets.PrivateData
import com.draco.ludere.utils.RetroViewUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.swordfish.libretrodroid.GLRetroView

class Menu(
    private val activity: Activity,
    private val retroView: GLRetroView
) {
    private val privateData = PrivateData(activity)

    private val menuOptions = listOfNotNull(
        activity.getString(R.string.menu_exit),
        activity.getString(R.string.menu_reset),
        activity.getString(R.string.menu_save_state),
        activity.getString(R.string.menu_load_state),
        activity.getString(R.string.menu_mute),
        activity.getString(R.string.menu_fast_forward),
        activity.getString(R.string.menu_rotation_lock),
        activity.getString(R.string.menu_next_disk).takeIf { retroView.getAvailableDisks() > 0 },
        activity.getString(R.string.menu_previous_disk).takeIf { retroView.getAvailableDisks() > 0 }
    ).toTypedArray()

    private val retroViewUtils = RetroViewUtils(privateData, retroView)

    private inner class MenuOnClickListener : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface?, which: Int) {
            when (menuOptions[which]) {
                activity.getString(R.string.menu_exit) -> activity.finishAffinity()
                activity.getString(R.string.menu_reset) -> retroView.reset()
                activity.getString(R.string.menu_save_state) -> retroViewUtils.saveState()
                activity.getString(R.string.menu_load_state) -> retroViewUtils.loadState()
                activity.getString(R.string.menu_mute) -> retroViewUtils.toggleMute()
                activity.getString(R.string.menu_fast_forward) -> retroViewUtils.toggleFastForward()
                activity.getString(R.string.menu_rotation_lock) -> {
                    /* If we are already unlocked, lock to the current orientation */
                    activity.requestedOrientation = if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                        when (activity.resources.configuration.orientation) {
                            Configuration.ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                            else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                    } else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
                activity.getString(R.string.menu_next_disk) -> retroViewUtils.nextDisk()
                activity.getString(R.string.menu_previous_disk) -> retroViewUtils.previousDisk()
            }
        }
    }

    fun show() {
        /* Save SRAM and tempstate as a precaution; treat it as a pause */
        retroViewUtils.saveSRAM()
        retroViewUtils.saveTempState()

        /* Show menu */
        MaterialAlertDialogBuilder(activity)
            .setItems(menuOptions, MenuOnClickListener())
            .show()
    }
}