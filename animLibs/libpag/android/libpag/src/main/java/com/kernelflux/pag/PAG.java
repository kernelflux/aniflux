package com.kernelflux.pag;

import com.kernelflux.pag.extra.tools.LibraryLoadUtils;

public class PAG {
    /**
     * Get SDK version information.
     */
    public static native String SDKVersion();

    static {
        LibraryLoadUtils.loadLibrary("anifluxPag");
    }
}
