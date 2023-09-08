package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.view.isVisible
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.databinding.SceytDialogDirectChannelActionsBinding
import com.sceyt.sceytchatuikit.extensions.getString
import com.sceyt.sceytchatuikit.extensions.setDrawableStart
import com.sceyt.sceytchatuikit.extensions.setTextViewDrawableColor
import com.sceyt.sceytchatuikit.presentation.common.getFirstMember
import com.sceyt.sceytchatuikit.presentation.common.isPeerDeleted
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class DirectChatActionsDialog(context: Context) : Dialog(context, R.style.SceytDialogNoTitle95) {
    private lateinit var binding: SceytDialogDirectChannelActionsBinding
    private var listener: ((ActionsEnum) -> Unit)? = null
    private lateinit var channel: SceytChannel
    private var showMuteIcon: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(SceytDialogDirectChannelActionsBinding.inflate(LayoutInflater.from(context)).also {
            binding = it
        }.root)

        binding.initView()
        binding.setupStyle()
        window?.let {
            it.setWindowAnimations(R.style.SceytDialogFromBottomAnimation)
            val wlp: WindowManager.LayoutParams = it.attributes
            wlp.gravity = Gravity.BOTTOM
            wlp.y = 30
            it.attributes = wlp
        }
    }

    private fun setData(channel: SceytChannel, showStartChatIcon: Boolean) {
        this.channel = channel
        this.showMuteIcon = showStartChatIcon
    }

    fun setChooseTypeCb(cb: (ActionsEnum) -> Unit) {
        listener = cb
    }

    private fun SceytDialogDirectChannelActionsBinding.initView() {
        channel.getFirstMember()?.let {
            blockUser.isVisible = it.user.blocked.not() && !channel.isPeerDeleted()
            unBlockUser.isVisible = it.user.blocked
        }

        muteUnMute.apply {
            if (showMuteIcon) {
                if (channel.muted) {
                    text = getString(R.string.sceyt_un_mute_)
                    setDrawableStart(R.drawable.sceyt_ic_un_mute, SceytKitConfig.sceytColorAccent)
                } else {
                    text = getString(R.string.sceyt_mute_)
                    setDrawableStart(R.drawable.sceyt_ic_muted_channel, SceytKitConfig.sceytColorAccent)
                }
            }
            isVisible = showMuteIcon
        }

        muteUnMute.setOnClickListener {
            listener?.invoke(if (channel.muted) ActionsEnum.UnMute else ActionsEnum.Mute)
            dismiss()
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
    }

    enum class ActionsEnum {
        ClearHistory, BlockUser, UnBlockUser, Delete, Mute, UnMute
    }

    private fun SceytDialogDirectChannelActionsBinding.setupStyle() {
        unBlockUser.setTextViewDrawableColor(SceytKitConfig.sceytColorAccent)
    }

    companion object {
        fun newInstance(context: Context, channel: SceytChannel, showMuteIcon: Boolean): DirectChatActionsDialog {
            val dialog = DirectChatActionsDialog(context)
            dialog.setData(channel, showMuteIcon)
            return dialog
        }
    }
}