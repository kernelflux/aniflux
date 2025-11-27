package com.kernelflux.lottie.animation.content;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import androidx.annotation.Nullable;
import com.kernelflux.lottie.utils.DropShadow;

public interface DrawingContent extends Content {
  void draw(Canvas canvas, Matrix parentMatrix, int alpha, @Nullable DropShadow shadowToApply);

  void getBounds(RectF outBounds, Matrix parentMatrix, boolean applyParents);
}
