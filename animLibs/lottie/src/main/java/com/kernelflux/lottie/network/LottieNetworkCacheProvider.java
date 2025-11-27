package com.kernelflux.lottie.network;


import androidx.annotation.NonNull;

import com.kernelflux.lottie.Lottie;

import java.io.File;

/**
 * Interface for providing the custom cache directory where animations downloaded via url are saved.
 *
 * @see Lottie#initialize
 */
public interface LottieNetworkCacheProvider {

  /**
   * Called during cache operations
   *
   * @return cache directory
   */
  @NonNull File getCacheDir();
}