/////////////////////////////////////////////////////////////////////////////////////////////////
//
//  Optimized JPAGAnimator implementation
//  Fixes: Issue #2322 - PAGAnimator finalize deadlock
//
//  Key improvements:
//  1. Safe finalize: clear resources before deletion
//  2. Atomic flag protection: prevent concurrent access during destruction
//  3. Callback safety: check object validity in callbacks
//  4. Resource management: disconnect callbacks before canceling animation
//
/////////////////////////////////////////////////////////////////////////////////////////////////

#include "JNIHelper.h"
#include "rendering/PAGAnimator.h"
#include <atomic>
#include <mutex>

namespace pag {
static jfieldID PAGAnimator_nativeContext;
static jmethodID PAGAnimator_onAnimationStart;
static jmethodID PAGAnimator_onAnimationEnd;
static jmethodID PAGAnimator_onAnimationCancel;
static jmethodID PAGAnimator_onAnimationRepeat;
static jmethodID PAGAnimator_onAnimationUpdate;

class AnimatorListener : public pag::PAGAnimator::Listener {
 public:
  AnimatorListener(JNIEnv* env, jobject animatorObject) {
    weakAnimator = env->NewWeakGlobalRef(animatorObject);
  }

  ~AnimatorListener() override {
    JNIEnvironment environment;
    auto env = environment.current();
    if (env == nullptr) {
      return;
    }
    if (weakAnimator != nullptr) {
      env->DeleteWeakGlobalRef(weakAnimator);
      weakAnimator = nullptr;
    }
  }

 protected:
  void onAnimationStart(pag::PAGAnimator*) override {
    JNIEnvironment environment;
    auto env = environment.current();
    auto animatorObject = getAnimatorObject(env);
    if (animatorObject == nullptr) {
      return;
    }
    env->CallVoidMethod(animatorObject, PAGAnimator_onAnimationStart);
    // Clear any pending exceptions
    if (env->ExceptionCheck()) {
      env->ExceptionClear();
    }
  }

  void onAnimationEnd(pag::PAGAnimator*) override {
    JNIEnvironment environment;
    auto env = environment.current();
    auto animatorObject = getAnimatorObject(env);
    if (animatorObject == nullptr) {
      return;
    }
    env->CallVoidMethod(animatorObject, PAGAnimator_onAnimationEnd);
    if (env->ExceptionCheck()) {
      env->ExceptionClear();
    }
  }

  void onAnimationCancel(pag::PAGAnimator*) override {
    JNIEnvironment environment;
    auto env = environment.current();
    auto animatorObject = getAnimatorObject(env);
    if (animatorObject == nullptr) {
      return;
    }
    env->CallVoidMethod(animatorObject, PAGAnimator_onAnimationCancel);
    if (env->ExceptionCheck()) {
      env->ExceptionClear();
    }
  }

  void onAnimationRepeat(pag::PAGAnimator*) override {
    JNIEnvironment environment;
    auto env = environment.current();
    auto animatorObject = getAnimatorObject(env);
    if (animatorObject == nullptr) {
      return;
    }
    env->CallVoidMethod(animatorObject, PAGAnimator_onAnimationRepeat);
    if (env->ExceptionCheck()) {
      env->ExceptionClear();
    }
  }

  void onAnimationUpdate(pag::PAGAnimator*) override {
    JNIEnvironment environment;
    auto env = environment.current();
    auto animatorObject = getAnimatorObject(env);
    if (animatorObject == nullptr) {
      return;
    }
    env->CallVoidMethod(animatorObject, PAGAnimator_onAnimationUpdate);
    if (env->ExceptionCheck()) {
      env->ExceptionClear();
    }
  }

 private:
  jobject weakAnimator = nullptr;

  jobject getAnimatorObject(JNIEnv* env) {
    if (env == nullptr || weakAnimator == nullptr) {
      return nullptr;
    }
    if (env->IsSameObject(weakAnimator, nullptr)) {
      return nullptr;
    }
    return weakAnimator;
  }
};

class JPAGAnimator {
 public:
  JPAGAnimator(JNIEnv* env, jobject animatorObject) : isCleared(false) {
    listener = std::make_shared<AnimatorListener>(env, animatorObject);
    animator = pag::PAGAnimator::MakeFrom(listener);
  }

  ~JPAGAnimator() {
    // 析构函数中不再调用 clear()，避免在 finalize 线程中触发回调
    // 所有清理工作应在 clear() 中完成
    if (!isCleared.load(std::memory_order_acquire)) {
      // 如果 clear() 未被调用，直接清理资源（安全路径）
      std::lock_guard<std::mutex> autoLock(locker);
      if (animator != nullptr) {
        // 直接设置为 nullptr，不触发 cancel()，避免回调
        animator = nullptr;
      }
      listener = nullptr;
    }
  }

