package com.kernelflux.aniflux.load

import android.content.Context
import android.content.res.AssetManager
import com.kernelflux.aniflux.util.AnimationTypeDetector
import pl.droidsonroids.gif.GifDrawable
import java.io.File
import java.io.InputStream

/**
 * GIF动画加载器 - 参考android-gif-drawable库的加载方式
 * 支持从文件路径、文件、资源ID、字节数组、输入流、网络URL加载GIF
 */
class GifAnimationLoader : AnimationLoader<GifDrawable> {
    
    override fun loadFromPath(context: Context,path: String): GifDrawable? {
        return try {
            GifDrawable(path)
        } catch (e: Exception) {
            android.util.Log.e("GifAnimationLoader", "Failed to load GIF from path: $path", e)
            null
        }
    }
    
    override fun loadFromFile(context: Context,file: File): GifDrawable? {
        return try {
            GifDrawable(file)
        } catch (e: Exception) {
            android.util.Log.e("GifAnimationLoader", "Failed to load GIF from file: ${file.absolutePath}", e)
            null
        }
    }
    
    override fun loadFromResource(context: Context, resourceId: Int): GifDrawable? {
        return try {
            GifDrawable(context.resources, resourceId)
        } catch (e: Exception) {
            android.util.Log.e("GifAnimationLoader", "Failed to load GIF from resource: $resourceId", e)
            null
        }
    }
    
    override fun loadFromBytes(context: Context,bytes: ByteArray): GifDrawable? {
        return try {
            GifDrawable(bytes)
        } catch (e: Exception) {
            android.util.Log.e("GifAnimationLoader", "Failed to load GIF from bytes", e)
            null
        }
    }
    
    override fun loadFromInputStream(context: Context,inputStream: InputStream): GifDrawable? {
        return try {
            GifDrawable(inputStream)
        } catch (e: Exception) {
            android.util.Log.e("GifAnimationLoader", "Failed to load GIF from input stream", e)
            null
        }
    }
    
    override fun loadFromUrl(context: Context,url: String, downloader: AnimationDownloader): GifDrawable? {
        return try {
            // 下载文件
            val tempFile = downloader.download(context,url)
            
            // 从临时文件加载
            val result = loadFromFile(context,tempFile)

            result
        } catch (e: Exception) {
            android.util.Log.e("GifAnimationLoader", "Failed to load GIF from URL: $url", e)
            null
        }
    }
    
    override fun loadFromAssetPath(context: Context, assetPath: String): GifDrawable? {
        return try {
            GifDrawable(context.assets, assetPath)
        } catch (e: Exception) {
            android.util.Log.e("GifAnimationLoader", "Failed to load GIF from asset path: $assetPath", e)
            null
        }
    }
    
    override fun getAnimationType(): AnimationTypeDetector.AnimationType {
        return AnimationTypeDetector.AnimationType.GIF
    }
}
