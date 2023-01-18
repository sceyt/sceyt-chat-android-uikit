package com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.DialogActionsBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.setTextViewsDrawableColor
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.ActionDialog.Action.Save
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.ActionDialog.Action.Share
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.FileType.Image
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class ActionDialog(
        context: Context,
        private val file: MediaFile,
        var listener: ((Action) -> Unit)? = null,
) : Dialog(context, R.style.SceytDialogNoTitle) {
    private lateinit var binding: DialogActionsBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogActionsBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        binding.setupStyle()
        initView()

        window?.let {
            it.setWindowAnimations(R.style.SceytDialogFromBottomAnimation)
            val wlp: WindowManager.LayoutParams = it.attributes
            wlp.gravity = Gravity.BOTTOM
            wlp.y = 30
            it.attributes = wlp
        }
    }

    private fun initView() {
        val fileTypeTitle = (context.getString(if (file.type == Image) R.string.sceyt_image else R.string.sceyt_video)).lowercase()
        binding.share.text = context.getString(R.string.sceyt_share).plus(" ").plus(fileTypeTitle)
        binding.share.setOnClickListener {
            listener?.invoke(Share)
            dismiss()
        }

        binding.save.text = context.getString(R.string.sceyt_save).plus(" ").plus(fileTypeTitle)
        binding.save.setOnClickListener {
            listener?.invoke(Save)
            dismiss()
        }

//        binding.forward.setOnClickListener {
//            listener?.invoke(Forward)
//            dismiss()
//        }

    }

    private fun DialogActionsBinding.setupStyle() {
        setTextViewsDrawableColor(listOf(save, share), context.getCompatColor(SceytKitConfig.sceytColorAccent))
    }

    enum class Action {
        Save, Forward, Share
    }
}
