package com.sceyt.chatuikit.config

import android.net.Uri
import com.sceyt.chatuikit.presentation.helpers.DeepLinkUriBuilder

/**
 * Configuration for generating or parsing channel invite deep links.
 *
 * Example result: https://link.sceyt.com/join/abc123
 */
data class ChannelInviteDeepLinkConfig(
        /**
         * The URL scheme used in the deep link (e.g., "https" or a custom scheme like "sceyt").
         */
        val scheme: String,

        /**
         * The host or domain name for the deep link.
         * Example: "link.sceyt.com"
         */
        val host: String,

        /**
         * The path prefix appended after the host that identifies the invite route.
         * Example: "/join/" → resulting link becomes https://link.sceyt.com/join/{inviteCode}
         */
        val pathPrefix: String,
) {
    /**
     * The full URL prefix composed of [scheme], [host], and [pathPrefix].
     * Example: "https://link.sceyt.com/join/"
     */

    fun buildInviteUrl(inviteKey: String): Uri {
        return DeepLinkUriBuilder()
            .scheme(scheme)
            .host(host)
            .paths(pathPrefix, inviteKey)
            .build()
    }
}
