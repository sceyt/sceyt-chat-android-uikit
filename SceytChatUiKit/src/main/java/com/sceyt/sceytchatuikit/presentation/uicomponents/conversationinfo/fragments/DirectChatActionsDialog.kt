package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.view.isVisible
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.channels.SceytDirectChannel
import com.sceyt.sceytchatuikit.databinding.DialogDirectChannelActionsBinding
import com.sceyt.sceytchatuikit.extensions.setTextViewDrawableColor
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class DirectChatActionsDialog(context: Context) : Dialog(context, R.style.SceytDialogNoTitle95) {
    private lateinit var binding: DialogDirectChannelActionsBinding
    private var listener: ((ActionsEnum) -> Unit)? = null
    private lateinit var channel: SceytDirectChannel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(DialogDirectChannelActionsBinding.inflate(LayoutInflater.from(context)).also {
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

    private fun setChannel(channel: SceytDirectChannel) {
        this.channel = channel
    }

    fun setChooseTypeCb(cb: (ActionsEnum) -> Unit) {
        listener = cb
    }

    private fun DialogDirectChannelActionsBinding.initView() {
        channel.peer?.let {
            blockUser.isVisible = it.user.blocked.not()
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
    }

    enum class ActionsEnum {
        ClearHistory, BlockUser, UnBlockUser, Delete
    }

    private fun DialogDirectChannelActionsBinding.setupStyle() {
        unBlockUser.setTextViewDrawableColor(SceytKitConfig.sceytColorAccent)
    }

    companion object {
        fun newInstance(context: Context, channel: SceytDirectChannel): DirectChatActionsDialog {
            val dialog = DirectChatActionsDialog(context)
            dialog.setChannel(channel)
            return dialog
        }
    }
}