package com.kernelflux.lottie.parser;


import com.kernelflux.lottie.parser.moshi.JsonReader;

import java.io.IOException;

interface ValueParser<V> {
  V parse(JsonReader reader, float scale) throws IOException;
}
