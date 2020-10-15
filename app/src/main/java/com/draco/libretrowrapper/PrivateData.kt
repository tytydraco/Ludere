package com.draco.libretrowrapper

import android.content.Context
import java.io.File

class PrivateData(context: Context) {
    val rom = File("${context.filesDir.absolutePath}/${context.getString(R.string.file_rom)}")
    val save = File("${context.filesDir.absolutePath}/${context.getString(R.string.file_save)}")
    val state = File("${context.filesDir.absolutePath}/${context.getString(R.string.file_state)}")
    val savedInstanceState = File("${context.filesDir.absolutePath}/${context.getString(R.string.file_state)}")
}