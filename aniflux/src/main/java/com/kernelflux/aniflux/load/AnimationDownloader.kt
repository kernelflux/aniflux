package com.kernelflux.aniflux.load

import android.content.Context
import java.io.File

/**
 * 动画资源下载器接口
 * 用于下载网络动画资源
 */
interface AnimationDownloader {

    fun getDownloadCacheDir(): String = "aniflux_downloader"

    /**
     * 下载动画资源
     * @param url 资源URL
     * @return 输入流
     * @throws Exception 下载失败时抛出异常
     */
    @Throws(Exception::class)
    fun download(context: Context, url: String): File

    /**
     * 下载动画资源到字节数组
     * @param url 资源URL
     * @return 字节数组
     * @throws Exception 下载失败时抛出异常
     */
    @Throws(Exception::class)
    fun downloadToBytes(context: Context, url: String): ByteArray
}
