package com.kernelflux.lottie.model.animatable;

import com.kernelflux.lottie.animation.keyframe.BaseKeyframeAnimation;
import com.kernelflux.lottie.value.Keyframe;

import java.util.List;

public interface AnimatableValue<K, A> {
  List<Keyframe<K>> getKeyframes();

  boolean isStatic();

  BaseKeyframeAnimation<K, A> createAnimation();
}
