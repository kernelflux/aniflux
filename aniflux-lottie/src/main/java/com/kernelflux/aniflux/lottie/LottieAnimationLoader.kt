package com.kernelflux.aniflux.lottie

import android.content.Context
import com.kernelflux.aniflux.log.AniFluxLog
import com.kernelflux.aniflux.log.AniFluxLogCategory
import com.kernelflux.aniflux.annotation.AutoRegisterLoader
import com.kernelflux.aniflux.load.AnimationDownloader
import com.kernelflux.aniflux.load.AnimationLoader
import com.kernelflux.aniflux.util.AnimationTypeDetector
import com.kernelflux.lottie.LottieComposition
import com.kernelflux.lottie.LottieCompositionFactory
import com.kernelflux.lottie.LottieDrawable
import java.io.File
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * Lottie animation loader - references lottie-android library's loading approach
 * Supports loading Lottie from file path, file, resource ID, byte array, input stream, network URL
 */
@AutoRegisterLoader(animationType = "LOTTIE")
class LottieAnimationLoader : AnimationLoader<LottieDrawable> {

    override fun loadFromPath(context: Context, path: String): LottieDrawable? {
        return try {
            val composition = loadLottieCompositionFromPath(context, path)
            composition?.let { createLottieDrawable(it) }
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to load Lottie from path: $path", e)
            null
        }
    }

    override fun loadFromFile(context: Context, file: File): LottieDrawable? {
        return try {
            val composition = loadLottieCompositionFromFile(file)
            composition?.let { createLottieDrawable(it) }
        } catch (e: Exception) {
            AniFluxLog.e(
                AniFluxLogCategory.LOADER,
                "Failed to load Lottie from file: ${file.absolutePath}",
                e
            )
            null
        }
    }

    override fun loadFromResource(context: Context, resourceId: Int): LottieDrawable? {
        return try {
            val composition = loadLottieCompositionFromResource(context, resourceId)
            composition?.let { createLottieDrawable(it) }
        } catch (e: Exception) {
            AniFluxLog.e(
                AniFluxLogCategory.LOADER,
                "Failed to load Lottie from resource: $resourceId",
                e
            )
            null
        }
    }

    override fun loadFromBytes(context: Context, bytes: ByteArray): LottieDrawable? {
        return try {
            val composition = loadLottieCompositionFromBytes(bytes)
            composition?.let { createLottieDrawable(it) }
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to load Lottie from bytes", e)
            null
        }
    }

    override fun loadFromInputStream(context: Context, inputStream: InputStream): LottieDrawable? {
        return try {
            val composition = loadLottieCompositionFromInputStream(inputStream)
            composition?.let { createLottieDrawable(it) }
        } catch (e: Exception) {
            AniFluxLog.e(
                AniFluxLogCategory.LOADER,
                "Failed to load Lottie from input stream",
                e
            )
            null
        }
    }

