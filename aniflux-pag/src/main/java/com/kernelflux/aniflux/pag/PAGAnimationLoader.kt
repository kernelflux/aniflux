package com.kernelflux.aniflux.pag

import android.content.Context
import com.kernelflux.aniflux.log.AniFluxLog
import com.kernelflux.aniflux.log.AniFluxLogCategory
import com.kernelflux.aniflux.annotation.AutoRegisterLoader
import com.kernelflux.aniflux.load.AnimationDownloader
import com.kernelflux.aniflux.load.AnimationLoader
import com.kernelflux.aniflux.util.AnimationTypeDetector
import com.kernelflux.pag.PAGFile
import java.io.File
import java.io.InputStream

/**
 * PAG animation loader - references libpag library's loading approach
 * Supports loading PAG from file path, file, resource ID, byte array, input stream, network URL
 */
@AutoRegisterLoader(animationType = "PAG")
class PAGAnimationLoader : AnimationLoader<PAGFile> {

    override fun loadFromPath(context: Context, path: String): PAGFile? {
        return try {
            // ⚠️ Issue: PAGFile.Load(path) uses internal cache, same path will share underlying File object
            // ✅ Solution: Read file as byte array, use PAGFile.Load(byte[]) to create independent instance
            val file = java.io.File(path)
            if (file.exists()) {
                val bytes = file.readBytes()
                PAGFile.Load(bytes)
            } else {
                // If file doesn't exist, try direct load (might be network URL or other path)
                PAGFile.Load(path)
            }
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to load PAG from path: $path", e)
            null
        }
    }

    override fun loadFromFile(context: Context, file: File): PAGFile? {
        return try {
            // ⚠️ Issue: PAGFile.Load(filePath) uses internal cache, same path will share underlying File object
            // This causes only the last one to display animation when multiple PAGViews use the same PAGFile
            // ✅ Solution: Use PAGFile.Load(byte[]) to create independent PAGFile instance for each request
            val bytes = file.readBytes()
            PAGFile.Load(bytes)
        } catch (e: Exception) {
            AniFluxLog.e(
                "PagAnimationLoader",
                "Failed to load PAG from file: ${file.absolutePath}",
                e
            )
            null
        }
    }

    override fun loadFromResource(context: Context, resourceId: Int): PAGFile? {
        return try {
            // PAG library doesn't have method to load directly from resource ID, need to read as byte array first
            val inputStream = context.resources.openRawResource(resourceId)
            val bytes = inputStream.readBytes()
            inputStream.close()
            loadFromBytes(context, bytes)
        } catch (e: Exception) {
            AniFluxLog.e(
                "PagAnimationLoader",
                "Failed to load PAG from resource: $resourceId",
                e
            )
            null
        }
    }

    override fun loadFromBytes(context: Context, bytes: ByteArray): PAGFile? {
        return try {
            PAGFile.Load(bytes)
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to load PAG from bytes", e)
            null
        }
    }

    override fun loadFromInputStream(context: Context, inputStream: InputStream): PAGFile? {
        return try {
            val bytes = inputStream.readBytes()
            loadFromBytes(context, bytes)
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to load PAG from input stream", e)
            null
        }
    }

    override fun loadFromUrl(
        context: Context,
        url: String,
        downloader: AnimationDownloader
    ): PAGFile? {
        return try {
            // Download file
            val tempFile = downloader.download(context, url)
            // Load from temporary file
            val result = loadFromFile(context, tempFile)
            result
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to load PAG from URL: $url", e)
            null
        }
    }

    override fun loadFromAssetPath(context: Context, assetPath: String): PAGFile? {
        return try {
            // ⚠️ Issue: PAGFile.Load(assets, path) may also have cache mechanism
            // ✅ Solution: Read asset as byte array, use PAGFile.Load(byte[]) to create independent instance
            val inputStream = context.assets.open(assetPath)
            val bytes = inputStream.readBytes()
            inputStream.close()
            PAGFile.Load(bytes)
        } catch (e: Exception) {
            AniFluxLog.e(
                "PagAnimationLoader",
                "Failed to load PAG from asset path: $assetPath",
                e
            )
            null
        }
    }

    override fun getAnimationType(): AnimationTypeDetector.AnimationType {
        return AnimationTypeDetector.AnimationType.PAG
    }
}

