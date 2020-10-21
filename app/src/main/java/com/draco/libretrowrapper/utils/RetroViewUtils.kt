package com.draco.libretrowrapper.utils

import com.swordfish.libretrodroid.GLRetroView
import java.util.concurrent.CountDownLatch

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
            with (privateData.savedInstanceState.outputStream()) {
                write(savedInstanceStateBytes)
                close()
            }
        }

        fun restoreTempState(retroView: GLRetroView, privateData: PrivateData, retroViewReadyLatch: CountDownLatch) {
            /* Don't bother restoring a temporary state if it doesn't exist */
            if (!privateData.savedInstanceState.exists())
                return

            Thread {
                /* Wait for the GLRetroView to become usable */
                retroViewReadyLatch.await()

                /* Fetch the state bytes */
                val stateInputStream = privateData.savedInstanceState.inputStream()
                val stateBytes = stateInputStream.readBytes()
                stateInputStream.close()

                /* Invalidate the temporary state so we cannot restore it twice */
                privateData.savedInstanceState.delete()

                /* Restore the temporary state */
                retroView.unserializeState(stateBytes)
            }.start()
        }
    }
}