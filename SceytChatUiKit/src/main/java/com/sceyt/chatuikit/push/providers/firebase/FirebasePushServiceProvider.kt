package com.sceyt.chatuikit.push.providers.firebase

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.push.PushDevice
import com.sceyt.chatuikit.push.PushDeviceProvider
import com.sceyt.chatuikit.push.PushServiceType

/**
 * Responsible for providing information needed to register the Firebase push notifications.
 */
class FirebasePushServiceProvider : PushDeviceProvider {
    private val firebaseMessaging: FirebaseMessaging by lazy { FirebaseMessaging.getInstance() }

    /**
     * Checks if Firebase is available on this device by verifying Google Play Services status.
     *
     * @param context The application context.
     * @return `true` if Firebase is supported; otherwise, `false`.
     */
    override fun isSupported(context: Context): Boolean {
        val result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        return result == ConnectionResult.SUCCESS
    }

    /**
     * Asynchronously generates a [PushDevice] by retrieving the Firebase token.
     *
     * @param onDeviceGenerated Callback invoked with the generated [PushDevice].
     */
    override fun generatePushDeviceAsync(onDeviceGenerated: (pushDevice: PushDevice) -> Unit) {
        firebaseMessaging.token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onDeviceGenerated(
                    PushDevice(
                        token = task.result,
                        service = PushServiceType.Fcm
                    )
                )
            } else SceytLog.e(TAG, "Failed to get Firebase token: ${task.exception?.message}")
        }
    }
}
