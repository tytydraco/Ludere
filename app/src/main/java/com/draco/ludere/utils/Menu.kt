package com.draco.ludere.utils

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import com.swordfish.libretrodroid.GLRetroView

class Menu(private val context: Context) {
    private val privateData = PrivateData(context)

    private val menuOptions = arrayOf(
        "Save State",
        "Load State",
        "Mute",
        "Fast Forward",
        "Change Slot"
    )

    private inner class MenuOnClickListener(private val retroView: GLRetroView) : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface?, which: Int) {
            when (menuOptions[which]) {
                "Save State" -> RetroViewUtils.saveState(retroView, privateData)
                "Load State" -> RetroViewUtils.loadState(retroView, privateData)
                "Mute" -> RetroViewUtils.toggleMute(retroView)
                "Fast Forward" -> RetroViewUtils.toggleFastForward(retroView)
            }
        }
    }

    fun show(retroView: GLRetroView) {
        AlertDialog.Builder(context)
            .setItems(menuOptions, MenuOnClickListener(retroView))
            .show()
    }
}