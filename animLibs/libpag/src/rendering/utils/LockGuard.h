/////////////////////////////////////////////////////////////////////////////////////////////////
//
//  Tencent is pleased to support the open source community by making libpag available.
//
//  Copyright (C) 2021 Tencent. All rights reserved.
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

#pragma once

#include <memory>
#include <mutex>

namespace pag {

class LockGuard {
 public:
  explicit LockGuard(std::shared_ptr<std::mutex> locker) : mutex(std::move(locker)) {
    if (mutex) {
      // 关键修复：在 -fno-exceptions 模式下，mutex 操作是安全的
      // 如果 mutex 已被销毁，行为是未定义的，但不会抛出异常
      mutex->lock();
      isLocked = true;
    } else {
      isLocked = false;
    }
  }

  ~LockGuard() {
    if (isLocked && mutex) {
      // 关键修复：在 -fno-exceptions 模式下，直接调用 unlock
      // 如果 mutex 已被销毁，行为是未定义的，但不会抛出异常
      mutex->unlock();
    }
  }

  // 禁止拷贝和赋值
  LockGuard(const LockGuard&) = delete;
  LockGuard& operator=(const LockGuard&) = delete;

  // 检查锁是否有效
  bool isValid() const {
    return isLocked && mutex != nullptr;
  }

 private:
  std::shared_ptr<std::mutex> mutex;
  bool isLocked = false;
};

}  // namespace pag
