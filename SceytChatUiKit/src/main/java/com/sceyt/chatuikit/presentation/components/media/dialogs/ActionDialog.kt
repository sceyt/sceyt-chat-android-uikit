package com.sceyt.chatuikit.presentation.components.media.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.core.view.isVisible
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytDialogMediaActionsBinding
import com.sceyt.chatuikit.presentation.components.media.dialogs.ActionDialog.Action.Forward
import com.sceyt.chatuikit.presentation.components.media.dialogs.ActionDialog.Action.Save
import com.sceyt.chatuikit.presentation.components.media.dialogs.ActionDialog.Action.ShowInChat
import com.sceyt.chatuikit.presentation.components.media.dialogs.ActionDialog.Action.Share
import com.sceyt.chatuikit.styles.common.DialogStyle

class ActionDialog(
    context: Context,
    private val showInChatVisible: Boolean = false,
    private val listener: ((Action) -> Unit)? = null,
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
        binding.showInChat.isVisible = showInChatVisible
        binding.showInChat.text = context.getString(R.string.sceyt_show_in_chat)
        binding.showInChat.setOnClickListener {
            listener?.invoke(ShowInChat)
            dismiss()
        }

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
            apply(showInChat)
            apply(save)
            apply(share)
            apply(forward)
        }
    }

    enum class Action {
        ShowInChat, Save, Forward, Share
    }
}
