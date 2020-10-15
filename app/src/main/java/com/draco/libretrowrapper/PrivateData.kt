package com.draco.libretrowrapper

import android.content.Context
import java.io.File

class PrivateData(context: Context) {
    val rom = File("${context.filesDir.absolutePath}/rom")
    val save = File("${context.filesDir.absolutePath}/save")
    val state = File("${context.filesDir.absolutePath}/state")
    val savedInstanceState = File("${context.cacheDir}/saved_instance_state")
}