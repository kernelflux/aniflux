package com.kernelflux.lottie.model.animatable;

import androidx.annotation.Nullable;

public class AnimatableTextProperties {

  @Nullable public final AnimatableTextStyle textStyle;
  @Nullable public final AnimatableTextRangeSelector rangeSelector;

  public AnimatableTextProperties(
      @Nullable AnimatableTextStyle textStyle,
      @Nullable AnimatableTextRangeSelector rangeSelector) {
    this.textStyle = textStyle;
    this.rangeSelector = rangeSelector;
  }
}
