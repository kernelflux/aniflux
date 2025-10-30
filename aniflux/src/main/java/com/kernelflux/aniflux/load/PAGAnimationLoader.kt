package com.kernelflux.aniflux.load

import android.content.Context
import com.kernelflux.aniflux.util.AnimationTypeDetector
import org.libpag.PAGFile
import java.io.File
import java.io.InputStream

/**
 * PAG动画加载器 - 参考libpag库的加载方式
 * 支持从文件路径、文件、资源ID、字节数组、输入流、网络URL加载PAG
 */
class PAGAnimationLoader : AnimationLoader<PAGFile> {

    override fun loadFromPath(context: Context,path: String): PAGFile? {
        return try {
            PAGFile.Load(path)
        } catch (e: Exception) {
            android.util.Log.e("PagAnimationLoader", "Failed to load PAG from path: $path", e)
            null
        }
    }

    override fun loadFromFile(context: Context,file: File): PAGFile? {
        return try {
            PAGFile.Load(file.absolutePath)
        } catch (e: Exception) {
            android.util.Log.e(
                "PagAnimationLoader",
                "Failed to load PAG from file: ${file.absolutePath}",
                e
            )
            null
        }
    }

    override fun loadFromResource(context: Context, resourceId: Int): PAGFile? {
        return try {
            // PAG库没有直接从资源ID加载的方法，需要先读取为字节数组
            val inputStream = context.resources.openRawResource(resourceId)
            val bytes = inputStream.readBytes()
            inputStream.close()
            loadFromBytes(context,bytes)
        } catch (e: Exception) {
            android.util.Log.e(
                "PagAnimationLoader",
                "Failed to load PAG from resource: $resourceId",
                e
            )
            null
        }
    }

    override fun loadFromBytes(context: Context,bytes: ByteArray): PAGFile? {
        return try {
            PAGFile.Load(bytes)
        } catch (e: Exception) {
            android.util.Log.e("PagAnimationLoader", "Failed to load PAG from bytes", e)
            null
        }
    }

    override fun loadFromInputStream(context: Context,inputStream: InputStream): PAGFile? {
        return try {
            val bytes = inputStream.readBytes()
            loadFromBytes(context,bytes)
        } catch (e: Exception) {
            android.util.Log.e("PagAnimationLoader", "Failed to load PAG from input stream", e)
            null
        }
    }

    override fun loadFromUrl(
        context: Context,
        url: String,
        downloader: AnimationDownloader
    ): PAGFile? {
        return try {
            // 下载文件
            val tempFile = downloader.download(context, url)
            // 从临时文件加载
            val result = loadFromFile(context,tempFile)
            result
        } catch (e: Exception) {
            android.util.Log.e("PagAnimationLoader", "Failed to load PAG from URL: $url", e)
            null
        }
    }

    override fun loadFromAssetPath(context: Context, assetPath: String): PAGFile? {
        return try {
            PAGFile.Load(context.assets, assetPath)
        } catch (e: Exception) {
            android.util.Log.e(
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
