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
        activity.getString(R.string.menu_reset),
        activity.getString(R.string.menu_save_state),
        activity.getString(R.string.menu_load_state),
        activity.getString(R.string.menu_mute),
        activity.getString(R.string.menu_fast_forward)
    ).toTypedArray()

    private val retroViewUtils = RetroViewUtils(retroView)

    private inner class MenuOnClickListener : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface?, which: Int) {
            when (menuOptions[which]) {
                activity.getString(R.string.menu_reset) -> retroView.reset()
                activity.getString(R.string.menu_save_state) -> retroViewUtils.saveStateTo(privateData.state)
                activity.getString(R.string.menu_load_state) -> retroViewUtils.loadStateFrom(privateData.state)
                activity.getString(R.string.menu_mute) -> retroViewUtils.toggleMute()
                activity.getString(R.string.menu_fast_forward) -> retroViewUtils.toggleFastForward()
            }
        }
    }

    fun show() {
        /* Save SRAM and tempstate as a precaution; treat it as a pause */
        retroViewUtils.saveSRAMTo(privateData.sram)
        retroViewUtils.saveStateTo(privateData.tempState)

        /* Show menu */
        MaterialAlertDialogBuilder(activity)
            .setItems(menuOptions, MenuOnClickListener())
            .show()
    }
}