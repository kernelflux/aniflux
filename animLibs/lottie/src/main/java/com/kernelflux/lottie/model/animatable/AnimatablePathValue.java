package com.kernelflux.lottie.model.animatable;

import android.graphics.PointF;

import com.kernelflux.lottie.animation.keyframe.BaseKeyframeAnimation;
import com.kernelflux.lottie.animation.keyframe.PathKeyframeAnimation;
import com.kernelflux.lottie.animation.keyframe.PointKeyframeAnimation;
import com.kernelflux.lottie.value.Keyframe;

import java.util.List;

public class AnimatablePathValue implements AnimatableValue<PointF, PointF> {
  private final List<Keyframe<PointF>> keyframes;

  public AnimatablePathValue(List<Keyframe<PointF>> keyframes) {
    this.keyframes = keyframes;
  }

  @Override
  public List<Keyframe<PointF>> getKeyframes() {
    return keyframes;
  }

  @Override
  public boolean isStatic() {
    return keyframes.size() == 1 && keyframes.get(0).isStatic();
  }

  @Override
  public BaseKeyframeAnimation<PointF, PointF> createAnimation() {
    if (keyframes.get(0).isStatic()) {
      return new PointKeyframeAnimation(keyframes);
    }
    return new PathKeyframeAnimation(keyframes);
  }
}
