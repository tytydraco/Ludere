package com.draco.ludere.utils

import android.content.Context
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File

class System(private val context: Context) {
    companion object {
        const val SYSTEM_BIN_NAME = "system.bin"
    }

    fun extractToFilesDir() {
        /* Bail if we have already populated our assets folder */
        if (!context.filesDir.listFiles().isNullOrEmpty())
            return

        /* Bail if we don't even have a system.bin tarball */
        if (context.assets.list("")?.contains(SYSTEM_BIN_NAME) == false)
            return

        try {
            /* Iterate over all tarred items */
            context.assets.open(SYSTEM_BIN_NAME).use { systemTarInputStream ->
                GzipCompressorInputStream(systemTarInputStream).use { gzipCompressorInputStream ->
                    TarArchiveInputStream(gzipCompressorInputStream).use { tarArchiveInputStream ->
                        while (true) {
                            val tarEntry = tarArchiveInputStream.nextEntry ?: break
                            val tarEntryOutFile = File(context.filesDir.path, tarEntry.name)

                            /* If this is a directory, prepare the file structure and skip */
                            if (tarEntry.isDirectory) {
                                tarEntryOutFile.mkdir()
                                continue
                            }

                            /* Copy the file to the output location */
                            tarEntryOutFile.outputStream().use {
                                tarArchiveInputStream.copyTo(it)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }
}