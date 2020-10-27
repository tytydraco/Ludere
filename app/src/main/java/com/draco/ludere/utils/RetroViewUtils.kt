package com.draco.ludere.utils

import com.swordfish.libretrodroid.GLRetroView

class RetroViewUtils {
    companion object {
        fun saveState(retroView: GLRetroView, privateData: PrivateData) {
            privateData.state.writeBytes(retroView.serializeState())
        }

        fun loadState(retroView: GLRetroView, privateData: PrivateData) {
            if (!privateData.state.exists())
                return

            val bytes = privateData.state.readBytes()
            if (bytes.isNotEmpty())
                retroView.unserializeState(bytes)
        }

        fun saveTempState(retroView: GLRetroView, privateData: PrivateData) {
            /* Save a temporary state since Android killed the activity */
            val savedInstanceStateBytes = retroView.serializeState()
            with (privateData.tempState.outputStream()) {
                write(savedInstanceStateBytes)
                close()
            }
        }

        fun restoreTempState(retroView: GLRetroView, privateData: PrivateData) {
            /* Don't bother restoring a temporary state if it doesn't exist */
            if (!privateData.tempState.exists())
                return

            /* Fetch the state bytes */
            val stateInputStream = privateData.tempState.inputStream()
            val stateBytes = stateInputStream.readBytes()
            stateInputStream.close()

            /* Restore the temporary state */
            var remainingTries = 10
            while (!retroView.unserializeState(stateBytes) && remainingTries-- > 0)
                Thread.sleep(50)
        }
    }
}