package com.kernelflux.aniflux.loader

import android.net.Uri
import com.kernelflux.aniflux.config.LoadOptions
import com.kernelflux.aniflux.config.LoadResource
import com.kernelflux.aniflux.config.MediaTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.libpag.PAGFile
import java.io.File

/**
 * @author: kerneflux
 * @date: 2025/9/21
 *  PAG媒体加载器
 */
class PAGMediaLoader : MediaLoader {

    override fun canHandle(source: LoadResource): Boolean {
        return when (source) {
            is LoadResource.UrlResource -> source.url.endsWith(".pag", ignoreCase = true)
            is LoadResource.FileResource -> source.file.extension.equals("pag", ignoreCase = true)
            is LoadResource.DrawableResource -> {
                // 无法确定资源类型，返回false
                false
            }

            is LoadResource.UriResource -> {
                // 尝试从URI中获取文件扩展名
                val uriString = source.uri.toString()
                uriString.endsWith(".pag", ignoreCase = true) ||
                        uriString.contains("pag", ignoreCase = true)
            }
        }
    }

    override suspend fun load(
        source: LoadResource,
        options: LoadOptions,
        target: MediaTarget
    ): Any = withContext(Dispatchers.IO) {
        when (source) {
            is LoadResource.UrlResource -> {
                loadFromUrl(source.url, target)
            }

            is LoadResource.FileResource -> {
                loadFromFile(source.file)
            }

            is LoadResource.DrawableResource -> {
                // 由于canHandle返回false，这里不应该被调用
                throw IllegalStateException("DrawableResource not supported for PAG")
            }

            is LoadResource.UriResource -> {
                loadFromUri(source.uri, target)
            }
        }
    }

    private suspend fun loadFromUrl(url: String, target: MediaTarget): PAGFile {
        return withContext(Dispatchers.IO) {
            when {
                url.startsWith("file:///android_asset/") -> {
                    // 处理 file:///android_asset/xxx.pag
                    loadFromAssetUrl(url, target)
                }

                url.startsWith("asset://") -> {
                    // 处理 asset://xxx.pag
                    loadFromAssetUrl(url, target)
                }

                url.startsWith("http://") || url.startsWith("https://") -> {
                    // 处理远程URL - 应该已经下载到本地
                    throw IllegalStateException("Remote URL should be downloaded first")
                }

                else -> {
                    // 其他情况，尝试直接加载
                    PAGFile.Load(url)
                }
            }
        }
    }

    private suspend fun loadFromAssetUrl(url: String, target: MediaTarget): PAGFile {
        return withContext(Dispatchers.IO) {
            val context = target.getContext()
            val assetPath = when {
                url.startsWith("file:///android_asset/") -> {
                    // file:///android_asset/xxx.pag -> xxx.pag
                    url.removePrefix("file:///android_asset/")
                }

                url.startsWith("asset://") -> {
                    // asset://xxx.pag -> xxx.pag
                    url.removePrefix("asset://")
                }

                else -> {
                    throw IllegalArgumentException("Invalid asset URL: $url")
                }
            }
            // 使用PAGFile.Load(AssetManager, String)方法
            PAGFile.Load(context.assets, assetPath)
        }
    }

    private suspend fun loadFromFile(file: File): PAGFile {
        return withContext(Dispatchers.IO) {
            PAGFile.Load(file.absolutePath)
        }
    }

    private suspend fun loadFromUri(uri: Uri, target: MediaTarget): PAGFile {
        return withContext(Dispatchers.IO) {
            val uriString = uri.toString()

            when {
                uriString.startsWith("file:///android_asset/") -> {
                    loadFromAssetUrl(uriString, target)
                }

                uriString.startsWith("asset://") -> {
                    loadFromAssetUrl(uriString, target)
                }

                uriString.startsWith("file://") -> {
                    // 普通文件URI
                    val filePath = uri.path
                    PAGFile.Load(filePath)
                }

                uriString.startsWith("content://") -> {
                    // Content URI，需要特殊处理
                    loadFromContentUri(uri, target)
                }

                uriString.startsWith("http://") || uriString.startsWith("https://") -> {
                    // 远程URL
                    throw IllegalStateException("Remote URL should be downloaded first")
                }

                else -> {
                    // 其他URI类型，尝试直接加载
                    PAGFile.Load(uriString)
                }
            }
        }
    }

    private suspend fun loadFromContentUri(uri: Uri, target: MediaTarget): PAGFile {
        return withContext(Dispatchers.IO) {
            val context = target.getContext()
            val tempFile = File(context.cacheDir, "temp_pag_${System.currentTimeMillis()}.pag")

            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    tempFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                val pagFile = PAGFile.Load(tempFile.absolutePath)
                tempFile.delete()
                pagFile
            } catch (e: Exception) {
                tempFile.delete()
                throw e
            }
        }
    }

}