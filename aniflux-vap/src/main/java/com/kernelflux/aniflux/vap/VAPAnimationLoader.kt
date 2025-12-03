package com.kernelflux.aniflux.vap

import android.content.Context
import com.kernelflux.aniflux.annotation.AutoRegisterLoader
import com.kernelflux.aniflux.load.AnimationDownloader
import com.kernelflux.aniflux.load.AnimationLoader
import com.kernelflux.aniflux.util.AnimationTypeDetector
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * VAP动画加载器 - 参考vap库的加载方式
 * 支持从文件路径、文件、资源ID、字节数组、输入流、网络URL加载VAP
 * VAP 库基于 File 和 IFileContainer，所以大部分来源需要先转换为 File
 */
@AutoRegisterLoader(animationType = "VAP")
class VAPAnimationLoader : AnimationLoader<File> {

    override fun loadFromPath(context: Context, path: String): File? {
        return try {
            val file = File(path)
            if (file.exists() && file.isFile) {
                file
            } else {
                android.util.Log.e("VAPAnimationLoader", "File does not exist or is not a file: $path")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("VAPAnimationLoader", "Failed to load VAP from path: $path", e)
            null
        }
    }

    override fun loadFromFile(context: Context, file: File): File? {
        return try {
            if (file.exists() && file.isFile) {
                file
            } else {
                android.util.Log.e("VAPAnimationLoader", "File does not exist or is not a file: ${file.absolutePath}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("VAPAnimationLoader", "Failed to load VAP from file: ${file.absolutePath}", e)
            null
        }
    }

    override fun loadFromResource(context: Context, resourceId: Int): File? {
        return try {
            // 从资源读取到临时文件
            val inputStream = context.resources.openRawResource(resourceId)
            val tempFile = createTempFileFromInputStream(context, inputStream, "vap_resource_$resourceId")
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            android.util.Log.e("VAPAnimationLoader", "Failed to load VAP from resource: $resourceId", e)
            null
        }
    }

    override fun loadFromBytes(context: Context, bytes: ByteArray): File? {
        return try {
            val inputStream = bytes.inputStream()
            createTempFileFromInputStream(context, inputStream, "vap_bytes_${bytes.hashCode()}")
        } catch (e: Exception) {
            android.util.Log.e("VAPAnimationLoader", "Failed to load VAP from bytes", e)
            null
        }
    }

    override fun loadFromInputStream(context: Context, inputStream: InputStream): File? {
        return try {
            createTempFileFromInputStream(context, inputStream, "vap_stream_${inputStream.hashCode()}")
        } catch (e: Exception) {
            android.util.Log.e("VAPAnimationLoader", "Failed to load VAP from input stream", e)
            null
        }
    }

    override fun loadFromUrl(context: Context, url: String, downloader: AnimationDownloader): File? {
        return try {
            // 下载文件
            val tempFile = downloader.download(context, url)
            // 验证文件存在
            if (tempFile.exists() && tempFile.isFile) {
                tempFile
            } else {
                android.util.Log.e("VAPAnimationLoader", "Downloaded file is invalid: $url")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("VAPAnimationLoader", "Failed to load VAP from URL: $url", e)
            null
        }
    }

    override fun loadFromAssetPath(context: Context, assetPath: String): File? {
        return try {
            // 从 Asset 读取到临时文件
            val inputStream = context.assets.open(assetPath)
            val tempFile = createTempFileFromInputStream(context, inputStream, "vap_asset_${assetPath.replace("/", "_")}")
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            android.util.Log.e("VAPAnimationLoader", "Failed to load VAP from asset path: $assetPath", e)
            null
        }
    }

    override fun getAnimationType(): AnimationTypeDetector.AnimationType {
        return AnimationTypeDetector.AnimationType.VAP
    }

    /**
     * 从输入流创建临时文件
     */
    private fun createTempFileFromInputStream(
        context: Context,
        inputStream: InputStream,
        prefix: String
    ): File? {
        return try {
            // 创建临时文件
            val tempFile = File.createTempFile(prefix, ".vap", context.cacheDir)
            tempFile.deleteOnExit() // 应用退出时删除临时文件

            // 将输入流内容写入临时文件
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
                outputStream.flush()
            }

            tempFile
        } catch (e: Exception) {
            android.util.Log.e("VAPAnimationLoader", "Failed to create temp file from input stream", e)
            null
        }
    }
}

