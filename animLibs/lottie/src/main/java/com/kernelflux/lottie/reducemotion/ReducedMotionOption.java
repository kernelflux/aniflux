package com.kernelflux.lottie.reducemotion;

import android.content.Context;
import androidx.annotation.Nullable;

public interface ReducedMotionOption {

  /**
   * Returns the current reduced motion mode.
   */
  ReducedMotionMode getCurrentReducedMotionMode(@Nullable Context context);
}
