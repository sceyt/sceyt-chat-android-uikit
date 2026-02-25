package com.sceyt.chatuikit.data.models.channels

/**
 * Keep activity/fragment navigation payloads compact to avoid Binder transaction limits.
 */
fun SceytChannel.toIntentPayload(): SceytChannel {
    return copy(
        messages = null,
        members = if (isGroup) null else members,
        newReactions = null,
        pendingReactions = null,
        events = null
    )
}
