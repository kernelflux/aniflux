package com.kernelflux.lottie.parser;

import android.graphics.PointF;

import com.kernelflux.lottie.LottieComposition;
import com.kernelflux.lottie.animation.keyframe.PathKeyframe;
import com.kernelflux.lottie.parser.moshi.JsonReader;
import com.kernelflux.lottie.utils.Utils;
import com.kernelflux.lottie.value.Keyframe;

import java.io.IOException;

class PathKeyframeParser {

  private PathKeyframeParser() {
  }

  static PathKeyframe parse(
      JsonReader reader, LottieComposition composition) throws IOException {
    boolean animated = reader.peek() == JsonReader.Token.BEGIN_OBJECT;
    Keyframe<PointF> keyframe = KeyframeParser.parse(
        reader, composition, Utils.dpScale(), PathParser.INSTANCE, animated, false);

    return new PathKeyframe(composition, keyframe);
  }
}
