package com.sceyt.chatuikit.presentation.common.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.TextView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytDialogViewOnceInfoBinding
import com.sceyt.chatuikit.extensions.showSafety
import com.sceyt.chatuikit.styles.common.DialogStyle

open class ViewOnceInfoDialog(
    context: Context
) : Dialog(context, R.style.SceytDialogNoTitle95) {
    private lateinit var binding: SceytDialogViewOnceInfoBinding
    private var style = DialogStyle.default(context)
    private var listener: (() -> Unit)? = null
    private var title: CharSequence = ""
    private var description: CharSequence = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(SceytDialogViewOnceInfoBinding.inflate(LayoutInflater.from(context)).also {
            binding = it
        }.root)

        binding.initView()
        binding.applyStyle()
        setCanceledOnTouchOutside(false)
        window?.let {
            it.setWindowAnimations(R.style.SceytDialogFromBottomAnimation)
            val wlp: WindowManager.LayoutParams = it.attributes
            wlp.gravity = Gravity.BOTTOM
            wlp.y = 30
            it.attributes = wlp
        }
    }

    fun setAcceptListener(onAcceptListener: () -> Unit) {
        listener = onAcceptListener
    }

    protected open fun SceytDialogViewOnceInfoBinding.initView() {
        tvTitle.setText(title, TextView.BufferType.SPANNABLE)
        tvDescription.setText(description, TextView.BufferType.SPANNABLE)
        btnOk.setOnClickListener {
            listener?.invoke()
            dismiss()
        }
    }

    protected open fun SceytDialogViewOnceInfoBinding.applyStyle() {
        style.backgroundStyle.apply(root)
        style.titleStyle.apply(tvTitle)
        style.subtitleStyle.apply(tvDescription)
        style.positiveButtonStyle.apply(btnOk)
    }

    companion object {
        private fun defaultDialogStyle(context: Context): DialogStyle {
            val default = DialogStyle.default(context)
            return default.copy(
                positiveButtonStyle = default.positiveButtonStyle.copy(
                    textStyle = default.titleStyle.copy(
                        color = Color.WHITE
                    )
                )
            )
        }

        fun newInstance(
            context: Context,
            title: CharSequence = context.getString(R.string.view_once_messages_title),
            description: CharSequence = context.getString(R.string.view_once_messages_desc),
            acceptListener: () -> Unit = {},
            dialogStyle: DialogStyle = defaultDialogStyle(context)
        ) = ViewOnceInfoDialog(context).apply {
            this.title = title
            this.description = description
            setAcceptListener(acceptListener)
            style = dialogStyle
        }

        fun showDialog(
            context: Context,
            title: CharSequence = context.getString(R.string.view_once_messages_title),
            description: CharSequence = context.getString(R.string.view_once_messages_desc),
            acceptListener: () -> Unit = {},
            dialogStyle: DialogStyle = defaultDialogStyle(context)
        ) = newInstance(
            context = context,
            title = title,
            description = description,
            acceptListener = acceptListener,
            dialogStyle = dialogStyle
        ).showSafety()
    }
}