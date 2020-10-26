package com.draco.ludere.utils

import android.content.Context
import com.draco.ludere.R
import java.io.File

class PrivateData(context: Context) {
    val systemDirPath = "${context.filesDir.absolutePath}/system"
    val internalDirPath = "${context.filesDir.absolutePath}/internal"

    init {
        File(systemDirPath).mkdirs()
        File(internalDirPath).mkdirs()
    }

    val rom = File("$systemDirPath/${context.getString(R.string.config_rom_file)}")
    val save = File("$internalDirPath/save")
    val state = File("$internalDirPath/state")
    val tempState = File("$internalDirPath/temp_state")
}