    override fun loadFromUrl(
        context: Context,
        url: String,
        downloader: AnimationDownloader
    ): LottieDrawable? {
        return try {
            // Download file
            val tempFile = downloader.download(context, url)
            // Load from temporary file
            val result = loadFromFile(context, tempFile)
            result
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.LOADER, "Failed to load Lottie from URL: $url", e)
            null
        }
    }

    override fun loadFromAssetPath(context: Context, assetPath: String): LottieDrawable? {
        return try {
            val composition = loadLottieCompositionFromAssetPath(context, assetPath)
            composition?.let { createLottieDrawable(it) }
        } catch (e: Exception) {
            AniFluxLog.e(
                AniFluxLogCategory.LOADER,
                "Failed to load Lottie from asset path: $assetPath",
                e
            )
            null
        }
    }

    /**
     * Load LottieComposition from Asset path
     */
    private fun loadLottieCompositionFromAssetPath(
        context: Context,
        assetPath: String
    ): LottieComposition? {
        val latch = CountDownLatch(1)
        var result: LottieComposition? = null

        LottieCompositionFactory.fromAsset(context, assetPath)
            .addListener { composition ->
                result = composition
                latch.countDown()
            }
            .addFailureListener {
                latch.countDown()
            }

        return try {
            latch.await(10, TimeUnit.SECONDS)
            result
        } catch (e: InterruptedException) {
            AniFluxLog.e(
                AniFluxLogCategory.LOADER,
                "Interrupted while loading Lottie from asset path",
                e
            )
            null
        }
    }

    override fun getAnimationType(): AnimationTypeDetector.AnimationType {
        return AnimationTypeDetector.AnimationType.LOTTIE
    }

    /**
     * Load LottieComposition from file path
     */
    private fun loadLottieCompositionFromPath(context: Context, path: String): LottieComposition? {
        val latch = CountDownLatch(1)
        var result: LottieComposition? = null

        LottieCompositionFactory.fromAsset(context, path)
            .addListener { composition ->
                result = composition
                latch.countDown()
            }
            .addFailureListener {
                latch.countDown()
            }

        return try {
            latch.await(10, TimeUnit.SECONDS)
            result
        } catch (e: InterruptedException) {
            AniFluxLog.e(
                AniFluxLogCategory.LOADER,
                "Interrupted while loading Lottie from path",
                e
            )
            null
        }
    }

    /**
     * Load LottieComposition from file
     */
    private fun loadLottieCompositionFromFile(file: File): LottieComposition? {
        val latch = CountDownLatch(1)
        var result: LottieComposition? = null

        // Read file content to byte array first to avoid stream closing issues
        val bytes = file.readBytes()
        
        // Determine if it's JSON or ZIP format based on file extension
        val fileName = file.name.lowercase()
        if (fileName.endsWith(".zip") || fileName.endsWith(".lottie")) {
            // ZIP format Lottie file
            val zis = ZipInputStream(bytes.inputStream())
            LottieCompositionFactory.fromZipStream(zis, null)
                .addListener { comp ->
                    result = comp
                    latch.countDown()
                }
                .addFailureListener { e -> 
                    e.printStackTrace()
                    latch.countDown()
                }
        } else {
            // JSON format Lottie file
            val jsonString = String(bytes)
            LottieCompositionFactory.fromJsonString(jsonString, null)
                .addListener { comp ->
                    result = comp
                    latch.countDown()
                }
                .addFailureListener { e -> 
                    e.printStackTrace()
                    latch.countDown()
                }
        }

        return try {
            latch.await(10, TimeUnit.SECONDS)
            result
        } catch (e: InterruptedException) {
            AniFluxLog.e(
                AniFluxLogCategory.LOADER,
                "Interrupted while loading Lottie from file",
                e
            )
            null
        }
    }

    /**
     * Load LottieComposition from resource ID
     */
    private fun loadLottieCompositionFromResource(
        context: Context,
        resourceId: Int
    ): LottieComposition? {
        val latch = CountDownLatch(1)
        var result: LottieComposition? = null

        LottieCompositionFactory.fromRawRes(context, resourceId)
            .addListener { composition ->
                result = composition
                latch.countDown()
            }
            .addFailureListener {
                latch.countDown()
            }

        return try {
            latch.await(10, TimeUnit.SECONDS)
            result
        } catch (e: InterruptedException) {
            AniFluxLog.e(
                AniFluxLogCategory.LOADER,
                "Interrupted while loading Lottie from resource",
                e
            )
            null
        }
    }

    /**
     * Load LottieComposition from byte array
     */
    private fun loadLottieCompositionFromBytes(bytes: ByteArray): LottieComposition? {
        val latch = CountDownLatch(1)
        var result: LottieComposition? = null

        LottieCompositionFactory.fromJsonString(String(bytes), null)
            .addListener { composition ->
                result = composition
                latch.countDown()
            }
            .addFailureListener {
                latch.countDown()
            }

        return try {
            latch.await(10, TimeUnit.SECONDS)
            result
        } catch (e: InterruptedException) {
            AniFluxLog.e(
                AniFluxLogCategory.LOADER,
                "Interrupted while loading Lottie from bytes",
                e
            )
            null
        }
    }

    /**
     * Load LottieComposition from input stream
     */
    private fun loadLottieCompositionFromInputStream(inputStream: InputStream): LottieComposition? {
        val latch = CountDownLatch(1)
        var result: LottieComposition? = null

        // Read input stream content to byte array first to avoid stream closing issues
        val bytes = inputStream.readBytes()
        val jsonInputStream = bytes.inputStream()

        LottieCompositionFactory.fromJsonInputStream(jsonInputStream, null)
            .addListener { composition ->
                result = composition
                latch.countDown()
            }
            .addFailureListener {
                latch.countDown()
            }

        return try {
            latch.await(10, TimeUnit.SECONDS)
            result
        } catch (e: InterruptedException) {
            AniFluxLog.e(
                AniFluxLogCategory.LOADER,
                "Interrupted while loading Lottie from input stream",
                e
            )
            null
        }
    }

    /**
     * Create LottieDrawable
     */
    private fun createLottieDrawable(composition: LottieComposition): LottieDrawable {
        val drawable = LottieDrawable()
        drawable.setComposition(composition)
        return drawable
    }
}

