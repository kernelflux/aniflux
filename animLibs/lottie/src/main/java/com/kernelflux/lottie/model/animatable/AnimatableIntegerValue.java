package com.kernelflux.lottie.model.animatable;

import com.kernelflux.lottie.animation.keyframe.BaseKeyframeAnimation;
import com.kernelflux.lottie.animation.keyframe.IntegerKeyframeAnimation;
import com.kernelflux.lottie.value.Keyframe;

import java.util.List;

public class AnimatableIntegerValue extends BaseAnimatableValue<Integer, Integer> {

  public AnimatableIntegerValue(List<Keyframe<Integer>> keyframes) {
    super(keyframes);
  }

  @Override public BaseKeyframeAnimation<Integer, Integer> createAnimation() {
    return new IntegerKeyframeAnimation(keyframes);
  }
}
