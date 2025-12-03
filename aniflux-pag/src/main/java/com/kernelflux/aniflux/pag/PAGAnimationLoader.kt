package com.kernelflux.aniflux.pag

import android.content.Context
import com.kernelflux.aniflux.annotation.AutoRegisterLoader
import com.kernelflux.aniflux.load.AnimationDownloader
import com.kernelflux.aniflux.load.AnimationLoader
import com.kernelflux.aniflux.util.AnimationTypeDetector
import com.kernelflux.pag.PAGFile
import java.io.File
import java.io.InputStream

/**
 * PAG动画加载器 - 参考libpag库的加载方式
 * 支持从文件路径、文件、资源ID、字节数组、输入流、网络URL加载PAG
 */
@AutoRegisterLoader(animationType = "PAG")
class PAGAnimationLoader : AnimationLoader<PAGFile> {

    override fun loadFromPath(context: Context, path: String): PAGFile? {
        return try {
            // ⚠️ 问题：PAGFile.Load(path) 会使用内部缓存，相同路径会共享底层 File 对象
            // ✅ 解决方案：读取文件为字节数组，使用 PAGFile.Load(byte[]) 创建独立实例
            val file = java.io.File(path)
            if (file.exists()) {
                val bytes = file.readBytes()
                PAGFile.Load(bytes)
            } else {
                // 如果文件不存在，尝试直接加载（可能是网络URL或其他路径）
                PAGFile.Load(path)
            }
        } catch (e: Exception) {
            android.util.Log.e("PagAnimationLoader", "Failed to load PAG from path: $path", e)
            null
        }
    }

    override fun loadFromFile(context: Context, file: File): PAGFile? {
        return try {
            // ⚠️ 问题：PAGFile.Load(filePath) 会使用内部缓存，相同路径会共享底层 File 对象
            // 这导致多个 PAGView 使用同一个 PAGFile 时，只有最后一个能显示动画
            // ✅ 解决方案：使用 PAGFile.Load(byte[]) 为每个请求创建独立的 PAGFile 实例
            val bytes = file.readBytes()
            PAGFile.Load(bytes)
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
            loadFromBytes(context, bytes)
        } catch (e: Exception) {
            android.util.Log.e(
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
            android.util.Log.e("PagAnimationLoader", "Failed to load PAG from bytes", e)
            null
        }
    }

    override fun loadFromInputStream(context: Context, inputStream: InputStream): PAGFile? {
        return try {
            val bytes = inputStream.readBytes()
            loadFromBytes(context, bytes)
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
            val result = loadFromFile(context, tempFile)
            result
        } catch (e: Exception) {
            android.util.Log.e("PagAnimationLoader", "Failed to load PAG from URL: $url", e)
            null
        }
    }

    override fun loadFromAssetPath(context: Context, assetPath: String): PAGFile? {
        return try {
            // ⚠️ 问题：PAGFile.Load(assets, path) 可能也有缓存机制
            // ✅ 解决方案：读取 asset 为字节数组，使用 PAGFile.Load(byte[]) 创建独立实例
            val inputStream = context.assets.open(assetPath)
            val bytes = inputStream.readBytes()
            inputStream.close()
            PAGFile.Load(bytes)
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

