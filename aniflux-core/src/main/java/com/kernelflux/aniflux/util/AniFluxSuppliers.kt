package com.kernelflux.aniflux.util

object AniFluxSuppliers {

    /**
     * Similar to Java 8's Supplier interface, used for lazy object retrieval
     * @param T Data type
     */
    fun interface AniFluxSupplier<T> {
        /**
         * Get object instance
         * @return Non-null T type object
         */
        fun get(): T
    }


    /**
     * Wrap a regular Supplier into a memoized cached Supplier
     * Uses double-checked locking pattern to ensure thread safety
     *
     * @param supplier Original Supplier
     * @return Cached Supplier
     */
    @JvmStatic
    fun <T> memorize(supplier: AniFluxSupplier<T>): AniFluxSupplier<T> {
        return object : AniFluxSupplier<T> {
            @Volatile
            private var instance: T? = null

            override fun get(): T {
                return instance ?: synchronized(this) {
                    instance ?: requireNotNull(supplier.get()) {
                        "Supplier returned null value"
                    }.also {
                        instance = it
                    }
                }
            }
        }
    }
}
