package com.onestopshop

import android.content.Context
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream
import kotlin.concurrent.thread

class AssetExtractor(private val context: Context) {

    fun extractAssets() {
        thread {
            val assetManager = context.assets
            val targetDir = File(context.filesDir, "ubuntu_rootfs")

            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            try {
                assetManager.open("ubuntu-rootfs.tar.gz").use { inputStream ->
                    GZIPInputStream(inputStream).use { gzipStream ->
                        TarArchiveInputStream(gzipStream).use { tarStream ->
                            var entry = tarStream.nextTarEntry
                            while (entry != null) {
                                // Mitigate Zip Slip vulnerability
                                val targetPath = targetDir.canonicalPath
                                val outputFile = File(targetDir, entry.name)
                                val outputPath = outputFile.canonicalPath
                                if (!outputPath.startsWith(targetPath + File.separator)) {
                                    throw SecurityException("Path traversal attack detected: \${entry.name}")
                                }

                                if (entry.isDirectory) {
                                    outputFile.mkdirs()
                                } else {
                                    outputFile.parentFile?.mkdirs()
                                    FileOutputStream(outputFile).use { output ->
                                        tarStream.copyTo(output)
                                    }

                                    // Set executable permissions for container binaries
                                    if (entry.name.startsWith("bin/") || entry.name.startsWith("usr/bin/")) {
                                        outputFile.setExecutable(true, false)
                                    }
                                }
                                entry = tarStream.nextTarEntry
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
