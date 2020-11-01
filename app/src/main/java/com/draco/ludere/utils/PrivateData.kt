package com.draco.ludere.utils

import android.content.Context
import com.draco.ludere.R
import java.io.File

class PrivateData(context: Context) {
    /* Prefer external storage, fall back on internal storage */
    private val storagePath = (context.getExternalFilesDir(null) ?: context.filesDir).absolutePath
    val systemDirPath = "$storagePath/system"
    val internalDirPath = "$storagePath/internal"

    init {
        File(systemDirPath).mkdirs()
        File(internalDirPath).mkdirs()
    }

    private val romFullName = context.getString(R.string.config_rom_file)
    val rom = File("$systemDirPath/$romFullName")
    val save = File("$internalDirPath/$romFullName.sav")
    val state = File("$internalDirPath/$romFullName.state")
    val tempState = File("$internalDirPath/$romFullName.tempstate")
}