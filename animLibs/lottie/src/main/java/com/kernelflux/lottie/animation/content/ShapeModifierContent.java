package com.kernelflux.lottie.animation.content;

import com.kernelflux.lottie.animation.keyframe.BaseKeyframeAnimation;
import com.kernelflux.lottie.model.content.ShapeData;

public interface ShapeModifierContent extends Content {
  void addUpdateListener(BaseKeyframeAnimation.AnimationListener listener);
  ShapeData modifyShape(ShapeData shapeData);
}
