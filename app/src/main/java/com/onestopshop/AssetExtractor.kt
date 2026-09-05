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
                // Extract proot binary
                assetManager.open("proot").use { inputStream ->
                    val prootFile = File(targetDir, "proot")
                    FileOutputStream(prootFile).use { output ->
                        inputStream.copyTo(output)
                    }
                    prootFile.setExecutable(true, false)
                }

                // Extract ubuntu rootfs
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

                                    // Set executable permissions for container binaries based on tar entry mode
                                    if ((entry.mode and 0b001_001_001) != 0) { // Check for execute bit (owner, group, or other)
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
