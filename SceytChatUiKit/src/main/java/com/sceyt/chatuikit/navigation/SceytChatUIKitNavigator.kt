package com.sceyt.chatuikit.navigation

import android.content.Context
import android.view.View
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

interface SceytChatUIKitNavigator {

    fun openChannel(context: Context, channel: SceytChannel, targetMessageId: Long? = null)

    fun openChannelInfo(context: Context, channel: SceytChannel)

    fun openGlobalSearch(context: Context, sourceView: View? = null)

    fun openStartChat(context: Context)

    fun openMediaPreview(context: Context, params: MediaPreviewParams)

    fun openSelfDestructingMediaPreview(context: Context, message: SceytMessage, attachment: SceytAttachment)

    fun openForward(context: Context, vararg messages: SceytMessage)

    fun openMessageInfo(context: Context, message: SceytMessage, itemStyle: MessageItemStyle)

    fun openPollResults(context: Context, message: SceytMessage)

    fun openCreatePoll(context: Context, channelId: Long)
}
