package com.sceyt.chatuikit.presentation.common

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.annotation.StringRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytDialogViewBinding
import com.sceyt.chatuikit.extensions.getCompatColor

class SceytDialog(context: Context) : Dialog(context, R.style.SceytDialogNoTitle) {
    private val binding: SceytDialogViewBinding by lazy { SceytDialogViewBinding.inflate(LayoutInflater.from(context)) }
    private var positiveClickListener: (() -> Unit)? = null
    private var negativeClickListener: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
        window?.setWindowAnimations(R.style.SceytDialogWindowAnimation)
    }

    private fun initView() {
        binding.buttonAccept.setOnClickListener {
            positiveClickListener?.invoke()
            dismiss()
        }

        binding.buttonCancel.setOnClickListener {
            negativeClickListener?.invoke()
            dismiss()
        }
    }

    fun setTitle(title: CharSequence): SceytDialog {
        binding.textTitle.text = title
        return this
    }

    fun setDescription(description: CharSequence): SceytDialog {
        binding.textDescription.text = description
        return this
    }

    fun setPositiveButtonTitle(positiveBtnTitle: CharSequence): SceytDialog {
        binding.buttonAccept.text = positiveBtnTitle
        return this
    }

    fun setNegativeButtonTitle(negativeBtnTitle: CharSequence): SceytDialog {
        binding.buttonCancel.text = negativeBtnTitle
        return this
    }

    fun setPositiveButtonTextColor(color: Int): SceytDialog {
        binding.buttonAccept.setTextColor(color)
        return this
    }

    fun setNegativeButtonTextColor(color: Int): SceytDialog {
        binding.buttonCancel.setTextColor(color)
        return this
    }

    fun setPositiveButtonClickListener(positiveClickListener: (() -> Unit)? = null): SceytDialog {
        this.positiveClickListener = positiveClickListener
        return this
    }

    fun setNegativeButtonClickListener(negativeClickListener: (() -> Unit)? = null): SceytDialog {
        this.negativeClickListener = negativeClickListener
        return this
    }

    companion object {
        private var lastDialog: SceytDialog? = null

        fun showDialog(context: Context, title: String, description: String,
                            positiveBtnTitle: String,
                            negativeBtnTitle: String = context.getString(R.string.sceyt_cancel),
                            replaceLastDialog: Boolean = true,
                            negativeCb: (() -> Unit)? = null,
                            positiveCb: (() -> Unit)? = null): SceytDialog {
            return showDialogImpl(context, title, description, positiveBtnTitle, negativeBtnTitle,
                replaceLastDialog, negativeCb, positiveCb)
        }

        fun showDialog(context: Context,
                       @StringRes titleId: Int = R.string.sceyt_empty_string,
                       @StringRes descId: Int = R.string.sceyt_empty_string,
                       @StringRes positiveBtnTitleId: Int,
                       @StringRes negativeBtnTitleId: Int = R.string.sceyt_cancel,
                       replaceLastDialog: Boolean = true,
                       negativeCb: (() -> Unit)? = null,
                       positiveCb: (() -> Unit)? = null
        ): SceytDialog {
            return showDialogImpl(context, context.getString(titleId), context.getString(descId),
                context.getString(positiveBtnTitleId), context.getString(negativeBtnTitleId),
                replaceLastDialog, negativeCb, positiveCb)
        }

        private fun showDialogImpl(
                context: Context, title: String, description: String,
                positiveBtnTitle: String,
                negativeBtnTitle: String,
                replaceLastDialog: Boolean,
                negativeCb: (() -> Unit)? = null,
                positiveCb: (() -> Unit)? = null,
        ): SceytDialog {

            if (replaceLastDialog)
                lastDialog?.dismiss()
            else lastDialog?.let {
                if (it.isShowing)
                    return it
                else lastDialog = null
            }

            return SceytDialog(context).apply {
                setTitle(title)
                setDescription(description)
                setPositiveButtonTitle(positiveBtnTitle)
                setNegativeButtonTitle(negativeBtnTitle)
                setPositiveButtonClickListener(positiveCb)
                setNegativeButtonClickListener(negativeCb)
                setPositiveButtonTextColor(context.getCompatColor(SceytChatUIKit.theme.colors.accentColor))
                setNegativeButtonTextColor(context.getCompatColor(SceytChatUIKit.theme.colors.accentColor))
                show()
                setOnDismissListener { lastDialog = null }
                lastDialog = this
            }
        }
    }
}