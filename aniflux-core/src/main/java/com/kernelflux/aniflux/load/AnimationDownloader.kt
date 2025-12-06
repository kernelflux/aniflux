package com.kernelflux.aniflux.load

import android.content.Context
import java.io.File

/**
 * Animation resource downloader interface
 * Used for downloading network animation resources
 */
interface AnimationDownloader {

    fun getDownloadCacheDir(): String = "aniflux_downloader"

    /**
     * Download animation resource
     * @param url Resource URL
     * @return File
     * @throws Exception Throws exception when download fails
     */
    @Throws(Exception::class)
    fun download(context: Context, url: String): File

    /**
     * Download animation resource to byte array
     * @param url Resource URL
     * @return Byte array
     * @throws Exception Throws exception when download fails
     */
    @Throws(Exception::class)
    fun downloadToBytes(context: Context, url: String): ByteArray
}
