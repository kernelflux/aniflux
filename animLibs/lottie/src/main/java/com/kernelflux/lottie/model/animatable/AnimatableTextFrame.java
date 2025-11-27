package com.kernelflux.lottie.model.animatable;

import com.kernelflux.lottie.animation.keyframe.TextKeyframeAnimation;
import com.kernelflux.lottie.model.DocumentData;
import com.kernelflux.lottie.value.Keyframe;

import java.util.List;

public class AnimatableTextFrame extends BaseAnimatableValue<DocumentData, DocumentData> {

  public AnimatableTextFrame(List<Keyframe<DocumentData>> keyframes) {
    super(keyframes);
  }

  @Override public TextKeyframeAnimation createAnimation() {
    return new TextKeyframeAnimation(keyframes);
  }
}
