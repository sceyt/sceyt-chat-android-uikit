package com.sceyt.chatuikit.notifications

import android.content.Context
import com.sceyt.chatuikit.notifications.push.PushNotification
import com.sceyt.chatuikit.notifications.service.FileTransferServiceNotification
import com.sceyt.chatuikit.persistence.lazyVar

/**
 * Manages different types of notifications used in the application.
 * This class acts as a central point for accessing and configuring various notification components.
 *
 * @param context The application context used for initializing notification components.
 */
class SceytNotifications(
        private val context: Context
) {
    /**
     * Handles push notifications for the app, such as incoming messages or events.
     */
    var pushNotification: PushNotification by lazyVar {
        PushNotification(context)
    }

    /**
     * Manages notifications related to file transfers, such as uploads attachments.
     *
     * A notification is shown when a file transfer starts, as it is tied to a foreground service
     * responsible for performing the operation. This ensures that the service is less likely to be
     * terminated by the system during the transfer.
     */
    var fileTransferServiceNotification: FileTransferServiceNotification by lazyVar {
        FileTransferServiceNotification(context)
    }
}