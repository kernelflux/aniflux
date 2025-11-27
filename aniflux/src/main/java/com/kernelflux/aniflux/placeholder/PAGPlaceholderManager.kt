package com.kernelflux.aniflux.placeholder

import android.graphics.Bitmap
import android.view.View
import androidx.lifecycle.Lifecycle
import org.libpag.PAGFile
import org.libpag.PAGImage
import org.libpag.PAGImageView
import org.libpag.PAGLayer

/**
 * PAG 占位图管理器
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
        // 安全检查：如果View类型不匹配，直接返回，不抛出异常
        val pagView = view as? PAGImageView ?: return
        
        // 获取所有可编辑的图片图层索引
        val editableIndices = try {
            pagFile.getEditableIndices(PAGLayer.LayerTypeImage)
        } catch (e: Exception) {
            return
        }
        if (editableIndices.isEmpty()) return
        
        // 建立占位符key到索引的映射
        replacements.getAll().forEach { (key, _) ->
            try {
                // 方法1：通过图层名称查找
                val indexByName = findLayerIndexByName(pagFile, key, editableIndices)
                if (indexByName != null) {
                    keyToIndexMap[key] = indexByName
                    return@forEach
                }
                
                // 方法2：通过索引顺序匹配（如果key是数字）
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
                // 单个key映射失败不影响其他key
            }
        }
        
        // 加载所有占位图
        replacements.getAll().forEach { (key, replacement) ->
            val targetIndex = keyToIndexMap[key] ?: return@forEach
            
            try {
                val request = imageLoader.load(
                    context = view.context,
                    source = replacement.imageSource,
                    width = 0,  // PAG会根据图层尺寸处理
                    height = 0,
                    callback = object : PlaceholderImageLoadCallback {
                        override fun onSuccess(bitmap: Bitmap) {
                            if (isCleared) return
                            
                            // 在主线程更新
                            mainHandler.post {
                                if (isCleared) return@post
                                
                                try {
                                    // 再次检查view是否有效
                                    val currentView = view
                                    // 创建PAGImage并替换
                                    val pagImage = PAGImage.FromBitmap(bitmap)
                                    pagFile.replaceImage(targetIndex, pagImage)
                                    
                                    // 刷新视图
                                    currentView.invalidate()
                                } catch (e: Exception) {
                                    // 忽略设置失败的错误，不影响主流程
                                }
                            }
                        }
                        
                        override fun onError(error: Throwable) {
                            // 错误处理：静默失败，不影响动画播放
                        }
                    }
                )
                activeRequests.add(request)
            } catch (e: Exception) {
                // 单个占位图加载失败不影响其他占位图
            }
        }
    }
    
    /**
     * 通过图层名称查找索引
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
                    // 单个图层查找失败，继续查找下一个
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

