package com.sceyt.chatuikit.presentation.components.channel_info.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.IntervalOption
import com.sceyt.chatuikit.databinding.SceytDialogAutoDeleteBinding
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.presentation.common.IntervalOptionsAdapter

class AutoDeleteDialog(
        context: Context,
) : Dialog(context, R.style.SceytDialogNoTitle) {
    private lateinit var binding: SceytDialogAutoDeleteBinding
    private var chooseListener: ((IntervalOption) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SceytDialogAutoDeleteBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        setOptions()
        binding.applyStyle()
        window?.setWindowAnimations(R.style.SceytDialogWindowAnimation)
    }

    private fun setOptions() {
        val options = SceytChatUIKit.config.messageAutoDeleteOptions.getOptions(context)
        binding.rvOptions.adapter = IntervalOptionsAdapter(options) {
            chooseListener?.invoke(it)
            dismiss()
        }
    }

    fun setChooseListener(listener: (IntervalOption) -> Unit): AutoDeleteDialog {
        chooseListener = listener
        return this
    }

    fun setTitles(title: String) {
        binding.tvTitle.text = title
    }

    private fun SceytDialogAutoDeleteBinding.applyStyle() {
        tvTitle.setTextColorRes(SceytChatUIKit.theme.textPrimaryColor)
    }
}