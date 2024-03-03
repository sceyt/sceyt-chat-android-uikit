package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.SceytDialogMuteNotificationsBinding

class MuteNotificationDialog(
        context: Context,
) : Dialog(context, R.style.SceytDialogNoTitle) {
    private lateinit var binding: SceytDialogMuteNotificationsBinding
    private var chooseListener: ((MuteTypeEnum) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SceytDialogMuteNotificationsBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        initView()
        window?.setWindowAnimations(R.style.SceytDialogWindowAnimation)
    }

    private fun initView() {
        binding.muteOneHour.setOnClickListener {
            chooseListener?.invoke(MuteTypeEnum.Mute1Hour)
            dismiss()
        }
        binding.muteTwoHour.setOnClickListener {
            chooseListener?.invoke(MuteTypeEnum.Mute8Hour)
            dismiss()
        }
        binding.muteForever.setOnClickListener {
            chooseListener?.invoke(MuteTypeEnum.MuteForever)
            dismiss()
        }
    }

    fun setChooseListener(listener: (MuteTypeEnum) -> Unit): MuteNotificationDialog {
        chooseListener = listener
        return this
    }

    fun setTitles(title: String) {
        binding.tvTitle.text = title
    }
}