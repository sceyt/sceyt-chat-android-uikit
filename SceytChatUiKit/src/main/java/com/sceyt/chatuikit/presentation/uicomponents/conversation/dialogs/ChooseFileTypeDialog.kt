package com.sceyt.chatuikit.presentation.uicomponents.conversation.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytDialogChooseFileTypeBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.setTextViewsDrawableColor
import com.sceyt.chatuikit.shared.helpers.chooseAttachment.AttachmentChooseType

class ChooseFileTypeDialog(context: Context) : Dialog(context, R.style.SceytDialogNoTitle95) {
    private lateinit var binding: SceytDialogChooseFileTypeBinding
    private var chooseListener: ((AttachmentChooseType) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SceytDialogChooseFileTypeBinding.inflate(LayoutInflater.from(context))
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

    private fun SceytDialogChooseFileTypeBinding.initView() {
        takePhoto.setOnClickListener {
            chooseListener?.invoke(AttachmentChooseType.Photo)
            dismiss()
        }

        takeVideo.setOnClickListener {
            chooseListener?.invoke(AttachmentChooseType.Video)
            dismiss()
        }

        gallery.setOnClickListener {
            chooseListener?.invoke(AttachmentChooseType.Gallery)
            dismiss()
        }

        file.setOnClickListener {
            chooseListener?.invoke(AttachmentChooseType.File)
            dismiss()
        }
    }

    fun setChooseListener(listener: (AttachmentChooseType) -> Unit): ChooseFileTypeDialog {
        chooseListener = listener
        return this
    }

    private fun SceytDialogChooseFileTypeBinding.setupStyle() {
        setTextViewsDrawableColor(listOf(takePhoto, takeVideo, gallery, file),
            context.getCompatColor(SceytChatUIKit.theme.accentColor))
    }
}