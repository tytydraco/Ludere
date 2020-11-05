package com.draco.ludere.utils

import com.draco.ludere.assets.PrivateData
import com.swordfish.libretrodroid.GLRetroView

class RetroViewUtils {
    companion object {
        fun reset(retroView: GLRetroView, privateData: PrivateData) {
            saveSRAM(retroView, privateData)
            retroView.reset()
        }

        fun saveSRAM(retroView: GLRetroView, privateData: PrivateData) {
            privateData.save.outputStream().use {
                it.write(retroView.serializeSRAM())
            }
        }

        fun saveState(retroView: GLRetroView, privateData: PrivateData) {
            privateData.state.outputStream().use {
                it.write(retroView.serializeState())
            }
        }

        fun loadState(retroView: GLRetroView, privateData: PrivateData) {
            if (!privateData.state.exists())
                return

            val bytes = privateData.state.inputStream().use {
                it.readBytes()
            }
            if (bytes.isNotEmpty())
                retroView.unserializeState(bytes)
        }

        fun saveTempState(retroView: GLRetroView, privateData: PrivateData) {
            /* Save a temporary state since Android killed the activity */
            val savedInstanceStateBytes = retroView.serializeState()
            privateData.tempState.outputStream().use {
                it.write(savedInstanceStateBytes)
            }
        }

        fun restoreTempState(retroView: GLRetroView, privateData: PrivateData) {
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

        fun toggleMute(retroView: GLRetroView) {
            retroView.audioEnabled = !retroView.audioEnabled
        }

        fun toggleFastForward(retroView: GLRetroView) {
            retroView.fastForwardEnabled = !retroView.fastForwardEnabled
        }

        fun nextDisk(retroView: GLRetroView) {
            val currentDisk = retroView.getCurrentDisk()
            if (currentDisk < retroView.getAvailableDisks())
                retroView.changeDisk(currentDisk + 1)
        }

        fun previousDisk(retroView: GLRetroView) {
            val currentDisk = retroView.getCurrentDisk()
            if (currentDisk > 0)
                retroView.changeDisk(currentDisk - 1)
        }
    }
}