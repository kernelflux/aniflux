package com.kernelflux.aniflux.placeholder

import android.graphics.Bitmap
import android.view.View
import androidx.lifecycle.Lifecycle
import com.kernelflux.pag.PAGFile
import com.kernelflux.pag.PAGImage
import com.kernelflux.pag.PAGImageView
import com.kernelflux.pag.PAGLayer

/**
 * PAG placeholder manager
 * 
 * @author: kerneflux
 * @date: 2025/11/27
 */
class PAGPlaceholderManager(
    view: View,
    private val pagFile: PAGFile,
    replacements: PlaceholderReplacementMap,
    imageLoader: PlaceholderImageLoader,
    lifecycle: Lifecycle?
) : PlaceholderManager(view, pagFile, replacements, imageLoader, lifecycle) {
    
    private val mainHandler = getMainHandler()
    private val keyToIndexMap = mutableMapOf<String, Int>()
    
    override fun applyReplacements() {
        // Get all editable image layer indices
        val editableIndices = try {
            pagFile.getEditableIndices(PAGLayer.LayerTypeImage)
        } catch (e: Exception) {
            return
        }
        if (editableIndices.isEmpty()) return
        
        // Build mapping from placeholder key to index
        replacements.getAll().forEach { (key, _) ->
            try {
                // Method 1: Find by layer name
                val indexByName = findLayerIndexByName(pagFile, key, editableIndices)
                if (indexByName != null) {
                    keyToIndexMap[key] = indexByName
                    return@forEach
                }
                
                // Method 2: Match by index order (if key is a number)
                val indexByOrder = try {
                    key.toIntOrNull()?.takeIf { it >= 0 && it < editableIndices.size }
                } catch (e: Exception) {
                    null
                }
                if (indexByOrder != null) {
                    keyToIndexMap[key] = editableIndices[indexByOrder]
                    return@forEach
                }
            } catch (e: Exception) {
                // Single key mapping failure doesn't affect other keys
            }
        }
        
        // Load all placeholder images
        replacements.getAll().forEach { (key, replacement) ->
            val targetIndex = keyToIndexMap[key] ?: return@forEach
            
            try {
                val request = imageLoader.load(
                    context = view.context,
                    source = replacement.imageSource,
                    width = 0,  // PAG will handle based on layer size
                    height = 0,
                    callback = object : PlaceholderImageLoadCallback {
                        override fun onSuccess(bitmap: Bitmap) {
                            if (isCleared) return
                            
                            // Update on main thread
                            mainHandler.post {
                                if (isCleared) return@post
                                
                                try {
                                    // Check again if view is valid
                                    val currentView = view
                                    // Create PAGImage and replace
                                    val pagImage = PAGImage.FromBitmap(bitmap)
                                    pagFile.replaceImage(targetIndex, pagImage)
                                    
                                    // Refresh view
                                    currentView.invalidate()
                                } catch (e: Exception) {
                                    // Ignore setup failure errors, don't affect main flow
                                }
                            }
                        }
                        
                        override fun onError(error: Throwable) {
                            // Error handling: fail silently, don't affect animation playback
                        }
                    }
                )
                activeRequests.add(request)
            } catch (e: Exception) {
                // Single placeholder load failure doesn't affect other placeholders
            }
        }
    }
    
    /**
     * Find index by layer name
     */
    private fun findLayerIndexByName(
        pagFile: PAGFile,
        layerName: String,
        editableIndices: IntArray
    ): Int? {
        return try {
            editableIndices.forEachIndexed { index, editableIndex ->
                try {
                    val layers = pagFile.getLayersByEditableIndex(editableIndex, PAGLayer.LayerTypeImage)
                    if (layers.isNotEmpty()) {
                        val layer = layers[0]
                        if (layer.layerName() == layerName) {
                            return editableIndex
                        }
                    }
                } catch (e: Exception) {
                    // Single layer lookup failed, continue to next
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        keyToIndexMap.clear()
    }
}

