package com.kernelflux.aniflux.load

import android.content.Context
import com.kernelflux.aniflux.util.AnimationTypeDetector
import com.opensource.svgaplayer.SVGADrawable
import com.opensource.svgaplayer.SVGAParser
import com.opensource.svgaplayer.SVGAVideoEntity
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * SVGA动画加载器 - 参考SVGAPlayer-Android库的加载方式
 * 支持从文件路径、文件、资源ID、字节数组、输入流、网络URL加载SVGA
 */
class SVGAAnimationLoader : AnimationLoader<SVGADrawable> {

    private var context: Context? = null

    fun setContext(context: Context) {
        this.context = context
    }

    override fun loadFromPath(context: Context, path: String): SVGADrawable? {
        return try {
            val videoEntity = loadSvgaVideoEntityFromPath(path)
            videoEntity?.let { createSvgaDrawable(it) }
        } catch (e: Exception) {
            android.util.Log.e("SvgaAnimationLoader", "Failed to load SVGA from path: $path", e)
            null
        }
    }

    override fun loadFromFile(context: Context, file: File): SVGADrawable? {
        return try {
            val videoEntity = loadSvgaVideoEntityFromFile(file)
            videoEntity?.let { createSvgaDrawable(it) }
        } catch (e: Exception) {
            android.util.Log.e(
                "SvgaAnimationLoader",
                "Failed to load SVGA from file: ${file.absolutePath}",
                e
            )
            null
        }
    }

    override fun loadFromResource(context: Context, resourceId: Int): SVGADrawable? {
        return try {
            val videoEntity = loadSvgaVideoEntityFromResource(context, resourceId)
            videoEntity?.let { createSvgaDrawable(it) }
        } catch (e: Exception) {
            android.util.Log.e(
                "SvgaAnimationLoader",
                "Failed to load SVGA from resource: $resourceId",
                e
            )
            null
        }
    }

    override fun loadFromBytes(context: Context, bytes: ByteArray): SVGADrawable? {
        return try {
            val videoEntity = loadSvgaVideoEntityFromBytes(bytes)
            videoEntity?.let { createSvgaDrawable(it) }
        } catch (e: Exception) {
            android.util.Log.e("SvgaAnimationLoader", "Failed to load SVGA from bytes", e)
            null
        }
    }

    override fun loadFromInputStream(context: Context, inputStream: InputStream): SVGADrawable? {
        return try {
            val videoEntity = loadSvgaVideoEntityFromInputStream(inputStream)
            videoEntity?.let { createSvgaDrawable(it) }
        } catch (e: Exception) {
            android.util.Log.e("SvgaAnimationLoader", "Failed to load SVGA from input stream", e)
            null
        }
    }

    override fun loadFromUrl(
        context: Context,
        url: String,
        downloader: AnimationDownloader
    ): SVGADrawable? {
        return try {
            // 下载文件
            val tempFile = downloader.download(context, url)
            // 从临时文件加载
            val result = loadFromFile(context, tempFile)
            result
        } catch (e: Exception) {
            android.util.Log.e("SvgaAnimationLoader", "Failed to load SVGA from URL: $url", e)
            null
        }
    }

    override fun loadFromAssetPath(context: Context, assetPath: String): SVGADrawable? {
        return try {
            val videoEntity = loadSvgaVideoEntityFromAssetPath(context, assetPath)
            videoEntity?.let { createSvgaDrawable(it) }
        } catch (e: Exception) {
            android.util.Log.e(
                "SvgaAnimationLoader",
                "Failed to load SVGA from asset path: $assetPath",
                e
            )
            null
        }
    }

    /**
     * 从Asset路径加载SVGAVideoEntity
     */
    private fun loadSvgaVideoEntityFromAssetPath(
        context: Context,
        assetPath: String
    ): SVGAVideoEntity? {
        val parser = SVGAParser(context)
        val latch = CountDownLatch(1)
        var result: SVGAVideoEntity? = null

        parser.decodeFromAssets(assetPath, object : SVGAParser.ParseCompletion {
            override fun onComplete(videoItem: SVGAVideoEntity) {
                result = videoItem
                latch.countDown()
            }

            override fun onError() {
                latch.countDown()
            }
        })

        return try {
            latch.await(10, TimeUnit.SECONDS)
            result
        } catch (e: InterruptedException) {
            android.util.Log.e(
                "SvgaAnimationLoader",
                "Interrupted while loading SVGA from asset path",
                e
            )
            null
        }
    }

    override fun getAnimationType(): AnimationTypeDetector.AnimationType {
        return AnimationTypeDetector.AnimationType.SVGA
    }

