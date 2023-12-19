package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.view.isVisible
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.databinding.SceytDialogGroupChannelActionsBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.setTextViewsDrawableColor
import com.sceyt.sceytchatuikit.presentation.common.getChannelType
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

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
        binding.setupStyle()
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
            ChannelTypeEnum.Private -> {
                leaveChat.text = context.getString(R.string.sceyt_leave_group)
                delete.text = context.getString(R.string.sceyt_delete_group)
                report.isVisible = false
            }

            ChannelTypeEnum.Public, ChannelTypeEnum.Broadcast -> {
                leaveChat.text = context.getString(R.string.sceyt_leave_channel)
                delete.text = context.getString(R.string.sceyt_delete_channel)
                // todo report.isVisible = true
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
    }

    private fun determinateState() {
        val myRole = channel.userRole
        val enabledActions = myRole == RoleTypeEnum.Owner.toString()
        binding.delete.isVisible = enabledActions
    }

    fun setChooseTypeCb(cb: (ActionsEnum) -> Unit) {
        listener = cb
    }

    enum class ActionsEnum {
        ClearHistory, Leave, Delete
    }

    private fun SceytDialogGroupChannelActionsBinding.setupStyle() {
        setTextViewsDrawableColor(listOf(pin, unPin, clearHistory, report),
            context.getCompatColor(SceytKitConfig.sceytColorAccent))
    }

    companion object {
        fun newInstance(context: Context, channel: SceytChannel): GroupChatActionsDialog {
            val dialog = GroupChatActionsDialog(context)
            dialog.setChannel(channel)
            return dialog
        }
    }
}