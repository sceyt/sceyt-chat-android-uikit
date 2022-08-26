package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.SceytDialogViewBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor

class DeleteMessageDialog(
        context: Context,
        private val positiveClickListener: (() -> Unit)? = null,
        private val negativeClickListener: (() -> Unit)? = null,
) : Dialog(context, R.style.SceytDialogNoTitle) {
    private lateinit var mBinding: SceytDialogViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = SceytDialogViewBinding.inflate(LayoutInflater.from(context))
        setContentView(mBinding.root)
        initView()
        window?.setWindowAnimations(R.style.SceytDialogWindowAnimation)
    }

    private fun initView() {
        mBinding.textTitle.text = context.getString(R.string.sceyt_delete_message_title)
        mBinding.textDescription.text = context.getString(R.string.sceyt_delete_message_body)

        mBinding.buttonAccept.apply {
            text = context.getString(R.string.sceyt_delete)
            setTextColor(context.getCompatColor(R.color.sceyt_color_red))
        }

        mBinding.buttonCancel.text = context.getString(R.string.sceyt_cancel)

        mBinding.buttonAccept.setOnClickListener {
            positiveClickListener?.invoke()
            dismiss()
        }

        mBinding.buttonCancel.setOnClickListener {
            negativeClickListener?.invoke()
            dismiss()
        }
    }
}