package com.sceyt.chatuikit.config

import androidx.annotation.IntRange
import com.sceyt.chat.models.channel.ChannelListQuery.ChannelListOrder
import com.sceyt.chatuikit.config.defaults.DefaultAutoDeleteMessagesOptions
import com.sceyt.chatuikit.config.defaults.DefaultAvatarBackgroundColors
import com.sceyt.chatuikit.config.defaults.DefaultMuteNotificationOptions
import com.sceyt.chatuikit.persistence.lazyVar
import kotlin.time.Duration.Companion.hours

class SceytChatUIKitConfig {
    var queryLimits: QueryLimits by lazyVar { QueryLimits() }
    var presenceConfig: PresenceConfig by lazyVar { PresenceConfig() }
    var channelURIConfig: ChannelURIConfig by lazyVar { ChannelURIConfig() }
    var channelTypesConfig: ChannelTypesConfig by lazyVar { ChannelTypesConfig() }
    var memberRolesConfig: MemberRolesConfig by lazyVar { MemberRolesConfig() }
    var notificationConfig: PushNotificationConfig by lazyVar { PushNotificationConfig() }
    var voiceRecorderConfig: VoiceRecorderConfig by lazyVar { VoiceRecorderConfig() }
    var channelLinkDeepLinkConfig: ChannelInviteDeepLinkConfig? = null
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
    var muteChannelNotificationOptions: MuteNotificationOptions by lazyVar {
        DefaultMuteNotificationOptions
    }
    var messageAutoDeleteOptions: AutoDeleteMessagesOptions by lazyVar {
        DefaultAutoDeleteMessagesOptions
    }
    var defaultAvatarBackgroundColors: AvatarBackgroundColors by lazyVar {
        DefaultAvatarBackgroundColors
    }

    @IntRange(from = 1, to = 6)
    var messageReactionPerUserLimit: Int = 6

    @IntRange(from = 1, to = 50)
    var messageMultiselectLimit: Int = 30

    @IntRange(from = 1, to = 50)
    var attachmentSelectionLimit: Int = 20

    var showGroupsInCommon = false
}