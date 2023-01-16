package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.view.isVisible
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.databinding.DialogGroupChannelActionsBinding
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.presentation.common.getMyRole
import org.koin.core.component.inject

class GroupChatActionsDialog(context: Context) : Dialog(context, R.style.SceytDialogNoTitle95), SceytKoinComponent {
    private lateinit var binding: DialogGroupChannelActionsBinding
    private var listener: ((ActionsEnum) -> Unit)? = null
    private lateinit var channel: SceytChannel
    private val preferences: SceytSharedPreference by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(DialogGroupChannelActionsBinding.inflate(LayoutInflater.from(context)).also {
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
        determinateState()
    }

    private fun setChannel(channel: SceytChannel) {
        this.channel = channel
    }

    private fun DialogGroupChannelActionsBinding.initView() {
        when (channel.channelType) {
            ChannelTypeEnum.Private -> {
                binding.leaveChat.text = context.getString(R.string.sceyt_leave_group)
                binding.delete.text = context.getString(R.string.sceyt_delete_group)
            }
            ChannelTypeEnum.Public -> {
                binding.leaveChat.text = context.getString(R.string.sceyt_leave_channel)
                binding.delete.text = context.getString(R.string.sceyt_delete_channel)
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
        val myRole = channel.getMyRole(preferences.getUserId())
        val enabledActions = myRole?.name == RoleTypeEnum.Owner.value()
        binding.delete.isVisible = enabledActions
    }

    fun setChooseTypeCb(cb: (ActionsEnum) -> Unit) {
        listener = cb
    }

    enum class ActionsEnum {
        ClearHistory, Leave, Delete
    }

    companion object {
        fun newInstance(context: Context, channel: SceytChannel): GroupChatActionsDialog {
            val dialog = GroupChatActionsDialog(context)
            dialog.setChannel(channel)
            return dialog
        }
    }
}