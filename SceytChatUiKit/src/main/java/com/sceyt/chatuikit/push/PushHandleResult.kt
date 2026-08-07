package com.sceyt.chatuikit.push

/**
 * Result of handling an incoming push notification.
 *
 * @param data the push payload this result belongs to.
 */
sealed class PushHandleResult(val data: PushData) {

    /**
     * The push was persisted successfully.
     * @param notificationScheduled true if the notification display work was enqueued.
     */
    class Handled(
        data: PushData,
        val notificationScheduled: Boolean,
    ) : PushHandleResult(data)

    /**
     * The push was ignored by the persistence layer (e.g. duplicate or outdated data).
     */
    class Skipped(data: PushData) : PushHandleResult(data)

    /**
     * Handling the push failed with an error.
     */
    class Failed(
        data: PushData,
        val throwable: Throwable,
    ) : PushHandleResult(data)
}