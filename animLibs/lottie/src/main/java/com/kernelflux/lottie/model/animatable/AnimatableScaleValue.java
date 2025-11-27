package com.kernelflux.lottie.model.animatable;

import com.kernelflux.lottie.animation.keyframe.BaseKeyframeAnimation;
import com.kernelflux.lottie.animation.keyframe.ScaleKeyframeAnimation;
import com.kernelflux.lottie.value.Keyframe;
import com.kernelflux.lottie.value.ScaleXY;

import java.util.List;

public class AnimatableScaleValue extends BaseAnimatableValue<ScaleXY, ScaleXY> {

  public AnimatableScaleValue(ScaleXY value) {
    super(value);
  }

  public AnimatableScaleValue(List<Keyframe<ScaleXY>> keyframes) {
    super(keyframes);
  }

  @Override public BaseKeyframeAnimation<ScaleXY, ScaleXY> createAnimation() {
    return new ScaleKeyframeAnimation(keyframes);
  }
}
