package com.kernelflux.lottie.model.content;


import androidx.annotation.Nullable;

import com.kernelflux.lottie.LottieComposition;
import com.kernelflux.lottie.LottieDrawable;
import com.kernelflux.lottie.animation.content.Content;
import com.kernelflux.lottie.model.layer.BaseLayer;

public interface ContentModel {
  @Nullable Content toContent(LottieDrawable drawable, LottieComposition composition, BaseLayer layer);
}
