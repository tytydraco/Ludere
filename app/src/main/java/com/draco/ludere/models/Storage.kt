package com.draco.ludere.models

import java.io.File

class Storage {
    lateinit var storagePath: String
    lateinit var romBytes: ByteArray
    lateinit var sram: File
    lateinit var state: File
    lateinit var tempState: File
}