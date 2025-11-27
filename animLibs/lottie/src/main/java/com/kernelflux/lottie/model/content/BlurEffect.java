package com.kernelflux.lottie.model.content;

import com.kernelflux.lottie.model.animatable.AnimatableFloatValue;

public class BlurEffect {

  final AnimatableFloatValue blurriness;

  public BlurEffect(AnimatableFloatValue blurriness) {
    this.blurriness = blurriness;
  }

  public AnimatableFloatValue getBlurriness() {
    return blurriness;
  }
}
