package com.kernelflux.aniflux.placeholder

/**
 * 占位图替换配置
 *
 * @param placeholderKey 占位符key（如 "user_1", "avatar", "logo"）
 *                       对应动画文件中的占位符名称
 * @param imageSource 图片源（业务方定义的类型）
 *                    可以是String（URL）、File、Uri、ResourceId等
 *                    由业务方的PlaceholderImageLoader负责解析和加载
 * @param scaleType 缩放类型（可选，某些格式可能不支持）
 *
 * @author: kerneflux
 * @date: 2025/11/27
 */
data class PlaceholderReplacement(
    val placeholderKey: String,
    val imageSource: Any,
    val scaleType: ImageScaleType = ImageScaleType.FIT_CENTER
)

/**
 * 图片缩放类型
 */
enum class ImageScaleType {
    /**
     * 居中缩放，保持宽高比，可能留白
     */
    FIT_CENTER,

    /**
     * 填充整个区域，保持宽高比，可能裁剪
     */
    CENTER_CROP,

    /**
     * 拉伸填充，不保持宽高比
     */
    FIT_XY,

    /**
     * 居中显示，不缩放
     */
    CENTER
}

/**
 * 占位图替换映射表
 * 支持链式调用
 */
class PlaceholderReplacementMap {
    private val replacements = mutableMapOf<String, PlaceholderReplacement>()

    /**
     * 添加占位图替换配置
     *
     * @param key 占位符key
     * @param source 图片源
     * @return this，支持链式调用
     */
    fun add(key: String, source: Any): PlaceholderReplacementMap {
        replacements[key] = PlaceholderReplacement(key, source)
        return this
    }

    /**
     * 添加占位图替换配置（完整配置）
     *
     * @param replacement 占位图替换配置
     * @return this，支持链式调用
     */
    fun add(replacement: PlaceholderReplacement): PlaceholderReplacementMap {
        replacements[replacement.placeholderKey] = replacement
        return this
    }

    /**
     * 批量添加
     *
     * @param map 占位符key到图片源的映射
     * @return this，支持链式调用
     */
    fun addAll(map: Map<String, Any>): PlaceholderReplacementMap {
        map.forEach { (key, source) ->
            replacements[key] = PlaceholderReplacement(key, source)
        }
        return this
    }

    /**
     * 获取指定key的替换配置
     */
    fun get(key: String): PlaceholderReplacement? = replacements[key]

    /**
     * 获取所有替换配置
     */
    fun getAll(): Map<String, PlaceholderReplacement> = replacements.toMap()

    /**
     * 是否为空
     */
    fun isEmpty(): Boolean = replacements.isEmpty()

    /**
     * 是否包含指定key
     */
    fun containsKey(key: String): Boolean = replacements.containsKey(key)

    /**
     * 清空所有配置
     */
    fun clear() {
        replacements.clear()
    }
}

