/*
 * Tencent is pleased to support the open source community by making vap available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * Licensed under the MIT License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kernelflux.vap

import android.content.res.AssetManager
import android.graphics.SurfaceTexture
import com.kernelflux.vap.file.IFileContainer
import com.kernelflux.vap.inter.IAnimListener
import com.kernelflux.vap.inter.IFetchResource
import com.kernelflux.vap.inter.OnResourceClickListener
import com.kernelflux.vap.mask.MaskConfig
import com.kernelflux.vap.util.IScaleType
import com.kernelflux.vap.util.ScaleType
import java.io.File

interface IAnimView {

    fun prepareTextureView()

    fun getSurfaceTexture(): SurfaceTexture?

    fun setAnimListener(animListener: IAnimListener?)

    fun setFetchResource(fetchResource: IFetchResource?)

    fun setOnResourceClickListener(resourceClickListener: OnResourceClickListener?)

    fun setLoop(playLoop: Int)

    fun supportMask(isSupport: Boolean, isEdgeBlur: Boolean)

    fun updateMaskConfig(maskConfig: MaskConfig?)

    fun setFps(fps: Int)

    fun setScaleType(type: ScaleType)

    fun setScaleType(scaleType: IScaleType)

    fun setMute(isMute: Boolean)

    fun startPlay(file: File)

    fun startPlay(assetManager: AssetManager, assetsPath: String)

    fun startPlay(fileContainer: IFileContainer)

    fun stopPlay()

    fun isRunning(): Boolean

    fun getRealSize(): Pair<Int, Int>
}
