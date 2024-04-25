package com.sceyt.chatuikit.presentation.uicomponents.conversation.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytDialogDeleteMessageBinding
import com.sceyt.chatuikit.extensions.getCompatColor

class DeleteMessageDialog(context: Context) : Dialog(context, R.style.SceytDialogNoTitle) {
    private lateinit var binding: SceytDialogDeleteMessageBinding
    private var positiveClickListener: ((Boolean) -> Unit)? = null
    private var deleteMessageCount: Int = 1
    private var requireForMe: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SceytDialogDeleteMessageBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        binding.initView()
        binding.applyStyle()
        window?.setWindowAnimations(R.style.SceytDialogWindowAnimation)
    }

    private fun SceytDialogDeleteMessageBinding.initView() {
        if (deleteMessageCount > 1) {
            textTitle.text = context.getString(R.string.sceyt_delete_messages_title)
            textDescription.text = context.getString(R.string.sceyt_delete_messages_body)
        } else {
            textTitle.text = context.getString(R.string.sceyt_delete_message_title)
            textDescription.text = context.getString(R.string.sceyt_delete_message_body)
        }
        checkbox.isVisible = !requireForMe

        buttonCancel.text = context.getString(R.string.sceyt_cancel)

        buttonDelete.setOnClickListener {
            val deleteForMe = !checkbox.isChecked || requireForMe
            positiveClickListener?.invoke(deleteForMe)
            dismiss()
        }

        buttonCancel.setOnClickListener {
            dismiss()
        }
    }

    fun setAcceptCallback(callback: (forMe: Boolean) -> Unit): DeleteMessageDialog {
        positiveClickListener = callback
        return this
    }

    fun setDeleteMessagesCount(count: Int): DeleteMessageDialog {
        deleteMessageCount = count
        return this
    }

    fun setRequireForMe(requireForMe: Boolean): DeleteMessageDialog {
        this.requireForMe = requireForMe
        return this
    }

    private fun SceytDialogDeleteMessageBinding.applyStyle() {
        buttonDelete.setTextColor(context.getCompatColor(SceytChatUIKit.theme.accentColor))
        buttonCancel.setTextColor(context.getCompatColor(SceytChatUIKit.theme.accentColor))
    }
}