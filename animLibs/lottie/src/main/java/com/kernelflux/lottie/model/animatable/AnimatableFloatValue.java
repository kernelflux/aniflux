package com.kernelflux.lottie.model.animatable;

import com.kernelflux.lottie.animation.keyframe.FloatKeyframeAnimation;
import com.kernelflux.lottie.value.Keyframe;

import java.util.List;

public class AnimatableFloatValue extends BaseAnimatableValue<Float, Float> {

  public AnimatableFloatValue(List<Keyframe<Float>> keyframes) {
    super(keyframes);
  }

  @Override public FloatKeyframeAnimation createAnimation() {
    return new FloatKeyframeAnimation(keyframes);
  }
}
