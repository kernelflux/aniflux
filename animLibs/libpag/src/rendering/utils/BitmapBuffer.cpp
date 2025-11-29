/////////////////////////////////////////////////////////////////////////////////////////////////
//
//  Tencent is pleased to support the open source community by making libpag available.
//
//  Copyright (C) 2023 Tencent. All rights reserved.
//
//  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
//  except in compliance with the License. You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  unless required by applicable law or agreed to in writing, software distributed under the
//  license is distributed on an "as is" basis, without warranties or conditions of any kind,
//  either express or implied. see the license for the specific language governing permissions
//  and limitations under the license.
//
/////////////////////////////////////////////////////////////////////////////////////////////////

#include "BitmapBuffer.h"
#include "base/utils/Log.h"
#include <thread>
#include <chrono>

namespace pag {
std::shared_ptr<BitmapBuffer> BitmapBuffer::Wrap(pag::HardwareBufferRef hardwareBuffer) {
  if (hardwareBuffer == nullptr) {
    return nullptr;
  }
  
  // 关键修复：检查 HardwareBuffer 是否有效
  if (!tgfx::HardwareBufferCheck(hardwareBuffer)) {
    LOGE("BitmapBuffer::Wrap() HardwareBuffer is invalid!");
    return nullptr;
  }
  
  auto info = tgfx::HardwareBufferGetInfo(hardwareBuffer);
  if (info.isEmpty()) {
    LOGE("BitmapBuffer::Wrap() Failed to get HardwareBuffer info!");
    return nullptr;
  }
  
  auto bitmap = std::shared_ptr<BitmapBuffer>(new BitmapBuffer(info));
  bitmap->hardwareBuffer = hardwareBuffer;
  bitmap->hardwareBacked = true;
  return bitmap;
}

std::shared_ptr<BitmapBuffer> BitmapBuffer::Wrap(const tgfx::ImageInfo& info, void* pixels) {
  if (info.isEmpty()) {
    return nullptr;
  }
  auto bitmap = std::shared_ptr<BitmapBuffer>(new BitmapBuffer(info));
  bitmap->pixels = pixels;
  return bitmap;
}

BitmapBuffer::BitmapBuffer(const tgfx::ImageInfo& info) : _info(info) {
}

HardwareBufferRef BitmapBuffer::getHardwareBuffer() const {
  return hardwareBacked ? hardwareBuffer : nullptr;
}

void* BitmapBuffer::lockPixels() {
  if (hardwareBuffer != nullptr) {
    // 关键修复：首先检查 HardwareBuffer 是否有效
    if (!tgfx::HardwareBufferCheck(hardwareBuffer)) {
      LOGE("BitmapBuffer::lockPixels() HardwareBuffer is invalid!");
      return nullptr;
    }
    
    // 关键修复：添加错误处理和重试机制
    void* result = nullptr;
    int retryCount = 0;
    const int MAX_RETRY = 3;
    
    while (retryCount < MAX_RETRY) {
      result = tgfx::HardwareBufferLock(hardwareBuffer);
      if (result != nullptr) {
        return result;
      }
      
      // 再次检查 HardwareBuffer 是否仍然有效
      if (!tgfx::HardwareBufferCheck(hardwareBuffer)) {
        LOGE("BitmapBuffer::lockPixels() HardwareBuffer became invalid during lock!");
        return nullptr;
      }
      
      retryCount++;
      if (retryCount < MAX_RETRY) {
        // 短暂延迟后重试，避免立即重试导致的系统压力
        std::this_thread::sleep_for(std::chrono::milliseconds(1));
      }
    }
    
    LOGE("BitmapBuffer::lockPixels() Failed to lock HardwareBuffer after %d retries!", MAX_RETRY);
    return nullptr;
  }
  return pixels;
}

void BitmapBuffer::unlockPixels() {
  if (hardwareBuffer != nullptr) {
    // 关键修复：检查 HardwareBuffer 是否仍然有效
    if (!tgfx::HardwareBufferCheck(hardwareBuffer)) {
      LOGE("BitmapBuffer::unlockPixels() HardwareBuffer is invalid, skip unlock!");
      return;
    }
    
    // 关键修复：在 -fno-exceptions 模式下，直接调用 unlock
    // 如果 HardwareBuffer 无效，HardwareBufferUnlock 会返回错误，但不会抛出异常
    tgfx::HardwareBufferUnlock(hardwareBuffer);
  }
}

}  // namespace pag
