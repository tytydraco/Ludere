package com.draco.ludere.utils

import android.app.Activity
import android.content.DialogInterface
import com.draco.ludere.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.swordfish.libretrodroid.GLRetroView

class Menu(
    private val activity: Activity,
    private val retroView: GLRetroView
) {
    private val privateData = PrivateData(activity)

    private val menuOptions = listOfNotNull(
        activity.getString(R.string.menu_exit),
        activity.getString(R.string.menu_save_state),
        activity.getString(R.string.menu_load_state),
        activity.getString(R.string.menu_mute),
        activity.getString(R.string.menu_fast_forward),
        activity.getString(R.string.menu_next_disk).takeIf { retroView.getAvailableDisks() > 0 },
        activity.getString(R.string.menu_previous_disk).takeIf { retroView.getAvailableDisks() > 0 }
    ).toTypedArray()

    private inner class MenuOnClickListener : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface?, which: Int) {
            when (menuOptions[which]) {
                activity.getString(R.string.menu_exit) -> activity.finishAffinity()
                activity.getString(R.string.menu_save_state) -> RetroViewUtils.saveState(retroView, privateData)
                activity.getString(R.string.menu_load_state) -> RetroViewUtils.loadState(retroView, privateData)
                activity.getString(R.string.menu_mute) -> RetroViewUtils.toggleMute(retroView)
                activity.getString(R.string.menu_fast_forward) -> RetroViewUtils.toggleFastForward(retroView)
                activity.getString(R.string.menu_next_disk) -> RetroViewUtils.nextDisk(retroView)
                activity.getString(R.string.menu_previous_disk) -> RetroViewUtils.previousDisk(retroView)
            }
        }
    }

    fun show() {
        MaterialAlertDialogBuilder(activity)
            .setItems(menuOptions, MenuOnClickListener())
            .show()
    }
}