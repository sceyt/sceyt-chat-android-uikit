package com.sceyt.chatuikit.push

import android.content.Context

/**
 * Interface responsible for providing the information needed to register a push notifications provider.
 */
interface PushDeviceProvider {

    /**
     * Determines whether this push notification provider is supported and valid for the current device.
     *
     * @param context The application context.
     * @return `true` if the provider is valid for this device; otherwise, `false`.
     */
    fun isSupported(context: Context): Boolean

    /**
     * Asynchronously generates a [PushDevice] instance and invokes the provided callback upon completion.
     *
     * @param onDeviceGenerated A callback function to handle the generated [PushDevice].
     */
    fun generatePushDeviceAsync(onDeviceGenerated: (pushDevice: PushDevice) -> Unit)
}
