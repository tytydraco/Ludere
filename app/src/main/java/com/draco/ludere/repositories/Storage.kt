package com.draco.ludere.repositories

import android.content.Context
import java.io.File

/**
 * Singleton for globally accessible ROM metadata
 */
class Storage(context: Context) {
    companion object {
        @Volatile private var instance: Storage? = null
        fun getInstance(context: Context): Storage = instance ?: synchronized(this) {
            instance ?: Storage(context).also { instance = it }
        }
    }

    val storagePath: String = (context.getExternalFilesDir(null) ?: context.filesDir).path
    val cachePath: String = (context.externalCacheDir ?: context.cacheDir).path
    val rom = File("$cachePath/rom")
    val sram = File("$storagePath/sram")
    val state = File("$storagePath/state")
    val tempState = File("$storagePath/tempstate")
}