package com.sceyt.chatuikit.config

import android.content.Context
import androidx.annotation.IntRange
import com.sceyt.chat.models.channel.ChannelListQuery.ChannelListOrder
import com.sceyt.chatuikit.config.defaults.DefaultAutoDeleteMessagesOptions
import com.sceyt.chatuikit.config.defaults.DefaultAvatarBackgroundColors
import com.sceyt.chatuikit.config.defaults.DefaultMuteNotificationOptions
import com.sceyt.chatuikit.persistence.lazyVar
import kotlin.time.Duration.Companion.hours

class SceytChatUIKitConfig(
        appContext: Context
) {
    var queryLimits: QueryLimits = QueryLimits()
    var presenceConfig: PresenceConfig = PresenceConfig()
    var channelURIConfig: ChannelURIConfig = ChannelURIConfig()
    var channelTypesConfig: ChannelTypesConfig = ChannelTypesConfig()
    var memberRolesConfig: MemberRolesConfig = MemberRolesConfig()
    var syncChannelsAfterConnect: Boolean = true
    var hardDeleteMessageForAll: Boolean = false
    var messageEditTimeout: Long = 2.hours.inWholeMilliseconds
    var preventDuplicateAttachmentUpload: Boolean = true
    var avatarResizeConfig: ResizeConfig = ResizeConfig.Low
    var imageAttachmentResizeConfig: ResizeConfig = ResizeConfig.Medium
    var videoAttachmentResizeConfig: VideoResizeConfig = VideoResizeConfig.Medium
    var channelListOrder: ChannelListOrder = ChannelListOrder.ListQueryChannelOrderLastMessage
    var defaultReactions: List<String> = listOf("😎", "😂", "👌", "😍", "👍", "😏")
    var mentionTriggerPrefix = '@'
    var uploadNotificationPendingIntentData: UploadNotificationPendingIntentData? = null
    var muteChannelNotificationOptions: List<IntervalOption> by lazyVar {
        DefaultMuteNotificationOptions(appContext).options
    }
    var messageAutoDeleteOptions: List<IntervalOption> by lazyVar {
        DefaultAutoDeleteMessagesOptions(appContext).options
    }
    var defaultAvatarBackgroundColors: List<Int> by lazyVar {
        DefaultAvatarBackgroundColors(appContext).colors
    }

    @IntRange(from = 1, to = 6)
    var messageReactionPerUserLimit: Int = 6

    @IntRange(from = 1, to = 50)
    var messageMultiselectLimit: Int = 30

    @IntRange(from = 1, to = 50)
    var attachmentSelectionLimit: Int = 20
}