package com.sceyt.chatuikit.presentation.components.channel_list.channels.dialogs

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.persistence.extensions.getChannelType
import com.sceyt.chatuikit.presentation.common.SceytDialog
import com.sceyt.chatuikit.presentation.components.channel_info.dialogs.AutoDeleteDialog
import com.sceyt.chatuikit.presentation.components.channel_info.dialogs.MuteNotificationDialog

object ChannelActionConfirmationWithDialog {

    fun confirmLeaveAction(context: Context, channel: SceytChannel, action: () -> Unit) {
        val titleId: Int
        val descId: Int
        when (channel.getChannelType()) {
            ChannelTypeEnum.Group -> {
                titleId = R.string.sceyt_leave_group_title
                descId = R.string.sceyt_leave_group_desc
            }

            ChannelTypeEnum.Public -> {
                titleId = R.string.sceyt_leave_channel_title
                descId = R.string.sceyt_leave_channel_desc
            }

            else -> return
        }
        SceytDialog.showDialog(context, titleId, descId, R.string.sceyt_leave, positiveCb = {
            action()
        })
    }

    fun confirmDeleteChatAction(context: Context, channel: SceytChannel, action: () -> Unit) {
        val titleId: Int
        val descId: Int
        when (channel.getChannelType()) {
            ChannelTypeEnum.Group -> {
                titleId = R.string.sceyt_delete_group_title
                descId = R.string.sceyt_delete_group_desc
            }

            ChannelTypeEnum.Public -> {
                titleId = R.string.sceyt_delete_channel_title
                descId = R.string.sceyt_delete_channel_desc
            }

            ChannelTypeEnum.Direct -> {
                titleId = R.string.sceyt_delete_p2p_title
                descId = R.string.sceyt_delete_p2p_desc
            }
        }
        SceytDialog.showDialog(context, titleId, descId, R.string.sceyt_delete, positiveCb = {
            action()
        })
    }

    fun confirmClearHistoryAction(context: Context, channel: SceytChannel, action: () -> Unit) {
        val descId: Int = when (channel.getChannelType()) {
            ChannelTypeEnum.Direct -> R.string.sceyt_clear_direct_history_desc
            ChannelTypeEnum.Group -> R.string.sceyt_clear_private_chat_history_desc
            ChannelTypeEnum.Public -> R.string.sceyt_clear_public_chat_history_desc
        }
        SceytDialog.showDialog(context, R.string.sceyt_clear_history_title, descId, R.string.sceyt_clear, positiveCb = {
            action()
        })
    }

    fun confirmMuteUntilAction(context: Context, action: (Long) -> Unit) {
        MuteNotificationDialog.showDialog(
            context = context,
            title = context.getString(R.string.sceyt_mute_chat),
            options = SceytChatUIKit.config.muteChannelNotificationOptions.getOptions(context)) {
            action(it.timeInterval)
        }
    }

    fun confirmAutoDeleteMessages(context: Context, action: (Long) -> Unit) {
        AutoDeleteDialog(context).setChooseListener {
            action(it.timeInterval)
        }.show()
    }
}