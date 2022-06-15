package com.sceyt.chat.ui.presentation.mainactivity.profile.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.SceytDialogEditAvatarTypeBinding

class EditAvatarTypeDialog(
        context: Context,
        private val chooseListener: ((EditAvatarType) -> Unit)? = null,
) : Dialog(context, R.style.SceytDialogNoTitle) {
    private lateinit var mBinding: SceytDialogEditAvatarTypeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = SceytDialogEditAvatarTypeBinding.inflate(LayoutInflater.from(context))
        setContentView(mBinding.root)
        initView()
        window?.setWindowAnimations(R.style.SceytDialogWindowAnimation)
    }

    private fun initView() {
        mBinding.tvTakePhoto.setOnClickListener {
            chooseListener?.invoke(EditAvatarType.TakePhoto)
            dismiss()
        }
        mBinding.tvUploadFromGallery.setOnClickListener {
            chooseListener?.invoke(EditAvatarType.ChooseFromGallery)
            dismiss()
        }
        mBinding.tvDelete.setOnClickListener {
            chooseListener?.invoke(EditAvatarType.Delete)
            dismiss()
        }
    }

    enum class EditAvatarType {
        ChooseFromGallery, TakePhoto, Delete
    }
}