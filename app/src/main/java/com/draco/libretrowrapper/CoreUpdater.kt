package com.draco.libretrowrapper

import android.content.Context
import android.os.Build
import java.net.URL
import java.util.zip.ZipFile

class CoreUpdater(
    context: Context,
    private val privateData: PrivateData
) {
    private val coreName = context.getString(R.string.rom_core)
    private val abiName = Build.SUPPORTED_ABIS[0]

    private val coreDownloadURL = "https://buildbot.libretro.com/nightly/android/latest/$abiName/${coreName}_libretro_android.so.zip"

    private val tempCoreFile = createTempFile("core", "zip")

    fun update() {
        val connection = URL(coreDownloadURL).openConnection()

        val coreInputStream = connection.getInputStream()
        val tempCoreOutputStream = tempCoreFile.outputStream()
        coreInputStream.copyTo(tempCoreOutputStream)
        coreInputStream.close()
        tempCoreOutputStream.close()

        val coreZip = ZipFile(tempCoreFile)
        val coreZipEntry = coreZip.entries().asSequence().first()

        val coreOutputStream = privateData.core.outputStream()
        val coreZipEntryInputStream = coreZip.getInputStream(coreZipEntry)
        coreZipEntryInputStream.copyTo(coreOutputStream)
        coreZipEntryInputStream.close()
        coreOutputStream.close()
        coreZip.close()

        tempCoreFile.delete()
    }
}