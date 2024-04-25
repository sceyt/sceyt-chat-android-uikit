package com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.dialogs

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
import com.sceyt.chatuikit.databinding.SceytDialogDirectChannelActionsBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.setTextViewsDrawableColor
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isPeerDeleted
import com.sceyt.chatuikit.persistence.extensions.isSelf

class DirectChatActionsDialog(context: Context) : Dialog(context, R.style.SceytDialogNoTitle95) {
    private lateinit var binding: SceytDialogDirectChannelActionsBinding
    private var listener: ((ActionsEnum) -> Unit)? = null
    private lateinit var channel: SceytChannel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(SceytDialogDirectChannelActionsBinding.inflate(LayoutInflater.from(context)).also {
            binding = it
        }.root)

        binding.initView()
        binding.applyStyle()
        binding.determinateState()
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

    private fun SceytDialogDirectChannelActionsBinding.initView() {
        if (channel.isSelf()) {
            blockUser.isVisible = false
            unBlockUser.isVisible = false
        } else
            channel.getPeer()?.let {
                blockUser.isVisible = it.user.blocked.not() && !channel.isPeerDeleted()
                unBlockUser.isVisible = it.user.blocked
            }

        clearHistory.setOnClickListener {
            listener?.invoke(ActionsEnum.ClearHistory)
            dismiss()
        }

        blockUser.setOnClickListener {
            listener?.invoke(ActionsEnum.BlockUser)
            dismiss()
        }

        unBlockUser.setOnClickListener {
            listener?.invoke(ActionsEnum.UnBlockUser)
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
            listener?.invoke(ActionsEnum.UnPin)
            dismiss()
        }
    }

    private fun SceytDialogDirectChannelActionsBinding.determinateState() {
        if (channel.isSelf()) {
            blockUser.isVisible = false
            unBlockUser.isVisible = false
        } else
            channel.getPeer()?.let {
                blockUser.isVisible = it.user.blocked.not() && !channel.isPeerDeleted()
                unBlockUser.isVisible = it.user.blocked
            }

        pin.isVisible = !channel.pinned
        unPin.isVisible = channel.pinned
    }

    enum class ActionsEnum {
        ClearHistory, BlockUser, UnBlockUser, Delete, Pin, UnPin
    }

    private fun SceytDialogDirectChannelActionsBinding.applyStyle() {
        setTextViewsDrawableColor(listOf(pin, unPin, blockUser, unBlockUser, clearHistory),
            context.getCompatColor(SceytChatUIKit.theme.accentColor))
    }

    companion object {
        fun newInstance(context: Context, channel: SceytChannel): DirectChatActionsDialog {
            val dialog = DirectChatActionsDialog(context)
            dialog.setData(channel)
            return dialog
        }
    }
}