package com.sceyt.chat.demo.presentation.main.profile.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.sceyt.chat.demo.databinding.LogoutDialogBinding
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.styles.common.DialogStyle

open class LogoutDialog(context: Context) : Dialog(context, R.style.SceytDialogStyle) {
    private lateinit var binding: LogoutDialogBinding
    private val style = DialogStyle.default(context)
    private var positiveClickListener: ((Boolean) -> Unit)? = null
    private var isDemoUser: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LogoutDialogBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        binding.initView()
        binding.applyStyle()
        window?.setWindowAnimations(R.style.SceytDialogWindowAnimation)
    }

    protected open fun LogoutDialogBinding.initView() {
        checkbox.isVisible = !isDemoUser
        buttonDelete.setOnClickListener {
            val deleteUser = checkbox.isChecked
            positiveClickListener?.invoke(deleteUser)
            dismiss()
        }

        buttonCancel.setOnClickListener {
            dismiss()
        }
    }

    fun setAcceptCallback(callback: (deleteUser: Boolean) -> Unit): LogoutDialog {
        positiveClickListener = callback
        return this
    }

    fun setIsDemoUser(isDemoUser: Boolean): LogoutDialog {
        this.isDemoUser = isDemoUser
        return this
    }

    protected open fun LogoutDialogBinding.applyStyle() {
        style.backgroundStyle.apply(root)
        style.titleStyle.apply(textTitle)
        style.subtitleStyle.apply(textDescription)
        style.checkboxStyle.apply(checkbox)
        style.positiveButtonStyle.apply(buttonDelete)
        style.negativeButtonStyle.apply(buttonCancel)
    }
}