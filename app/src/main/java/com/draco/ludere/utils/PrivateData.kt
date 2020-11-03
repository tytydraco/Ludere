package com.draco.ludere.utils

import android.content.Context
import com.draco.ludere.R
import java.io.File

class PrivateData(
    context: Context,
    romHash: Int
) {
    /* Prefer external storage, fall back on internal storage */
    private val storagePath = (context.getExternalFilesDir(null) ?: context.filesDir).absolutePath
    val systemDirPath = "$storagePath/system"
    val internalDirPath = "$storagePath/internal"

    init {
        File(systemDirPath).mkdirs()
        File("$internalDirPath/$romHash").mkdirs()
    }

    private val romFullName = context.getString(R.string.config_rom_path)
    val save = File("$internalDirPath/$romHash/$romFullName.save")
    val state = File("$internalDirPath/$romHash/$romFullName.state")
    val tempState = File("$internalDirPath/$romHash/$romFullName.tempstate")
}