package com.sceyt.chat.ui.presentation.uicomponents.conversation.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.SceytDialogChooseFileTypeBinding
import com.sceyt.chat.ui.shared.helpers.chooseAttachment.AttachmentChooseType

class ChooseFileTypeDialog(
        context: Context,
        private val chooseListener: ((AttachmentChooseType) -> Unit)? = null,
) : Dialog(context, R.style.SceytDialogNoTitle) {
    private lateinit var mBinding: SceytDialogChooseFileTypeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = SceytDialogChooseFileTypeBinding.inflate(LayoutInflater.from(context))
        setContentView(mBinding.root)
        initView()
        window?.setWindowAnimations(R.style.SceytDialogWindowAnimation)
    }

    private fun initView() {
        mBinding.tvTakePhoto.setOnClickListener {
            chooseListener?.invoke(AttachmentChooseType.Camera)
            dismiss()
        }
        mBinding.tvUploadFromGallery.setOnClickListener {
            chooseListener?.invoke(AttachmentChooseType.Gallery)
            dismiss()
        }
        mBinding.tvFile.setOnClickListener {
            chooseListener?.invoke(AttachmentChooseType.File)
            dismiss()
        }
    }
}