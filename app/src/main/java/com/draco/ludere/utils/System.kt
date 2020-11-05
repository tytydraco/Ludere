package com.draco.ludere.utils

import android.content.Context
import com.draco.ludere.R
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File

class System(private val context: Context) {
    fun extractToFilesDir() {
        /* Bail if we have already populated our assets folder */
        if (!context.filesDir.listFiles().isNullOrEmpty())
            return

        try {
            /* Iterate over all tarred items */
            context.resources.openRawResource(R.raw.system).use { systemTarInputStream ->
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