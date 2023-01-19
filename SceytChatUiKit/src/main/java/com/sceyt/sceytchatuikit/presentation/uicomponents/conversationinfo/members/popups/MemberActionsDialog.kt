package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.popups

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.view.isVisible
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.databinding.DialogMembrerActionsBinding
import com.sceyt.sceytchatuikit.di.SceytKoinComponent

class MemberActionsDialog(context: Context) : Dialog(context, R.style.SceytDialogNoTitle95), SceytKoinComponent {
    private lateinit var binding: DialogMembrerActionsBinding
    private var listener: ((ActionsEnum) -> Unit)? = null
    private lateinit var member: SceytMember
    private var currentIsOwner: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(DialogMembrerActionsBinding.inflate(LayoutInflater.from(context)).also {
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

    private fun DialogMembrerActionsBinding.initView() {
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
        val enableRevokeAdmin = member.role.name == RoleTypeEnum.Admin.toString() && currentIsOwner
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