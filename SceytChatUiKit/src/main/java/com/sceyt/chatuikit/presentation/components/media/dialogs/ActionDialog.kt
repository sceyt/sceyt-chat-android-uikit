package com.sceyt.chatuikit.presentation.components.media.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytDialogMediaActionsBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.setTextViewsDrawableColor
import com.sceyt.chatuikit.presentation.components.media.dialogs.ActionDialog.Action.Forward
import com.sceyt.chatuikit.presentation.components.media.dialogs.ActionDialog.Action.Save
import com.sceyt.chatuikit.presentation.components.media.dialogs.ActionDialog.Action.Share

class ActionDialog(
        context: Context,
        var listener: ((Action) -> Unit)? = null
) : Dialog(context, R.style.SceytDialogNoTitle95) {
    private lateinit var binding: SceytDialogMediaActionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SceytDialogMediaActionsBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        binding.applyStyle()
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
        binding.share.text = context.getString(R.string.sceyt_share)
        binding.share.setOnClickListener {
            listener?.invoke(Share)
            dismiss()
        }

        binding.save.text = context.getString(R.string.sceyt_save)
        binding.save.setOnClickListener {
            listener?.invoke(Save)
            dismiss()
        }

        binding.forward.setOnClickListener {
            listener?.invoke(Forward)
            dismiss()
        }
    }

    private fun SceytDialogMediaActionsBinding.applyStyle() {
        setTextViewsDrawableColor(listOf(save, share, forward), context.getCompatColor(SceytChatUIKit.theme.colors.accentColor))
    }

    enum class Action {
        Save, Forward, Share
    }
}
