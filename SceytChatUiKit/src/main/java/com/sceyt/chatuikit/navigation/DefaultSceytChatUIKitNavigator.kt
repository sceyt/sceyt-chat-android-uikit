package com.sceyt.chatuikit.navigation

import android.app.Activity
import android.content.Context
import android.view.View
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.components.channel.messages.ChannelActivity
import com.sceyt.chatuikit.presentation.components.channel.messages.preview.SelfDestructingMediaPreviewActivity
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelInfoActivity
import com.sceyt.chatuikit.presentation.components.create_poll.CreatePollActivity
import com.sceyt.chatuikit.presentation.components.forward.ForwardActivity
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchActivity
import com.sceyt.chatuikit.presentation.components.media.MediaPreviewActivity
import com.sceyt.chatuikit.presentation.components.message_info.MessageInfoActivity
import com.sceyt.chatuikit.presentation.components.poll_results.PollResultsActivity
import com.sceyt.chatuikit.presentation.components.startchat.StartChatActivity
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

open class DefaultSceytChatUIKitNavigator : SceytChatUIKitNavigator {

    override fun openChannel(context: Context, channel: SceytChannel, targetMessageId: Long?) {
        ChannelActivity.launch(context, channel, targetMessageId)
    }

    override fun openChannelInfo(context: Context, channel: SceytChannel) {
        ChannelInfoActivity.launch(context, channel)
    }

    override fun openGlobalSearch(context: Context, sourceView: View?) {
        val activity = context as? Activity
        if (activity != null && sourceView != null) {
            GlobalSearchActivity.launch(activity, sourceView)
        } else {
            GlobalSearchActivity.launch(context)
        }
    }

    override fun openStartChat(context: Context) {
        StartChatActivity.launch(context)
    }

    override fun openMediaPreview(context: Context, params: MediaPreviewParams) {
        when (params) {
            is MediaPreviewParams.SingleAttachment -> {
                val activity = context as? Activity
                val sourceView = params.sourceView
                if (activity != null && sourceView != null) {
                    MediaPreviewActivity.launch(
                        activity = activity,
                        attachment = params.attachment,
                        from = params.from,
                        channelId = params.channelId,
                        reversed = params.reversed,
                        showInChatChannel = params.showInChatChannel,
                        sourceView = sourceView,
                    )
                } else {
                    MediaPreviewActivity.launch(
                        context = context,
                        attachment = params.attachment,
                        from = params.from,
                        channelId = params.channelId,
                        reversed = params.reversed,
                        showInChatChannel = params.showInChatChannel,
                    )
                }
            }
            is MediaPreviewParams.PreloadedList -> {
                val activity = context as? Activity
                MediaPreviewActivity.launchWithPreloadedData(
                    activity = activity ?: error("openMediaPreview with PreloadedList requires an Activity context"),
                    items = params.items,
                    initialIndex = params.initialIndex,
                    showInChatChannel = params.showInChatChannel,
                    sourceView = params.sourceView,
                )
            }
        }
    }

    override fun openSelfDestructingMediaPreview(
        context: Context,
        message: SceytMessage,
        attachment: SceytAttachment,
    ) {
        SelfDestructingMediaPreviewActivity.launchActivity(context, message, attachment)
    }

    override fun openForward(context: Context, vararg messages: SceytMessage) {
        ForwardActivity.launch(context, *messages)
    }

    override fun openMessageInfo(context: Context, message: SceytMessage, itemStyle: MessageItemStyle) {
        MessageInfoActivity.launch(context, message, itemStyle)
    }

    override fun openPollResults(context: Context, message: SceytMessage) {
        PollResultsActivity.launch(context, message)
    }

    override fun openCreatePoll(context: Context, channelId: Long) {
        CreatePollActivity.launch(context, channelId)
    }
}
