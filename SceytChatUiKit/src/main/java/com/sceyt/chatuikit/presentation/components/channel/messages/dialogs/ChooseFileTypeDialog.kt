package com.sceyt.chatuikit.presentation.components.channel.messages.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytDialogChooseFileTypeBinding
import com.sceyt.chatuikit.shared.helpers.picker.PickType
import com.sceyt.chatuikit.styles.DialogStyle

class ChooseFileTypeDialog(context: Context) : Dialog(context, R.style.SceytDialogNoTitle95) {
    private lateinit var binding: SceytDialogChooseFileTypeBinding
    private val style = DialogStyle.default(context)
    private var chooseListener: ((PickType) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SceytDialogChooseFileTypeBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        binding.initView()
        binding.applyStyle()

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
            chooseListener?.invoke(PickType.Photo)
            dismiss()
        }

        takeVideo.setOnClickListener {
            chooseListener?.invoke(PickType.Video)
            dismiss()
        }

        gallery.setOnClickListener {
            chooseListener?.invoke(PickType.Gallery)
            dismiss()
        }

        file.setOnClickListener {
            chooseListener?.invoke(PickType.File)
            dismiss()
        }
    }

    fun setChooseListener(listener: (PickType) -> Unit): ChooseFileTypeDialog {
        chooseListener = listener
        return this
    }

    private fun SceytDialogChooseFileTypeBinding.applyStyle() {
        style.backgroundStyle.apply(root)
        with(style.optionButtonStyle) {
            apply(takePhoto)
            apply(takeVideo)
            apply(gallery)
            apply(file)
        }
    }
}