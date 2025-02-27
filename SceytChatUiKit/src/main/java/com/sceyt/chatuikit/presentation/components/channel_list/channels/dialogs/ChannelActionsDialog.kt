package com.sceyt.chatuikit.presentation.components.channel_list.channels.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.view.isVisible
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytDialogChannelActionsBinding
import com.sceyt.chatuikit.persistence.extensions.checkIsMemberInChannel
import com.sceyt.chatuikit.persistence.extensions.haveDeleteChannelPermission
import com.sceyt.chatuikit.styles.DialogStyle

open class ChannelActionsDialog(context: Context) : Dialog(context, R.style.SceytDialogNoTitle95) {
    private lateinit var binding: SceytDialogChannelActionsBinding
    private val style = DialogStyle.default(context)
    private var listener: ((ActionsEnum) -> Unit)? = null
    private lateinit var channel: SceytChannel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(SceytDialogChannelActionsBinding.inflate(LayoutInflater.from(context)).also {
            binding = it
        }.root)

        binding.initView()
        binding.applyStyle()
        binding.setIconsVisibility()
        window?.let {
            it.setWindowAnimations(R.style.SceytDialogFromBottomAnimation)
            val wlp: WindowManager.LayoutParams = it.attributes
            wlp.gravity = Gravity.BOTTOM
            wlp.y = 30
            it.attributes = wlp
        }
    }

    protected open fun setData(channel: SceytChannel) {
        this.channel = channel
    }

    fun setChooseTypeCb(cb: (ActionsEnum) -> Unit) {
        listener = cb
    }

    protected open fun SceytDialogChannelActionsBinding.initView() {
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

    protected open fun SceytDialogChannelActionsBinding.setIconsVisibility() {
        val isSelf = channel.isSelf
        markAsRead.isVisible = channel.unread || channel.newMessageCount > 0
        markAsUnRead.isVisible = !channel.unread && channel.newMessageCount == 0L
        mute.isVisible = !channel.muted && !isSelf
        unMute.isVisible = channel.muted && !isSelf
        pin.isVisible = !channel.pinned
        unPin.isVisible = channel.pinned
        leave.isVisible = channel.isGroup && channel.checkIsMemberInChannel()
        delete.isVisible = !channel.isGroup && channel.haveDeleteChannelPermission()
    }

    enum class ActionsEnum {
        Pin, UnPin, MarkAsRead, MarkAsUnRead, Mute, UnMute, Leave, Delete
    }

    protected open fun SceytDialogChannelActionsBinding.applyStyle() {
        style.backgroundStyle.apply(root)
        with(style.optionButtonStyle) {
            apply(pin)
            apply(unPin)
            apply(markAsRead)
            apply(markAsUnRead)
            apply(mute)
            apply(unMute)
        }
        with(style.warningOptionButtonStyle) {
            apply(delete)
            apply(leave)
        }
    }

    companion object {
        fun newInstance(context: Context, channel: SceytChannel): ChannelActionsDialog {
            val dialog = ChannelActionsDialog(context)
            dialog.setData(channel)
            return dialog
        }
    }
}