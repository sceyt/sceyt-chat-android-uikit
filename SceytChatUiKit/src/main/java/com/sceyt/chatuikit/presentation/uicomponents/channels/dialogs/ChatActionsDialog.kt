package com.sceyt.chatuikit.presentation.uicomponents.channels.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.view.isVisible
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytDialogChannelActionsBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.setTextViewsDrawableColor
import com.sceyt.chatuikit.persistence.extensions.checkIsMemberInChannel
import com.sceyt.chatuikit.persistence.extensions.isSelf

class ChatActionsDialog(context: Context) : Dialog(context, R.style.SceytDialogNoTitle95) {
    private lateinit var binding: SceytDialogChannelActionsBinding
    private var listener: ((ActionsEnum) -> Unit)? = null
    private lateinit var channel: SceytChannel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(SceytDialogChannelActionsBinding.inflate(LayoutInflater.from(context)).also {
            binding = it
        }.root)

        binding.initView()
        binding.setupStyle()
        binding.setIconsVisibility()
        window?.let {
            it.setWindowAnimations(R.style.SceytDialogFromBottomAnimation)
            val wlp: WindowManager.LayoutParams = it.attributes
            wlp.gravity = Gravity.BOTTOM
            wlp.y = 30
            it.attributes = wlp
        }
    }

    private fun setData(channel: SceytChannel) {
        this.channel = channel
    }

    fun setChooseTypeCb(cb: (ActionsEnum) -> Unit) {
        listener = cb
    }

    private fun SceytDialogChannelActionsBinding.initView() {
        pin.setOnClickListener {
            listener?.invoke(ActionsEnum.Pin)
            dismiss()
        }

        unPin.setOnClickListener {
            listener?.invoke(ActionsEnum.UnPin)
            dismiss()
        }

        markAsRead.setOnClickListener {
            listener?.invoke(ActionsEnum.MarkAsRead)
            dismiss()
        }

        markAsUnRead.setOnClickListener {
            listener?.invoke(ActionsEnum.MarkAsUnRead)
            dismiss()
        }

        mute.setOnClickListener {
            listener?.invoke(ActionsEnum.Mute)
            dismiss()
        }

        unMute.setOnClickListener {
            listener?.invoke(ActionsEnum.UnMute)
            dismiss()
        }

        leave.setOnClickListener {
            listener?.invoke(ActionsEnum.Leave)
            dismiss()
        }

        delete.setOnClickListener {
            listener?.invoke(ActionsEnum.Delete)
            dismiss()
        }
    }

    private fun SceytDialogChannelActionsBinding.setIconsVisibility() {
        val isSelf = channel.isSelf()
        markAsRead.isVisible = channel.unread
        markAsUnRead.isVisible = !channel.unread
        mute.isVisible = !channel.muted && !isSelf
        unMute.isVisible = channel.muted && !isSelf
        pin.isVisible = !channel.pinned
        unPin.isVisible = channel.pinned
        leave.isVisible = channel.isGroup && channel.checkIsMemberInChannel()
        delete.isVisible = !channel.isGroup
    }

    enum class ActionsEnum {
        Pin, UnPin, MarkAsRead, MarkAsUnRead, Mute, UnMute, Leave, Delete
    }

    private fun SceytDialogChannelActionsBinding.setupStyle() {
        setTextViewsDrawableColor(listOf(pin, unPin, markAsRead, markAsUnRead, mute, unMute),
            context.getCompatColor(SceytChatUIKit.theme.accentColor))
    }

    companion object {
        fun newInstance(context: Context, channel: SceytChannel): ChatActionsDialog {
            val dialog = ChatActionsDialog(context)
            dialog.setData(channel)
            return dialog
        }
    }
}