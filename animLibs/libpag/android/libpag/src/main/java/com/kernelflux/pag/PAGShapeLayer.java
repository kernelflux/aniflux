package com.kernelflux.pag;

import com.kernelflux.pag.extra.tools.LibraryLoadUtils;

public class PAGShapeLayer extends PAGLayer {
    public PAGShapeLayer(long nativeContext) {
        super(nativeContext);
    }

    private static native void nativeInit();

    static {
        LibraryLoadUtils.loadLibrary("anifluxPag");
        nativeInit();
    }
}