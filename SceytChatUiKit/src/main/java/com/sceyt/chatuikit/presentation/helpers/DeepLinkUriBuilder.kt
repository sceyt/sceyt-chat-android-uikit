package com.sceyt.chatuikit.presentation.helpers

import android.net.Uri

/**
 * Generic helper to safely build deep link URIs.
 *
 * Usage:
 * val uri = DeepLinkUriBuilder()
 *      .scheme("https")
 *      .host("link.sceyt.com")
 *      .path("/join/")
 *      .queryParam("ref", "123")
 *      .build()
 */
class DeepLinkUriBuilder {

    private var scheme: String = "https"
    private var host: String = ""
    private val pathSegments = mutableListOf<String>()
    private val queryParams = mutableMapOf<String, String>()

    /** Set the scheme (https, sceyt, etc.) */
    fun scheme(value: String) = apply {
        scheme = value.trim().removePrefix("/").removeSuffix("/")
    }

    /** Set the host/domain */
    fun host(value: String) = apply {
        host = value.trim().removePrefix("/").removeSuffix("/")
    }

    /** Add a path segment (trims slashes automatically) */
    fun path(value: String) = apply {
        value.trim()
            .removePrefix("/")
            .removeSuffix("/")
            .takeIf { it.isNotEmpty() }
            ?.let { pathSegments.add(it) }
    }

    /** Add multiple path segments at once */
    fun paths(vararg values: String) = apply {
        values.forEach { path(it) }
    }

    /** Add a query parameter */
    fun queryParam(key: String, value: String) = apply {
        queryParams[key] = value
    }

    /** Build the Uri object */
    fun build(): Uri {
        require(scheme.isNotBlank()) { "Scheme cannot be blank" }
        require(host.isNotBlank()) { "Host cannot be blank" }

        val builder = Uri.Builder()
            .scheme(scheme)
            .authority(host)

        pathSegments.forEach { builder.appendPath(it) }
        queryParams.forEach { (k, v) -> builder.appendQueryParameter(k, v) }

        return builder.build()
    }

    /** Convenience: build as string */
    fun buildUrl(): String = build().toString()
}
