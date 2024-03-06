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
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.setTextViewsDrawableColor
import com.sceyt.sceytchatuikit.presentation.common.getPeer
import com.sceyt.sceytchatuikit.presentation.common.isPeerDeleted
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

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
        binding.setupStyle()
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
    }

    enum class ActionsEnum {
        ClearHistory, BlockUser, UnBlockUser, Delete
    }

    private fun SceytDialogDirectChannelActionsBinding.setupStyle() {
        setTextViewsDrawableColor(listOf(pin, unPin, blockUser, unBlockUser, clearHistory),
            context.getCompatColor(SceytKitConfig.sceytColorAccent))
    }

    companion object {
        fun newInstance(context: Context, channel: SceytChannel): DirectChatActionsDialog {
            val dialog = DirectChatActionsDialog(context)
            dialog.setData(channel)
            return dialog
        }
    }
}