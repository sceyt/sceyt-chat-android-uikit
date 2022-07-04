package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.DialogMuteNotificationsBinding

class MuteNotificationDialog(
        context: Context,
        private val chooseListener: ((MuteTypeEnum) -> Unit)? = null,
) : Dialog(context, R.style.SceytDialogNoTitle) {
    private lateinit var mBinding: DialogMuteNotificationsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DialogMuteNotificationsBinding.inflate(LayoutInflater.from(context))
        setContentView(mBinding.root)
        initView()
        window?.setWindowAnimations(R.style.SceytDialogWindowAnimation)
    }

    private fun initView() {
        mBinding.muteOneHour.setOnClickListener {
            chooseListener?.invoke(MuteTypeEnum.Mute1Hour)
            dismiss()
        }
        mBinding.muteTwoHour.setOnClickListener {
            chooseListener?.invoke(MuteTypeEnum.Mute2Hour)
            dismiss()
        }
        mBinding.muteOneDay.setOnClickListener {
            chooseListener?.invoke(MuteTypeEnum.Mute1Day)
            dismiss()
        }
        mBinding.muteForever.setOnClickListener {
            chooseListener?.invoke(MuteTypeEnum.MuteForever)
            dismiss()
        }
    }
}