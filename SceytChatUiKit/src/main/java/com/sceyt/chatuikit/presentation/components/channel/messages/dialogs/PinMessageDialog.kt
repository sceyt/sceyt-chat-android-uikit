package com.sceyt.chatuikit.presentation.components.channel.messages.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytDialogPinMessageBinding
import com.sceyt.chatuikit.styles.common.DialogStyle

class PinMessageDialog(
    context: Context,
    private val listener: ((PinType) -> Unit)? = null,
) : Dialog(context, R.style.SceytDialogNoTitle95) {

    private lateinit var binding: SceytDialogPinMessageBinding
    private val style = DialogStyle.default(context)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SceytDialogPinMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.applyStyle()
        initView()

        window?.setWindowAnimations(R.style.SceytDialogWindowAnimation)
    }

    private fun initView() {
        binding.pinForAll.setOnClickListener {
            listener?.invoke(PinType.SHARED)
            dismiss()
        }

        binding.pinForMe.setOnClickListener {
            listener?.invoke(PinType.PERSONAL)
            dismiss()
        }
    }

    private fun SceytDialogPinMessageBinding.applyStyle() {
        style.backgroundStyle.apply(root)
        with(style.optionButtonStyle) {
            apply(pinForAll)
            apply(pinForMe)
        }
    }
}
