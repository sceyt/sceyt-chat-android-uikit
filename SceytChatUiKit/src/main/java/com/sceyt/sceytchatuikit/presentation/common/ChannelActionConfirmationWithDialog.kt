package com.sceyt.sceytchatuikit.presentation.common

import android.content.Context
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.MuteNotificationDialog
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.MuteTypeEnum
import java.util.concurrent.TimeUnit

object ChannelActionConfirmationWithDialog {

    fun confirmLeaveAction(context: Context, channel: SceytChannel, action: () -> Unit) {
        val titleId: Int
        val descId: Int
        when (channel.getChannelType()) {
            ChannelTypeEnum.Private -> {
                titleId = R.string.sceyt_leave_group_title
                descId = R.string.sceyt_leave_group_desc
            }

            ChannelTypeEnum.Public, ChannelTypeEnum.Broadcast -> {
                titleId = R.string.sceyt_leave_channel_title
                descId = R.string.sceyt_leave_channel_desc
            }

            else -> return
        }
        SceytDialog.showSceytDialog(context, titleId, descId, R.string.sceyt_leave, positiveCb = {
            action()
        })
    }

    fun confirmDeleteChatAction(context: Context, channel: SceytChannel, action: () -> Unit) {
        val titleId: Int
        val descId: Int
        when (channel.getChannelType()) {
            ChannelTypeEnum.Private, ChannelTypeEnum.Group -> {
                titleId = R.string.sceyt_delete_group_title
                descId = R.string.sceyt_delete_group_desc
            }

            ChannelTypeEnum.Public, ChannelTypeEnum.Broadcast -> {
                titleId = R.string.sceyt_delete_channel_title
                descId = R.string.sceyt_delete_channel_desc
            }

            ChannelTypeEnum.Direct -> {
                titleId = R.string.sceyt_delete_p2p_title
                descId = R.string.sceyt_delete_p2p_desc
            }
        }
        SceytDialog.showSceytDialog(context, titleId, descId, R.string.sceyt_delete, positiveCb = {
            action()
        })
    }

    fun confirmClearHistoryAction(context: Context, channel: SceytChannel, action: () -> Unit) {
        val descId: Int = when (channel.getChannelType()) {
            ChannelTypeEnum.Direct -> R.string.sceyt_clear_direct_history_desc
            ChannelTypeEnum.Private, ChannelTypeEnum.Group -> R.string.sceyt_clear_private_chat_history_desc
            ChannelTypeEnum.Public, ChannelTypeEnum.Broadcast -> R.string.sceyt_clear_public_chat_history_desc
        }
        SceytDialog.showSceytDialog(context, R.string.sceyt_clear_history_title, descId, R.string.sceyt_clear, positiveCb = {
            action()
        })
    }

    fun confirmMuteUntilAction(context: Context, action: (Long) -> Unit) {
        MuteNotificationDialog(context).setChooseListener {
            val until = when (it) {
                MuteTypeEnum.Mute1Hour -> TimeUnit.HOURS.toMillis(1)
                MuteTypeEnum.Mute8Hour -> TimeUnit.HOURS.toMillis(8)
                MuteTypeEnum.MuteForever -> 0L
            }
            action(until)
        }.show()
    }
}