package com.sceyt.chatuikit.config

import android.content.Context
import androidx.annotation.IntRange
import androidx.core.graphics.toColorInt
import com.sceyt.chat.models.channel.ChannelListQuery.ChannelListOrder
import com.sceyt.chatuikit.config.defaults.DefaultAutoDeleteMessagesOptions
import com.sceyt.chatuikit.config.defaults.DefaultMuteNotificationOptions
import kotlin.time.Duration.Companion.hours

class SceytChatUIKitConfig(
        appContext: Context
) {
    var queryLimits: QueryLimits = QueryLimits()
    var presenceConfig: PresenceConfig = PresenceConfig()
    var channelURIConfig: ChannelURIConfig = ChannelURIConfig()
    var channelTypesConfig: ChannelTypesConfig = ChannelTypesConfig()
    var memberRolesConfig: MemberRolesConfig = MemberRolesConfig()
    var muteChannelNotificationOptions: List<IntervalOption> = DefaultMuteNotificationOptions(appContext).options
    var messageAutoDeleteOptions: List<IntervalOption> = DefaultAutoDeleteMessagesOptions(appContext).options
    var syncChannelsAfterConnect: Boolean = true
    var hardDeleteMessageForAll: Boolean = false
    var messageEditTimeout: Long = 2.hours.inWholeMilliseconds
    var preventDuplicateAttachmentUpload: Boolean = true
    var avatarResizeConfig: ResizeConfig = ResizeConfig.Low
    var imageAttachmentResizeConfig: ResizeConfig = ResizeConfig.Medium
    var videoAttachmentResizeConfig: VideoResizeConfig = VideoResizeConfig.Medium
    var channelListOrder: ChannelListOrder = ChannelListOrder.ListQueryChannelOrderLastMessage
    var defaultAvatarBackgroundColors: List<Int> = listOf("#4F6AFF".toColorInt(), "#B463E7".toColorInt())
    var defaultReactions: List<String> = listOf("😎", "😂", "👌", "😍", "👍", "😏")
    var mentionTriggerPrefix = '@'
    var uploadNotificationPendingIntentData: UploadNotificationPendingIntentData? = null

    @IntRange(from = 1, to = 6)
    var messageReactionPerUserLimit: Int = 6

    @IntRange(from = 1, to = 50)
    var maxMultiselectMessagesCount: Int = 30

    @IntRange(from = 1, to = 50)
    var attachmentSelectionLimit: Int = 20
}