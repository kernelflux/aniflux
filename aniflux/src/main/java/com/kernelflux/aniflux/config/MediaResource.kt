package com.kernelflux.aniflux.config

import android.graphics.drawable.Drawable
import android.net.Uri
import com.airbnb.lottie.LottieComposition
import com.opensource.svgaplayer.SVGAVideoEntity
import org.libpag.PAGFile
import java.io.File

/**
 * @author: kerneflux
 * @date: 2025/9/21
 */

//1. 输入资源类型
sealed class LoadResource {
    data class UrlResource(val url: String) : LoadResource()
    data class DrawableResource(val resourceId: Int) : LoadResource()
    data class FileResource(val file: File) : LoadResource()
    data class UriResource(val uri: Uri) : LoadResource()
}

// 2. 输出资源类型
sealed class LoadResult {
    data class Success(val resource: LoadedResource) : LoadResult()
    data class Failure(val error: String) : LoadResult()
}

sealed class LoadedResource {
    data class DrawableResource(val drawable: Drawable) : LoadedResource()
    data class PAGResource(val pagFile: PAGFile) : LoadedResource()
    data class SVGAResource(val sVGAEntity: SVGAVideoEntity) : LoadedResource()
    data class LottieResource(val composition: LottieComposition) : LoadedResource()
}