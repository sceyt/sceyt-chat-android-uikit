package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.SceytDialogEditAvatarTypeBinding

class EditAvatarTypeDialog(
        context: Context,
        private val enableDelete: Boolean,
        private val chooseListener: ((EditAvatarType) -> Unit)? = null,
) : Dialog(context, R.style.SceytDialogNoTitle) {
    private lateinit var binding: SceytDialogEditAvatarTypeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SceytDialogEditAvatarTypeBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        initView()
        window?.setWindowAnimations(R.style.SceytDialogWindowAnimation)
    }

    private fun initView() {
        binding.tvDelete.isVisible = enableDelete

        binding.tvTakePhoto.setOnClickListener {
            chooseListener?.invoke(EditAvatarType.TakePhoto)
            dismiss()
        }
        binding.tvUploadFromGallery.setOnClickListener {
            chooseListener?.invoke(EditAvatarType.ChooseFromGallery)
            dismiss()
        }
        binding.tvDelete.setOnClickListener {
            chooseListener?.invoke(EditAvatarType.Delete)
            dismiss()
        }
    }

    enum class EditAvatarType {
        ChooseFromGallery, TakePhoto, Delete
    }
}