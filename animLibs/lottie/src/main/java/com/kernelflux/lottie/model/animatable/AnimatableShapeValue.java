package com.kernelflux.lottie.model.animatable;

import android.graphics.Path;

import com.kernelflux.lottie.animation.keyframe.ShapeKeyframeAnimation;
import com.kernelflux.lottie.model.content.ShapeData;
import com.kernelflux.lottie.value.Keyframe;

import java.util.List;

public class AnimatableShapeValue extends BaseAnimatableValue<ShapeData, Path> {

  public AnimatableShapeValue(List<Keyframe<ShapeData>> keyframes) {
    super(keyframes);
  }

  @Override public ShapeKeyframeAnimation createAnimation() {
    return new ShapeKeyframeAnimation(keyframes);
  }
}
