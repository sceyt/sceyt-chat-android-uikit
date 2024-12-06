package com.sceyt.chatuikit.presentation.components.media.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytDialogMediaActionsBinding
import com.sceyt.chatuikit.presentation.components.media.dialogs.ActionDialog.Action.Forward
import com.sceyt.chatuikit.presentation.components.media.dialogs.ActionDialog.Action.Save
import com.sceyt.chatuikit.presentation.components.media.dialogs.ActionDialog.Action.Share
import com.sceyt.chatuikit.styles.DialogStyle

class ActionDialog(
        context: Context,
        var listener: ((Action) -> Unit)? = null,
) : Dialog(context, R.style.SceytDialogNoTitle95) {
    private lateinit var binding: SceytDialogMediaActionsBinding
    private val style = DialogStyle.default(context)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SceytDialogMediaActionsBinding.inflate(layoutInflater)
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
        style.backgroundStyle.apply(root)
        with(style.optionButtonStyle) {
            apply(save)
            apply(share)
            apply(forward)
        }
    }

    enum class Action {
        Save, Forward, Share
    }
}
