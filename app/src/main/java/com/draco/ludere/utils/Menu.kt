package com.draco.ludere.utils

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import com.draco.ludere.R
import com.swordfish.libretrodroid.GLRetroView

class Menu(
    private val context: Context,
    private val retroView: GLRetroView
) {
    private val privateData = PrivateData(context)

    private val menuOptions = listOfNotNull(
        context.getString(R.string.menu_save_state),
        context.getString(R.string.menu_load_state),
        context.getString(R.string.menu_mute),
        context.getString(R.string.menu_fast_forward),
        context.getString(R.string.menu_next_disc).takeIf { retroView.getAvailableDisks() > 0 },
        context.getString(R.string.menu_previous_disk).takeIf { retroView.getAvailableDisks() > 0 }
    ).toTypedArray()

    private inner class MenuOnClickListener : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface?, which: Int) {
            when (menuOptions[which]) {
                context.getString(R.string.menu_save_state) -> RetroViewUtils.saveState(retroView, privateData)
                context.getString(R.string.menu_load_state) -> RetroViewUtils.loadState(retroView, privateData)
                context.getString(R.string.menu_mute) -> RetroViewUtils.toggleMute(retroView)
                context.getString(R.string.menu_fast_forward) -> RetroViewUtils.toggleFastForward(retroView)
                context.getString(R.string.menu_next_disc) -> RetroViewUtils.nextDisc(retroView)
                context.getString(R.string.menu_previous_disk) -> RetroViewUtils.previousDisc(retroView)
            }
        }
    }

    fun show() {
        AlertDialog.Builder(context)
            .setItems(menuOptions, MenuOnClickListener())
            .show()
    }
}