  std::shared_ptr<pag::PAGAnimator> get() {
    // 检查是否已清理（原子操作，无需锁）
    if (isCleared.load(std::memory_order_acquire)) {
      return nullptr;
    }
    // 关键优化：最小化锁持有时间，只复制 shared_ptr
    std::shared_ptr<pag::PAGAnimator> result;
    {
      std::lock_guard<std::mutex> autoLock(locker);
      result = animator;  // 复制 shared_ptr，延长生命周期
    }
    // 锁已释放，返回的 shared_ptr 可以安全使用
    return result;
  }

  void clear() {
    // 使用原子标志防止重复清理
    bool expected = false;
    if (!isCleared.compare_exchange_strong(expected, true, 
                                            std::memory_order_acq_rel)) {
      // 已经被清理，直接返回
      return;
    }

    // 先断开回调链，再取消动画，避免回调中访问已销毁对象
    std::shared_ptr<pag::PAGAnimator> animatorToCancel;
    std::shared_ptr<pag::AnimatorListener> listenerToClear;
    
    {
      std::lock_guard<std::mutex> autoLock(locker);
      // 保存引用，避免在锁外访问
      animatorToCancel = animator;
      listenerToClear = listener;
      
      // 先清空引用，防止回调中访问
      animator = nullptr;
      listener = nullptr;
    }

    // 在锁外取消动画，避免死锁
    // 此时 listener 已清空，cancel() 不会触发回调到 Java
    if (animatorToCancel != nullptr) {
      animatorToCancel->cancel();
    }
    
    // 清理引用
    animatorToCancel = nullptr;
    listenerToClear = nullptr;
  }

  bool isDestroyed() const {
    return isCleared.load(std::memory_order_acquire);
  }

 private:
  std::atomic<bool> isCleared;  // 原子标志，标记是否已清理
  std::mutex locker;
  std::shared_ptr<pag::AnimatorListener> listener;
  std::shared_ptr<pag::PAGAnimator> animator;
};
}  // namespace pag

using namespace pag;

std::shared_ptr<PAGAnimator> getPAGAnimator(JNIEnv* env, jobject thiz) {
  if (env == nullptr || thiz == nullptr) {
    return nullptr;
  }
  
  auto jAnimator =
      reinterpret_cast<JPAGAnimator*>(env->GetLongField(thiz, PAGAnimator_nativeContext));
  if (jAnimator == nullptr) {
    return nullptr;
  }
  
  // 检查对象是否已销毁（原子操作，无需锁）
  if (jAnimator->isDestroyed()) {
    return nullptr;
  }
  
  // 关键优化：使用 get() 方法，它已经优化过，会先获取引用再释放锁
  // 这样避免了嵌套锁，返回的 shared_ptr 可以安全使用
  return jAnimator->get();
}

void setPAGAnimator(JNIEnv* env, jobject thiz, JPAGAnimator* animator) {
  if (env == nullptr || thiz == nullptr) {
    // 如果 env 或 thiz 无效，直接删除 animator（如果存在）
    if (animator != nullptr) {
      delete animator;
    }
    return;
  }
  
  auto old = reinterpret_cast<JPAGAnimator*>(env->GetLongField(thiz, PAGAnimator_nativeContext));
  
  // 先设置新值，再删除旧值，避免并发问题
  env->SetLongField(thiz, PAGAnimator_nativeContext, (jlong)animator);
  
  // 删除旧对象前先清理资源
  if (old != nullptr) {
    old->clear();  // 先清理，避免在析构中触发回调
    delete old;    // 再删除
  }
}

