package com.draco.libretrowrapper.utils

import android.content.Context
import com.draco.libretrowrapper.R
import java.io.File

class PrivateData(context: Context) {
    val rom = File("${context.filesDir.absolutePath}/${context.getString(R.string.config_rom_file)}")
    val core = File("${context.filesDir.absolutePath}/core")
    val save = File("${context.filesDir.absolutePath}/save")
    val state = File("${context.filesDir.absolutePath}/state")
    val savedInstanceState = File("${context.cacheDir.absolutePath}/saved_instance_state")
}