package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.view.isVisible
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.SceytDialogEditAvatarTypeBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.setTextViewsDrawableColor
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

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
        binding.initView()
        binding.setupStyle()

        window?.let {
            it.setWindowAnimations(R.style.SceytDialogFromBottomAnimation)
            val wlp: WindowManager.LayoutParams = it.attributes
            wlp.gravity = Gravity.BOTTOM
            wlp.y = 30
            it.attributes = wlp
        }
    }

    private fun SceytDialogEditAvatarTypeBinding.initView() {
        tvDelete.isVisible = enableDelete

        tvTakePhoto.setOnClickListener {
            chooseListener?.invoke(EditAvatarType.TakePhoto)
            dismiss()
        }

        tvGallery.setOnClickListener {
            chooseListener?.invoke(EditAvatarType.ChooseFromGallery)
            dismiss()
        }

        tvDelete.setOnClickListener {
            chooseListener?.invoke(EditAvatarType.Delete)
            dismiss()
        }
    }

    enum class EditAvatarType {
        ChooseFromGallery, TakePhoto, Delete
    }

    private fun SceytDialogEditAvatarTypeBinding.setupStyle() {
        setTextViewsDrawableColor(listOf(tvTakePhoto, tvGallery),
            context.getCompatColor(SceytKitConfig.sceytColorAccent))
    }
}