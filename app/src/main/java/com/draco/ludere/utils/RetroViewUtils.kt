package com.draco.ludere.utils

import com.draco.ludere.assets.PrivateData
import com.swordfish.libretrodroid.GLRetroView

class RetroViewUtils(
    private val privateData: PrivateData,
    private val retroView: GLRetroView
) {
    companion object {
        const val FAST_FORWARD_SPEED = 2
    }

    fun reset() {
        saveSRAM()
        retroView.reset()
    }

    fun saveSRAM() {
        privateData.save.outputStream().use {
            it.write(retroView.serializeSRAM())
        }
    }

    fun saveState() {
        privateData.state.outputStream().use {
            it.write(retroView.serializeState())
        }
    }

    fun loadState() {
        if (!privateData.state.exists())
            return

        val bytes = privateData.state.inputStream().use {
            it.readBytes()
        }
        if (bytes.isNotEmpty())
            retroView.unserializeState(bytes)
    }

    fun saveTempState() {
        /* Save a temporary state since Android killed the activity */
        val savedInstanceStateBytes = retroView.serializeState()
        privateData.tempState.outputStream().use {
            it.write(savedInstanceStateBytes)
        }
    }

    fun restoreTempState() {
        /* Don't bother restoring a temporary state if it doesn't exist */
        if (!privateData.tempState.exists())
            return

        /* Fetch the state bytes */
        val stateBytes = privateData.tempState.inputStream().use {
            it.readBytes()
        }

        /* Restore the temporary state */
        var remainingTries = 10
        while (!retroView.unserializeState(stateBytes) && remainingTries-- > 0)
            Thread.sleep(50)
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