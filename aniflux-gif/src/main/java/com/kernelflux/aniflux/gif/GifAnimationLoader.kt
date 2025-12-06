package com.kernelflux.aniflux.gif

import android.content.Context
import com.kernelflux.aniflux.log.AniFluxLog
import com.kernelflux.aniflux.log.AniFluxLogCategory
import com.kernelflux.aniflux.annotation.AutoRegisterLoader
import com.kernelflux.aniflux.load.AnimationDownloader
import com.kernelflux.aniflux.load.AnimationLoader
import com.kernelflux.aniflux.util.AnimationTypeDetector
import com.kernelflux.gif.GifDrawable
import java.io.File
import java.io.InputStream

/**
 * GIF animation loader
 * 
 * Marked with @AutoRegisterLoader annotation, registration code will be automatically generated at compile time
 * 
 * @author: kernelflux
 * @date: 2025/01/XX
 */
@AutoRegisterLoader(animationType = "GIF")
class GifAnimationLoader : AnimationLoader<GifDrawable> {
    
    override fun loadFromPath(context: Context,path: String): GifDrawable? {
        return try {
            GifDrawable(path)
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to load GIF from path: $path", e)
            null
        }
    }
    
    override fun loadFromFile(context: Context,file: File): GifDrawable? {
        return try {
            GifDrawable(file)
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to load GIF from file: ${file.absolutePath}", e)
            null
        }
    }
    
    override fun loadFromResource(context: Context, resourceId: Int): GifDrawable? {
        return try {
            GifDrawable(context.resources, resourceId)
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to load GIF from resource: $resourceId", e)
            null
        }
    }
    
    override fun loadFromBytes(context: Context,bytes: ByteArray): GifDrawable? {
        return try {
            GifDrawable(bytes)
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to load GIF from bytes", e)
            null
        }
    }
    
    override fun loadFromInputStream(context: Context,inputStream: InputStream): GifDrawable? {
        return try {
            GifDrawable(inputStream)
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to load GIF from input stream", e)
            null
        }
    }
    
    override fun loadFromUrl(context: Context, url: String, downloader: AnimationDownloader): GifDrawable? {
        return try {
            val tempFile = downloader.download(context, url)
            loadFromFile(context, tempFile)
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to load GIF from URL: $url", e)
            null
        }
    }
    
    override fun loadFromAssetPath(context: Context, assetPath: String): GifDrawable? {
        return try {
            GifDrawable(context.assets, assetPath)
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to load GIF from asset path: $assetPath", e)
            null
        }
    }
    
    override fun getAnimationType(): AnimationTypeDetector.AnimationType {
        return AnimationTypeDetector.AnimationType.GIF
    }
}
