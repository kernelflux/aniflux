package com.kernelflux.aniflux.placeholder

/**
 * Placeholder replacement configuration
 *
 * @param placeholderKey Placeholder key (e.g., "user_1", "avatar", "logo")
 *                       Corresponds to placeholder name in animation file
 * @param imageSource Image source (business-defined type)
 *                    Can be String (URL), File, Uri, ResourceId, etc.
 *                    Parsed and loaded by business's PlaceholderImageLoader
 * @param scaleType Scale type (optional, some formats may not support)
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
 * Image scale type
 */
enum class ImageScaleType {
    /**
     * Center scale, maintain aspect ratio, may have padding
     */
    FIT_CENTER,

    /**
     * Fill entire area, maintain aspect ratio, may crop
     */
    CENTER_CROP,

    /**
     * Stretch fill, don't maintain aspect ratio
     */
    FIT_XY,

    /**
     * Center display, no scaling
     */
    CENTER
}

/**
 * Placeholder replacement map
 * Supports method chaining
 */
class PlaceholderReplacementMap {
    private val replacements = mutableMapOf<String, PlaceholderReplacement>()

    /**
     * Add placeholder replacement configuration
     *
     * @param key Placeholder key
     * @param source Image source
     * @return this, supports method chaining
     */
    fun add(key: String, source: Any): PlaceholderReplacementMap {
        replacements[key] = PlaceholderReplacement(key, source)
        return this
    }

    /**
     * Add placeholder replacement configuration (full configuration)
     *
     * @param replacement Placeholder replacement configuration
     * @return this, supports method chaining
     */
    fun add(replacement: PlaceholderReplacement): PlaceholderReplacementMap {
        replacements[replacement.placeholderKey] = replacement
        return this
    }

    /**
     * Batch add
     *
     * @param map Map from placeholder key to image source
     * @return this, supports method chaining
     */
    fun addAll(map: Map<String, Any>): PlaceholderReplacementMap {
        map.forEach { (key, source) ->
            replacements[key] = PlaceholderReplacement(key, source)
        }
        return this
    }

    /**
     * Get replacement configuration for specified key
     */
    fun get(key: String): PlaceholderReplacement? = replacements[key]

    /**
     * Get all replacement configurations
     */
    fun getAll(): Map<String, PlaceholderReplacement> = replacements.toMap()

    /**
     * Whether empty
     */
    fun isEmpty(): Boolean = replacements.isEmpty()

    /**
     * Whether contains specified key
     */
    fun containsKey(key: String): Boolean = replacements.containsKey(key)

    /**
     * Clear all configurations
     */
    fun clear() {
        replacements.clear()
    }
}

