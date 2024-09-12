package com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytDialogAutoDeleteBinding
import com.sceyt.chatuikit.databinding.SceytDialogMuteNotificationsBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.setTextViewsTextColor

class AutoDeleteDialog(
        context: Context,
) : Dialog(context, R.style.SceytDialogNoTitle) {
    private lateinit var binding: SceytDialogAutoDeleteBinding
    private var chooseListener: ((AutoDeleteType) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SceytDialogAutoDeleteBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        initView()
        binding.applyStyle()
        window?.setWindowAnimations(R.style.SceytDialogWindowAnimation)
    }

    private fun initView() {
        binding.deleteOneDay.setOnClickListener {
            chooseListener?.invoke(AutoDeleteType.Delete1Day)
            dismiss()
        }
        binding.deleteOneWeek.setOnClickListener {
            chooseListener?.invoke(AutoDeleteType.Delete1Week)
            dismiss()
        }
        binding.deleteOneMonth.setOnClickListener {
            chooseListener?.invoke(AutoDeleteType.Delete1Month)
            dismiss()
        }
        binding.deleteOff.setOnClickListener {
            chooseListener?.invoke(AutoDeleteType.DeleteOff)
            dismiss()
        }
    }

    fun setChooseListener(listener: (AutoDeleteType) -> Unit): AutoDeleteDialog {
        chooseListener = listener
        return this
    }

    fun setTitles(title: String) {
        binding.tvTitle.text = title
    }

    private fun SceytDialogAutoDeleteBinding.applyStyle() {
        setTextViewsTextColor(listOf(tvTitle, deleteOneDay, deleteOneWeek, deleteOneMonth, deleteOff),
            context.getCompatColor(SceytChatUIKit.theme.textPrimaryColor))
    }
}