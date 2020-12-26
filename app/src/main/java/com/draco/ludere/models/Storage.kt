package com.draco.ludere.models

import java.io.File

data class Storage(
    var storagePath: String,
    var sram: File,
    var state: File,
    var tempState: File
)