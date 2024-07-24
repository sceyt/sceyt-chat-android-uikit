package com.sceyt.chatuikit.sceytconfigs

import androidx.annotation.IntRange

data class SceytChatUIKitConfig(
        @IntRange(1, 50) val channelsLoadSize: Int = 20,
        @IntRange(1, 50) val channelMembersLoadSize: Int = 30,
        @IntRange(1, 50) val usersLoadSize: Int = 30,
        @IntRange(1, 50) val messagesLoadSize: Int = 50,
        @IntRange(1, 50) val attachmentsLoadSize: Int = 20,
        @IntRange(1, 50) val reactionsLoadSize: Int = 30,
        @IntRange(from = 1, to = 6) var maxSelfReactionsSize: Int = 6,
        @IntRange(from = 1, to = 50) var maxMultiselectMessagesCount: Int = 30,
        val sortChannelsBy: ChannelSortType = ChannelSortType.ByLastMsg,
        val presenceStatusText: String = "",
        val uploadNotificationClickHandleData: UploadNotificationClickHandleData? = null,
        val shouldHardDeleteMessageForAll: Boolean = false
)

enum class ChannelSortType {
    ByLastMsg,
    ByChannelCreatedAt
}