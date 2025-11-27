package com.kernelflux.lottie.model.content;

import android.graphics.PointF;

import com.kernelflux.lottie.LottieComposition;
import com.kernelflux.lottie.LottieDrawable;
import com.kernelflux.lottie.animation.content.Content;
import com.kernelflux.lottie.animation.content.EllipseContent;
import com.kernelflux.lottie.model.animatable.AnimatablePointValue;
import com.kernelflux.lottie.model.animatable.AnimatableValue;
import com.kernelflux.lottie.model.layer.BaseLayer;

public class CircleShape implements ContentModel {
  private final String name;
  private final AnimatableValue<PointF, PointF> position;
  private final AnimatablePointValue size;
  private final boolean isReversed;
  private final boolean hidden;

  public CircleShape(String name, AnimatableValue<PointF, PointF> position,
      AnimatablePointValue size, boolean isReversed, boolean hidden) {
    this.name = name;
    this.position = position;
    this.size = size;
    this.isReversed = isReversed;
    this.hidden = hidden;
  }

  @Override public Content toContent(LottieDrawable drawable, LottieComposition composition, BaseLayer layer) {
    return new EllipseContent(drawable, layer, this);
  }

  public String getName() {
    return name;
  }

  public AnimatableValue<PointF, PointF> getPosition() {
    return position;
  }

  public AnimatablePointValue getSize() {
    return size;
  }

  public boolean isReversed() {
    return isReversed;
  }

  public boolean isHidden() {
    return hidden;
  }
}
