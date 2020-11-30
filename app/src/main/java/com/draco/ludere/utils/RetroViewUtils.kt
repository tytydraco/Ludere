package com.draco.ludere.utils

import com.swordfish.libretrodroid.GLRetroView
import java.io.File

class RetroViewUtils(private val retroView: GLRetroView) {
    fun saveSRAMTo(file: File) {
        file.outputStream().use {
            it.write(retroView.serializeSRAM())
        }
    }

    fun loadStateFrom(file: File) {
        if (!file.exists())
            return

        val stateBytes = file.inputStream().use {
            it.readBytes()
        }

        if (stateBytes.isEmpty())
            return

        retroView.unserializeState(stateBytes)
    }

    fun saveStateTo(file: File) {
        file.outputStream().use {
            it.write(retroView.serializeState())
        }
    }

    fun toggleMute() {
        retroView.audioEnabled = !retroView.audioEnabled
    }

    fun toggleFastForward() {
        retroView.frameSpeed = if (retroView.frameSpeed == 1)
            2
        else
            1
    }
}