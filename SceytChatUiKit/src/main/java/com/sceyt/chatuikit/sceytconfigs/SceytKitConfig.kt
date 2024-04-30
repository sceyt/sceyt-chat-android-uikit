package com.sceyt.chatuikit.sceytconfigs

import androidx.annotation.IntRange

object SceytKitConfig {

    @JvmField
    @IntRange(1, 50)
    var CHANNELS_LOAD_SIZE = 20

    @JvmField
    @IntRange(1, 50)
    var CHANNELS_MEMBERS_LOAD_SIZE = 30

    @JvmField
    @IntRange(1, 50)
    var USERS_LOAD_SIZE = 30

    @JvmField
    @IntRange(1, 50)
    var MESSAGES_LOAD_SIZE = 50

    @JvmField
    @IntRange(1, 50)
    var ATTACHMENTS_LOAD_SIZE = 20

    @JvmField
    @IntRange(1, 50)
    var REACTIONS_LOAD_SIZE = 30

    @JvmField
    @IntRange(from = 1, to = 6)
    var MAX_SELF_REACTIONS_SIZE = 6

    @JvmField
    @IntRange(from = 1, to = 50)
    var MAX_MULTISELECT_MESSAGES_COUNT = 30

    @JvmField
    var sortChannelsBy: ChannelSortType = ChannelSortType.ByLastMsg

    @JvmField
    var presenceStatusText = ""

    @JvmField
    var backgroundUploadNotificationClickData: BackgroundUploadNotificationClickData? = null

    enum class ChannelSortType {
        ByLastMsg,
        ByChannelCreatedAt
    }
}
