
package com.kernelflux.pag.extra.relinker;

public class MissingLibraryException extends RuntimeException {
    public MissingLibraryException(final String library) {
        super(library);
    }
}