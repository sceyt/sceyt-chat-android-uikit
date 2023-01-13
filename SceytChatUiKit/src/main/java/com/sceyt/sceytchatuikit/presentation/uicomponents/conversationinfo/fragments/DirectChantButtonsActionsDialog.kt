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
import com.sceyt.sceytchatuikit.databinding.DialogChannelActionsBinding

class DirectChantButtonsActionsDialog(context: Context) : Dialog(context, R.style.SceytDialogNoTitle95) {
    private lateinit var binding: DialogChannelActionsBinding
    private var listener: ((ActionsEnum) -> Unit)? = null
    private lateinit var channel: SceytDirectChannel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(DialogChannelActionsBinding.inflate(LayoutInflater.from(context)).also {
            binding = it
        }.root)

        binding.initView()
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

    private fun DialogChannelActionsBinding.initView() {
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

    companion object {
        fun newInstance(context: Context, channel: SceytDirectChannel): DirectChantButtonsActionsDialog {
            val dialog = DirectChantButtonsActionsDialog(context)
            dialog.setChannel(channel)
            return dialog
        }
    }
}