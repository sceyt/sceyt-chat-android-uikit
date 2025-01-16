package com.sceyt.chatuikit.config

import com.sceyt.chatuikit.push.PushData
import com.sceyt.chatuikit.push.PushDeviceProvider

/**
 * Configuration class for push notifications in the application.
 */
data class PushNotificationConfig(
        /**
         * Enables or disables push notifications on the device.
         * If disabled, the device's token will not be registered for notifications.
         * Default value: `true`.
         */
        val isPushEnabled: Boolean = true,

        /**
         * Suppresses push notifications when the app is in the foreground.
         * If `true`, notifications will not be displayed while the app is actively in use.
         * Default value: `false`.
         */
        val suppressWhenAppIsInForeground: Boolean = false,
        /**
         * Determines whether the app should display notifications upon receiving a push message.
         * Accepts a lambda that can include app-specific logic.
         * Default value: always `true`.
         */
        val shouldDisplayNotification: (PushData) -> Boolean = { true },

        /**
         * A list of generators responsible for providing device registration details.
         * Default value: an empty list.
         *
         * Note: The first provider that returns `true` from [PushDeviceProvider.isSupported] will be used.
         */
        val pushProviders: List<PushDeviceProvider> = emptyList()
)