    /**
     * 从文件路径加载SVGAVideoEntity
     */
    private fun loadSvgaVideoEntityFromPath(path: String): SVGAVideoEntity? {
        val context = this.context ?: return null
        val parser = SVGAParser(context)
        val latch = CountDownLatch(1)
        var result: SVGAVideoEntity? = null

        parser.decodeFromAssets(path, object : SVGAParser.ParseCompletion {
            override fun onComplete(videoItem: SVGAVideoEntity) {
                result = videoItem
                latch.countDown()
            }

            override fun onError() {
                latch.countDown()
            }
        })

        return try {
            latch.await(10, TimeUnit.SECONDS)
            result
        } catch (e: InterruptedException) {
            android.util.Log.e("SvgaAnimationLoader", "Interrupted while loading SVGA from path", e)
            null
        }
    }

    /**
     * 从文件加载SVGAVideoEntity
     */
    private fun loadSvgaVideoEntityFromFile(file: File): SVGAVideoEntity? {
        val context = this.context ?: return null
        val parser = SVGAParser(context)
        val latch = CountDownLatch(1)
        var result: SVGAVideoEntity? = null


        val bytes = file.readBytes()
        val cacheKey = "svga-from-file-${file.hashCode()}"
        parser.decodeFromInputStream(
            ByteArrayInputStream(bytes),
            cacheKey,
            object : SVGAParser.ParseCompletion {
                override fun onComplete(videoItem: SVGAVideoEntity) {
                    result = videoItem
                    latch.countDown()
                }

                override fun onError() {
                    latch.countDown()
                }
            }
        )
        return try {
            latch.await(10, TimeUnit.SECONDS)
            result
        } catch (e: InterruptedException) {
            android.util.Log.e("SvgaAnimationLoader", "Interrupted while loading SVGA from file", e)
            null
        }
    }

    /**
     * 从资源ID加载SVGAVideoEntity
     */
    private fun loadSvgaVideoEntityFromResource(
        context: Context,
        resourceId: Int
    ): SVGAVideoEntity? {
        val parser = SVGAParser(context)
        val latch = CountDownLatch(1)
        var result: SVGAVideoEntity? = null


        // 先读取资源内容到字节数组，避免流关闭问题
        val bytes = context.resources.openRawResource(resourceId).readBytes()
        val cacheKey = "svga-from-file-${resourceId.hashCode()}"
        parser.decodeFromInputStream(
            ByteArrayInputStream(bytes),
            cacheKey,
            object : SVGAParser.ParseCompletion {
                override fun onComplete(videoItem: SVGAVideoEntity) {
                    result = videoItem
                    latch.countDown()
                }

                override fun onError() {
                    latch.countDown()
                }
            }
        )

        return try {
            latch.await(10, TimeUnit.SECONDS)
            result
        } catch (e: InterruptedException) {
            android.util.Log.e(
                "SvgaAnimationLoader",
                "Interrupted while loading SVGA from resource",
                e
            )
            null
        }
    }

    /**
     * 从字节数组加载SVGAVideoEntity
     */
    private fun loadSvgaVideoEntityFromBytes(bytes: ByteArray): SVGAVideoEntity? {
        val context = this.context ?: return null
        val parser = SVGAParser(context)
        val latch = CountDownLatch(1)
        var result: SVGAVideoEntity? = null

        val input = java.io.ByteArrayInputStream(bytes)
        val cacheKey = "svga-from-file-${bytes.hashCode()}"
        parser.decodeFromInputStream(
            input,
            cacheKey,
            object : SVGAParser.ParseCompletion {
                override fun onComplete(videoItem: SVGAVideoEntity) {
                    result = videoItem
                    latch.countDown()
                }

                override fun onError() {
                    latch.countDown()
                }
            }
        )

        return try {
            latch.await(10, TimeUnit.SECONDS)
            result
        } catch (e: InterruptedException) {
            android.util.Log.e(
                "SvgaAnimationLoader",
                "Interrupted while loading SVGA from bytes",
                e
            )
            null
        }
    }

    /**
     * 从输入流加载SVGAVideoEntity
     */
    private fun loadSvgaVideoEntityFromInputStream(inputStream: InputStream): SVGAVideoEntity? {
        val context = this.context ?: return null
        val parser = SVGAParser(context)
        val latch = CountDownLatch(1)
        var result: SVGAVideoEntity? = null

        // 先读取输入流内容到字节数组，避免流关闭问题
        val bytes = inputStream.readBytes()
        val cacheKey = "svga-from-file-${bytes.hashCode()}"
        parser.decodeFromInputStream(
            ByteArrayInputStream(bytes),
            cacheKey,
            object : SVGAParser.ParseCompletion {
                override fun onComplete(videoItem: SVGAVideoEntity) {
                    result = videoItem
                    latch.countDown()
                }

                override fun onError() {
                    latch.countDown()
                }
            }
        )

        return try {
            latch.await(10, TimeUnit.SECONDS)
            result
        } catch (e: InterruptedException) {
            android.util.Log.e(
                "SvgaAnimationLoader",
                "Interrupted while loading SVGA from input stream",
                e
            )
            null
        }
    }

    /**
     * 创建SVGADrawable
     */
    private fun createSvgaDrawable(videoEntity: SVGAVideoEntity): SVGADrawable {
        return SVGADrawable(videoEntity)
    }
}
