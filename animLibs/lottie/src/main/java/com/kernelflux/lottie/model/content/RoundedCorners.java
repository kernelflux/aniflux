package com.kernelflux.lottie.model.content;

import androidx.annotation.Nullable;

import com.kernelflux.lottie.LottieComposition;
import com.kernelflux.lottie.LottieDrawable;
import com.kernelflux.lottie.animation.content.Content;
import com.kernelflux.lottie.animation.content.RoundedCornersContent;
import com.kernelflux.lottie.model.animatable.AnimatableValue;
import com.kernelflux.lottie.model.layer.BaseLayer;

public class RoundedCorners implements ContentModel {
  private final String name;
  private final AnimatableValue<Float, Float> cornerRadius;

  public RoundedCorners(String name, AnimatableValue<Float, Float> cornerRadius) {
    this.name = name;
    this.cornerRadius = cornerRadius;
  }

  public String getName() {
    return name;
  }

  public AnimatableValue<Float, Float> getCornerRadius() {
    return cornerRadius;
  }

  @Nullable @Override public Content toContent(LottieDrawable drawable, LottieComposition composition, BaseLayer layer) {
    return new RoundedCornersContent(drawable, layer, this);
  }
}
