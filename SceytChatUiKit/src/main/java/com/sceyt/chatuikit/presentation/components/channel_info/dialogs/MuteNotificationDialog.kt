package com.sceyt.chatuikit.presentation.components.channel_info.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.IntervalOption
import com.sceyt.chatuikit.databinding.SceytDialogMuteNotificationsBinding
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.presentation.common.IntervalOptionsAdapter

class MuteNotificationDialog(
        context: Context,
) : Dialog(context, R.style.SceytDialogNoTitle) {
    private lateinit var binding: SceytDialogMuteNotificationsBinding
    private var chooseListener: ((IntervalOption) -> Unit)? = null
    private var title: String = ""
    private var options: List<IntervalOption> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SceytDialogMuteNotificationsBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        binding.applyStyle()
        binding.initViews()
        setOptionsAdapter()
        window?.setWindowAnimations(R.style.SceytDialogWindowAnimation)
    }

    private fun SceytDialogMuteNotificationsBinding.initViews() {
        tvTitle.text = title
    }

    private fun setOptionsAdapter() {
        binding.rvOptions.adapter = IntervalOptionsAdapter(options) {
            chooseListener?.invoke(it)
            dismiss()
        }
    }

    private fun setOptions(options: List<IntervalOption>) {
        this.options = options
    }

    private fun setTitles(title: String) {
        this.title = title
    }

    fun setChooseListener(listener: (IntervalOption) -> Unit): MuteNotificationDialog {
        chooseListener = listener
        return this
    }

    private fun SceytDialogMuteNotificationsBinding.applyStyle() {
        tvTitle.setTextColorRes(SceytChatUIKit.theme.colors.textPrimaryColor)
    }

    companion object {
        fun showDialog(
                context: Context,
                title: String,
                options: List<IntervalOption>,
                listener: (IntervalOption) -> Unit
        ) {
            MuteNotificationDialog(context).apply {
                setTitles(title)
                setOptions(options)
                setChooseListener(listener)
            }.show()
        }
    }
}