package com.kernelflux.aniflux.log

/**
 * AniFlux log categories for better log organization
 *
 * @author: kernelflux
 * @date: 2025/12/06
 */
enum class AniFluxLogCategory(val tag: String) {
    /**
     * Request lifecycle logs
     */
    REQUEST("AniFlux.Request"),

    /**
     * Target (View) related logs
     */
    TARGET("AniFlux.Target"),

    /**
     * Engine related logs
     */
    ENGINE("AniFlux.Engine"),

    /**
     * Cache related logs
     */
    CACHE("AniFlux.Cache"),

    /**
     * Loader related logs
     */
    LOADER("AniFlux.Loader"),

    /**
     * Manager related logs
     */
    MANAGER("AniFlux.Manager"),

    /**
     * General framework logs
     */
    GENERAL("AniFlux");

}

