package com.kernelflux.aniflux.vap

import android.content.Context
import com.kernelflux.aniflux.annotation.AutoRegisterLoader
import com.kernelflux.aniflux.load.AnimationDownloader
import com.kernelflux.aniflux.load.AnimationLoader
import com.kernelflux.aniflux.log.AniFluxLog
import com.kernelflux.aniflux.log.AniFluxLogCategory
import com.kernelflux.aniflux.util.AnimationTypeDetector
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * VAP animation loader - references vap library's loading approach
 * Supports loading VAP from file path, file, resource ID, byte array, input stream, network URL
 * VAP library is based on File and IFileContainer, so most sources need to be converted to File first
 */
@AutoRegisterLoader(animationType = "VAP")
class VAPAnimationLoader : AnimationLoader<File> {

    override fun loadFromPath(context: Context, path: String): File? {
        return try {
            val file = File(path)
            if (file.exists() && file.isFile) {
                file
            } else {
                AniFluxLog.e(AniFluxLogCategory.LOADER, "File does not exist or is not a file: $path")
                null
            }
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to load VAP from path: $path", e)
            null
        }
    }

    override fun loadFromFile(context: Context, file: File): File? {
        return try {
            if (file.exists() && file.isFile) {
                file
            } else {
                AniFluxLog.e(AniFluxLogCategory.LOADER, "File does not exist or is not a file: ${file.absolutePath}")
                null
            }
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to load VAP from file: ${file.absolutePath}", e)
            null
        }
    }

    override fun loadFromResource(context: Context, resourceId: Int): File? {
        return try {
            // Read from resource to temporary file
            val inputStream = context.resources.openRawResource(resourceId)
            val tempFile = createTempFileFromInputStream(context, inputStream, "vap_resource_$resourceId")
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to load VAP from resource: $resourceId", e)
            null
        }
    }

    override fun loadFromBytes(context: Context, bytes: ByteArray): File? {
        return try {
            val inputStream = bytes.inputStream()
            createTempFileFromInputStream(context, inputStream, "vap_bytes_${bytes.hashCode()}")
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to load VAP from bytes", e)
            null
        }
    }

    override fun loadFromInputStream(context: Context, inputStream: InputStream): File? {
        return try {
            createTempFileFromInputStream(context, inputStream, "vap_stream_${inputStream.hashCode()}")
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to load VAP from input stream", e)
            null
        }
    }

    override fun loadFromUrl(context: Context, url: String, downloader: AnimationDownloader): File? {
        return try {
            // Download file
            val tempFile = downloader.download(context, url)
            // Verify file exists
            if (tempFile.exists() && tempFile.isFile) {
                tempFile
            } else {
                AniFluxLog.e(AniFluxLogCategory.LOADER, "Downloaded file is invalid: $url")
                null
            }
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to load VAP from URL: $url", e)
            null
        }
    }

    override fun loadFromAssetPath(context: Context, assetPath: String): File? {
        return try {
            // Read from Asset to temporary file
            val inputStream = context.assets.open(assetPath)
            val tempFile = createTempFileFromInputStream(context, inputStream, "vap_asset_${assetPath.replace("/", "_")}")
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to load VAP from asset path: $assetPath", e)
            null
        }
    }

    override fun getAnimationType(): AnimationTypeDetector.AnimationType {
        return AnimationTypeDetector.AnimationType.VAP
    }

    /**
     * Create temporary file from input stream
     */
    private fun createTempFileFromInputStream(
        context: Context,
        inputStream: InputStream,
        prefix: String
    ): File? {
        return try {
            // Create temporary file
            val tempFile = File.createTempFile(prefix, ".vap", context.cacheDir)
            tempFile.deleteOnExit() // Delete temporary file when app exits

            // Write input stream content to temporary file
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
                outputStream.flush()
            }

            tempFile
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to create temp file from input stream", e)
            null
        }
    }
}

