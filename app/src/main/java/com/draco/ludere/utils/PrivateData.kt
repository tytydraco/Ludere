package com.draco.ludere.utils

import android.content.Context
import com.draco.ludere.R
import java.io.File

class PrivateData(context: Context) {
    /* Prefer external storage, fall back on internal storage */
    private val storagePath = (context.getExternalFilesDir(null) ?: context.filesDir).absolutePath
    val systemDir = File("$storagePath/system")
    val internalDir = File("$storagePath/internal")

    fun prepare() {
        systemDir.mkdirs()
        internalDir.mkdirs()
    }

    private val romFullName = context.getString(R.string.config_rom_path)
    val rom = File("${systemDir.path}/$romFullName")
    val save = File("${internalDir.path}/$romFullName.save")
    val state = File("${internalDir.path}/$romFullName.state")
    val tempState = File("${internalDir.path}/$romFullName.tempstate")
}