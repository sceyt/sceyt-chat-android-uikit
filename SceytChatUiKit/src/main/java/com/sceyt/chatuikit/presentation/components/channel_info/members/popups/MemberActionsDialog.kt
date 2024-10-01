package com.sceyt.chatuikit.presentation.components.channel_info.members.popups

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.view.isVisible
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.databinding.SceytDialogMembrerActionsBinding
import com.sceyt.chatuikit.koin.SceytKoinComponent

class MemberActionsDialog(context: Context) : Dialog(context, R.style.SceytDialogNoTitle95), SceytKoinComponent {
    private lateinit var binding: SceytDialogMembrerActionsBinding
    private var listener: ((ActionsEnum) -> Unit)? = null
    private lateinit var member: SceytMember
    private var currentIsOwner: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(SceytDialogMembrerActionsBinding.inflate(LayoutInflater.from(context)).also {
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

    private fun setMember(member: SceytMember, currentIsOwner: Boolean) {
        this.member = member
        this.currentIsOwner = currentIsOwner
    }

    private fun SceytDialogMembrerActionsBinding.initView() {
        revokeAdmin.setOnClickListener {
            listener?.invoke(ActionsEnum.RevokeAdmin)
            dismiss()
        }

        remove.setOnClickListener {
            listener?.invoke(ActionsEnum.Delete)
            dismiss()
        }
    }

    private fun determinateState() {
        val enableRevokeAdmin = member.role.name == RoleTypeEnum.Admin.value && currentIsOwner
        binding.revokeAdmin.isVisible = enableRevokeAdmin
    }

    fun setChooseTypeCb(cb: (ActionsEnum) -> Unit) {
        listener = cb
    }

    enum class ActionsEnum {
        RevokeAdmin, Delete
    }

    companion object {
        fun newInstance(context: Context, member: SceytMember, currentIsOwner: Boolean): MemberActionsDialog {
            val dialog = MemberActionsDialog(context)
            dialog.setMember(member, currentIsOwner)
            return dialog
        }
    }
}