package com.kernelflux.aniflux.util

object AniFluxSuppliers {

    /**
     * 类似Java 8的Supplier接口，用于延迟获取对象
     * @param T 数据类型
     */
    fun interface AniFluxSupplier<T> {
        /**
         * 获取对象实例
         * @return 非空的T类型对象
         */
        fun get(): T
    }


    /**
     * 将普通的Supplier包装成带记忆化缓存的Supplier
     * 使用双重检查锁定模式确保线程安全
     *
     * @param supplier 原始的Supplier
     * @return 带缓存的Supplier
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
