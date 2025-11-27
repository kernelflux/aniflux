package com.kernelflux.lottie.model.content;

import android.graphics.PointF;

import com.kernelflux.lottie.LottieComposition;
import com.kernelflux.lottie.LottieDrawable;
import com.kernelflux.lottie.animation.content.Content;
import com.kernelflux.lottie.animation.content.RectangleContent;
import com.kernelflux.lottie.model.animatable.AnimatableFloatValue;
import com.kernelflux.lottie.model.animatable.AnimatableValue;
import com.kernelflux.lottie.model.layer.BaseLayer;

public class RectangleShape implements ContentModel {
  private final String name;
  private final AnimatableValue<PointF, PointF> position;
  private final AnimatableValue<PointF, PointF> size;
  private final AnimatableFloatValue cornerRadius;
  private final boolean hidden;

  public RectangleShape(String name, AnimatableValue<PointF, PointF> position,
      AnimatableValue<PointF, PointF> size, AnimatableFloatValue cornerRadius, boolean hidden) {
    this.name = name;
    this.position = position;
    this.size = size;
    this.cornerRadius = cornerRadius;
    this.hidden = hidden;
  }

  public String getName() {
    return name;
  }

  public AnimatableFloatValue getCornerRadius() {
    return cornerRadius;
  }

  public AnimatableValue<PointF, PointF> getSize() {
    return size;
  }

  public AnimatableValue<PointF, PointF> getPosition() {
    return position;
  }

  public boolean isHidden() {
    return hidden;
  }

  @Override public Content toContent(LottieDrawable drawable, LottieComposition composition, BaseLayer layer) {
    return new RectangleContent(drawable, layer, this);
  }

  @Override public String toString() {
    return "RectangleShape{position=" + position +
        ", size=" + size +
        '}';
  }
}
