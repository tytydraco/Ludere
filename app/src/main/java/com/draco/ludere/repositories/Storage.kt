package com.draco.ludere.repositories

import android.content.Context
import java.io.File

class Storage(context: Context) {
    companion object {
        @Volatile private var instance: Storage? = null
        fun getInstance(context: Context): Storage = instance ?: synchronized(this) {
            instance ?: Storage(context).also { instance = it }
        }
    }

    val storagePath: String = (context.getExternalFilesDir(null) ?: context.filesDir).path
    val sram = File("$storagePath/sram")
    val state = File("$storagePath/state")
    val tempState = File("$storagePath/tempstate")
}