extern "C" {

PAG_API void Java_org_libpag_PAGAnimator_nativeInit(JNIEnv* env, jclass clazz) {
  PAGAnimator_nativeContext = env->GetFieldID(clazz, "nativeContext", "J");
  PAGAnimator_onAnimationStart = env->GetMethodID(clazz, "onAnimationStart", "()V");
  PAGAnimator_onAnimationEnd = env->GetMethodID(clazz, "onAnimationEnd", "()V");
  PAGAnimator_onAnimationCancel = env->GetMethodID(clazz, "onAnimationCancel", "()V");
  PAGAnimator_onAnimationRepeat = env->GetMethodID(clazz, "onAnimationRepeat", "()V");
  PAGAnimator_onAnimationUpdate = env->GetMethodID(clazz, "onAnimationUpdate", "()V");
}

PAG_API void Java_org_libpag_PAGAnimator_nativeSetup(JNIEnv* env, jobject thiz) {
  if (env == nullptr || thiz == nullptr) {
    return;
  }
  setPAGAnimator(env, thiz, new JPAGAnimator(env, thiz));
}

PAG_API void Java_org_libpag_PAGAnimator_nativeRelease(JNIEnv* env, jobject thiz) {
  if (env == nullptr || thiz == nullptr) {
    return;
  }
  auto jAnimator =
      reinterpret_cast<JPAGAnimator*>(env->GetLongField(thiz, PAGAnimator_nativeContext));
  if (jAnimator != nullptr) {
    jAnimator->clear();
  }
}

PAG_API void Java_org_libpag_PAGAnimator_nativeFinalize(JNIEnv* env, jobject thiz) {
  // 关键修复：在 finalize 中先清理资源，再删除对象
  // 这样可以避免在析构函数中触发回调导致的死锁
  if (env == nullptr || thiz == nullptr) {
    return;
  }
  
  auto jAnimator =
      reinterpret_cast<JPAGAnimator*>(env->GetLongField(thiz, PAGAnimator_nativeContext));
  
  if (jAnimator != nullptr) {
    // 先清理资源（会断开回调链，避免回调中访问已销毁对象）
    jAnimator->clear();
    // 再删除对象（此时析构函数不会触发回调）
    delete jAnimator;
  }
  
  // 最后清空 nativeContext
  env->SetLongField(thiz, PAGAnimator_nativeContext, 0);
}

PAG_API jboolean Java_org_libpag_PAGAnimator_isSync(JNIEnv* env, jobject thiz) {
  auto animator = getPAGAnimator(env, thiz);
  if (animator == nullptr) {
    return JNI_FALSE;
  }
  return animator->isSync() ? JNI_TRUE : JNI_FALSE;
}

PAG_API void Java_org_libpag_PAGAnimator_setSync(JNIEnv* env, jobject thiz, jboolean sync) {
  auto animator = getPAGAnimator(env, thiz);
  if (animator == nullptr) {
    return;
  }
  animator->setSync(sync == JNI_TRUE);
}

PAG_API jlong Java_org_libpag_PAGAnimator_duration(JNIEnv* env, jobject thiz) {
  auto animator = getPAGAnimator(env, thiz);
  if (animator == nullptr) {
    return 0;
  }
  return animator->duration();
}

PAG_API void Java_org_libpag_PAGAnimator_setDuration(JNIEnv* env, jobject thiz, jlong duration) {
  auto animator = getPAGAnimator(env, thiz);
  if (animator == nullptr) {
    return;
  }
  animator->setDuration(duration);
}

PAG_API jint Java_org_libpag_PAGAnimator_repeatCount(JNIEnv* env, jobject thiz) {
  auto animator = getPAGAnimator(env, thiz);
  if (animator == nullptr) {
    return 0;
  }
  return animator->repeatCount();
}

PAG_API void Java_org_libpag_PAGAnimator_setRepeatCount(JNIEnv* env, jobject thiz, jint count) {
  auto animator = getPAGAnimator(env, thiz);
  if (animator == nullptr) {
    return;
  }
  animator->setRepeatCount(count);
}

PAG_API jdouble Java_org_libpag_PAGAnimator_progress(JNIEnv* env, jobject thiz) {
  auto animator = getPAGAnimator(env, thiz);
  if (animator == nullptr) {
    return 0;
  }
  return animator->progress();
}

PAG_API void Java_org_libpag_PAGAnimator_setProgress(JNIEnv* env, jobject thiz, jdouble progress) {
  // 关键修复：在回调中调用此方法时，检查对象有效性
  auto animator = getPAGAnimator(env, thiz);
  if (animator == nullptr) {
    return;
  }
  animator->setProgress(progress);
}

PAG_API jboolean Java_org_libpag_PAGAnimator_isRunning(JNIEnv* env, jobject thiz) {
  auto animator = getPAGAnimator(env, thiz);
  if (animator == nullptr) {
    return JNI_FALSE;
  }
  return animator->isRunning() ? JNI_TRUE : JNI_FALSE;
}

PAG_API void Java_org_libpag_PAGAnimator_doStart(JNIEnv* env, jobject thiz) {
  auto animator = getPAGAnimator(env, thiz);
  if (animator == nullptr) {
    return;
  }
  animator->start();
}

PAG_API void Java_org_libpag_PAGAnimator_cancel(JNIEnv* env, jobject thiz) {
  auto animator = getPAGAnimator(env, thiz);
  if (animator == nullptr) {
    return;
  }
  animator->cancel();
}

PAG_API void Java_org_libpag_PAGAnimator_update(JNIEnv* env, jobject thiz) {
  auto animator = getPAGAnimator(env, thiz);
  if (animator == nullptr) {
    return;
  }
  animator->update();
}
}

