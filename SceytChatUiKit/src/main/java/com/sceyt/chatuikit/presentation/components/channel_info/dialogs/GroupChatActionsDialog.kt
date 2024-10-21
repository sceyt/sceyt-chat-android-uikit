package com.sceyt.chatuikit.presentation.components.channel_info.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.view.isVisible
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytDialogGroupChannelActionsBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.setTextViewsDrawableColor
import com.sceyt.chatuikit.extensions.setTextViewsTextColor
import com.sceyt.chatuikit.persistence.extensions.getChannelType

class GroupChatActionsDialog(context: Context) : Dialog(context, R.style.SceytDialogNoTitle95) {
    private lateinit var binding: SceytDialogGroupChannelActionsBinding
    private var listener: ((ActionsEnum) -> Unit)? = null
    private lateinit var channel: SceytChannel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(SceytDialogGroupChannelActionsBinding.inflate(LayoutInflater.from(context)).also {
            binding = it
        }.root)

        binding.initView()
        binding.applyStyle()
        window?.let {
            it.setWindowAnimations(R.style.SceytDialogFromBottomAnimation)
            val wlp: WindowManager.LayoutParams = it.attributes
            wlp.gravity = Gravity.BOTTOM
            wlp.y = 30
            it.attributes = wlp
        }
        determinateState()
    }

    private fun setChannel(channel: SceytChannel) {
        this.channel = channel
    }

    private fun SceytDialogGroupChannelActionsBinding.initView() {
        when (channel.getChannelType()) {
            ChannelTypeEnum.Group -> {
                leaveChat.text = context.getString(R.string.sceyt_leave_group)
                delete.text = context.getString(R.string.sceyt_delete_group)
                report.isVisible = false
            }

            ChannelTypeEnum.Public -> {
                leaveChat.text = context.getString(R.string.sceyt_leave_channel)
                delete.text = context.getString(R.string.sceyt_delete_channel)
            }

            else -> {}
        }

        clearHistory.setOnClickListener {
            listener?.invoke(ActionsEnum.ClearHistory)
            dismiss()
        }

        leaveChat.setOnClickListener {
            listener?.invoke(ActionsEnum.Leave)
            dismiss()
        }

        delete.setOnClickListener {
            listener?.invoke(ActionsEnum.Delete)
            dismiss()
        }

        pin.setOnClickListener {
            listener?.invoke(ActionsEnum.Pin)
            dismiss()
        }

        unPin.setOnClickListener {
            listener?.invoke(ActionsEnum.Unpin)
            dismiss()
        }
    }

    private fun determinateState() {
        val myRole = channel.userRole
        val enabledActions = myRole == RoleTypeEnum.Owner.value
        with(binding) {
            delete.isVisible = enabledActions
            pin.isVisible = !channel.pinned
            unPin.isVisible = channel.pinned
        }
    }

    fun setChooseTypeCb(cb: (ActionsEnum) -> Unit) {
        listener = cb
    }

    enum class ActionsEnum {
        ClearHistory, Leave, Delete, Pin, Unpin
    }

    private fun SceytDialogGroupChannelActionsBinding.applyStyle() {
        setTextViewsDrawableColor(listOf(pin, unPin, clearHistory, report),
            context.getCompatColor(SceytChatUIKit.theme.colors.accentColor))
        setTextViewsDrawableColor(listOf(leaveChat, delete),
            context.getCompatColor(SceytChatUIKit.theme.colors.warningColor))
        setTextViewsTextColor(listOf(pin, unPin, clearHistory, report),
            context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor))
        setTextViewsTextColor(listOf(leaveChat, delete),
            context.getCompatColor(SceytChatUIKit.theme.colors.warningColor))
    }

    companion object {
        fun newInstance(context: Context, channel: SceytChannel): GroupChatActionsDialog {
            val dialog = GroupChatActionsDialog(context)
            dialog.setChannel(channel)
            return dialog
        }
    }
}