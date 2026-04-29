package com.sceyt.chatuikit.data.models.channels

/**
 * Keep activity/fragment navigation payloads compact to avoid Binder transaction limits.
 */
fun SceytChannel.toIntentPayload(): SceytChannel {
    return copy(
        messages = null,
        members = members?.take(30),
        newReactions = null,
        pendingReactions = null,
        events = null
    )
}
