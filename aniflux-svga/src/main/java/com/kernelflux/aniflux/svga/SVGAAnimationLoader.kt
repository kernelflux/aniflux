package com.kernelflux.aniflux.svga

import android.content.Context
import com.kernelflux.aniflux.annotation.AutoRegisterLoader
import com.kernelflux.aniflux.load.AnimationDownloader
import com.kernelflux.aniflux.load.AnimationLoader
import com.kernelflux.aniflux.log.AniFluxLog
import com.kernelflux.aniflux.log.AniFluxLogCategory
import com.kernelflux.aniflux.util.AnimationTypeDetector
import com.kernelflux.svga.SVGADrawable
import com.kernelflux.svga.SVGAParser
import com.kernelflux.svga.SVGAVideoEntity
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * SVGA animation loader - references SVGAPlayer-Android library's loading approach
 * Supports loading SVGA from file path, file, resource ID, byte array, input stream, network URL
 */
@AutoRegisterLoader(animationType = "SVGA")
class SVGAAnimationLoader : AnimationLoader<SVGADrawable> {

    private var context: Context? = null

    fun setContext(context: Context) {
        this.context = context
    }

    override fun loadFromPath(context: Context, path: String): SVGADrawable? {
        // Set context for use in private methods
        setContext(context)
        return try {
            val videoEntity = loadSvgaVideoEntityFromPath(path)
            videoEntity?.let { createSvgaDrawable(it) }
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to load SVGA from path: $path", e)
            null
        }
    }

    override fun loadFromFile(context: Context, file: File): SVGADrawable? {
        // Set context for use in private methods
        setContext(context)
        return try {
            val videoEntity = loadSvgaVideoEntityFromFile(file)
            videoEntity?.let { createSvgaDrawable(it) }
        } catch (e: Exception) {
            AniFluxLog.e(
                AniFluxLogCategory.LOADER,
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
            AniFluxLog.e(
                AniFluxLogCategory.LOADER,
                "Failed to load SVGA from resource: $resourceId",
                e
            )
            null
        }
    }

    override fun loadFromBytes(context: Context, bytes: ByteArray): SVGADrawable? {
        // Set context for use in private methods
        setContext(context)
        return try {
            val videoEntity = loadSvgaVideoEntityFromBytes(bytes)
            videoEntity?.let { createSvgaDrawable(it) }
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to load SVGA from bytes", e)
            null
        }
    }

    override fun loadFromInputStream(context: Context, inputStream: InputStream): SVGADrawable? {
        // Set context for use in private methods
        setContext(context)
        return try {
            val videoEntity = loadSvgaVideoEntityFromInputStream(inputStream)
            videoEntity?.let { createSvgaDrawable(it) }
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to load SVGA from input stream", e)
            null
        }
    }

    override fun loadFromUrl(
        context: Context,
        url: String,
        downloader: AnimationDownloader
    ): SVGADrawable? {
        return try {
            // Download file
            val tempFile = downloader.download(context, url)
            // Load from temporary file
            val result = loadFromFile(context, tempFile)
            result
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to load SVGA from URL: $url", e)
            null
        }
    }

    override fun loadFromAssetPath(context: Context, assetPath: String): SVGADrawable? {
        return try {
            val videoEntity = loadSvgaVideoEntityFromAssetPath(context, assetPath)
            videoEntity?.let { createSvgaDrawable(it) }
        } catch (e: Exception) {
            AniFluxLog.e(
                AniFluxLogCategory.LOADER,
                "Failed to load SVGA from asset path: $assetPath",
                e
            )
            null
        }
    }

    /**
     * Load SVGAVideoEntity from Asset path
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
            AniFluxLog.e(
                AniFluxLogCategory.LOADER,
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
     * Load SVGAVideoEntity from file path
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
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Interrupted while loading SVGA from path", e)
            null
        }
    }

    /**
     * Load SVGAVideoEntity from file
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
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Interrupted while loading SVGA from file", e)
            null
        }
    }

    /**
     * Load SVGAVideoEntity from resource ID
     */
    private fun loadSvgaVideoEntityFromResource(
        context: Context,
        resourceId: Int
    ): SVGAVideoEntity? {
        val parser = SVGAParser(context)
        val latch = CountDownLatch(1)
        var result: SVGAVideoEntity? = null

        // Read resource content to byte array first to avoid stream closing issues
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
            AniFluxLog.e(
                AniFluxLogCategory.LOADER,
                "Interrupted while loading SVGA from resource",
                e
            )
            null
        }
    }

    /**
     * Load SVGAVideoEntity from byte array
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
            AniFluxLog.e(
                AniFluxLogCategory.LOADER,
                "Interrupted while loading SVGA from bytes",
                e
            )
            null
        }
    }

    /**
     * Load SVGAVideoEntity from input stream
     */
    private fun loadSvgaVideoEntityFromInputStream(inputStream: InputStream): SVGAVideoEntity? {
        val context = this.context ?: return null
        val parser = SVGAParser(context)
        val latch = CountDownLatch(1)
        var result: SVGAVideoEntity? = null

        // Read input stream content to byte array first to avoid stream closing issues
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
            AniFluxLog.e(
                AniFluxLogCategory.LOADER,
                "Interrupted while loading SVGA from input stream",
                e
            )
            null
        }
    }

    /**
     * Create SVGADrawable
     */
    private fun createSvgaDrawable(videoEntity: SVGAVideoEntity): SVGADrawable {
        return SVGADrawable(videoEntity)
    }
}

