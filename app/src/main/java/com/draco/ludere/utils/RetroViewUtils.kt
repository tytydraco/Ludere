package com.draco.ludere.utils

import com.swordfish.libretrodroid.GLRetroView
import java.io.File

class RetroViewUtils(private val retroView: GLRetroView) {
    companion object {
        const val FAST_FORWARD_SPEED = 2
        const val RESTORE_STATE_ATTEMPTS = 10
        const val RESTORE_STATE_FAIL_DELAY_MS = 50L
    }

    fun saveSRAMTo(file: File) {
        file.outputStream().use {
            it.write(retroView.serializeSRAM())
        }
    }

    fun loadStateFrom(file: File) {
        /* Don't bother loading a state if it doesn't exist */
        if (!file.exists())
            return

        /* Fetch the state bytes */
        val stateBytes = file.inputStream().use {
            it.readBytes()
        }

        /* Don't bother if there's nothing to restore */
        if (stateBytes.isEmpty())
            return

        /* Load the state */
        var remainingTries = RESTORE_STATE_ATTEMPTS
        while (!retroView.unserializeState(stateBytes) && remainingTries-- > 0)
            Thread.sleep(RESTORE_STATE_FAIL_DELAY_MS)
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
            FAST_FORWARD_SPEED
        else
            1
    }

    fun nextDisk() {
        val currentDisk = retroView.getCurrentDisk()
        if (currentDisk < retroView.getAvailableDisks())
            retroView.changeDisk(currentDisk + 1)
    }

    fun previousDisk() {
        val currentDisk = retroView.getCurrentDisk()
        if (currentDisk > 0)
            retroView.changeDisk(currentDisk - 1)
    }
}