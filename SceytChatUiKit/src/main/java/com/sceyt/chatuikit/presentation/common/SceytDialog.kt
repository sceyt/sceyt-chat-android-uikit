package com.sceyt.chatuikit.presentation.common

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.annotation.StringRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytDialogViewBinding
import com.sceyt.chatuikit.extensions.dismissSafety
import com.sceyt.chatuikit.styles.common.DialogStyle

class SceytDialog(context: Context) : Dialog(context, R.style.SceytDialogStyle) {
    private val binding by lazy { SceytDialogViewBinding.inflate(layoutInflater) }
    private var style = DialogStyle.default(context)
    private var positiveClickListener: (() -> Unit)? = null
    private var negativeClickListener: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
        window?.setWindowAnimations(R.style.SceytDialogWindowAnimation)
        binding.applyStyle()
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
        binding.textTitle.setText(title, TextView.BufferType.SPANNABLE)
        return this
    }

    fun setDescription(description: CharSequence): SceytDialog {
        binding.textDescription.setText(description, TextView.BufferType.SPANNABLE)
        return this
    }

    fun setPositiveButtonTitle(positiveBtnTitle: CharSequence): SceytDialog {
        binding.buttonAccept.setText(positiveBtnTitle, TextView.BufferType.SPANNABLE)
        return this
    }

    fun setNegativeButtonTitle(negativeBtnTitle: CharSequence): SceytDialog {
        binding.buttonCancel.setText(negativeBtnTitle, TextView.BufferType.SPANNABLE)
        return this
    }

    fun setPositiveButtonTextColor(color: Int): SceytDialog {
        val positiveButtonStyle = style.positiveButtonStyle
        val newStyle = positiveButtonStyle.copy(
            textStyle = positiveButtonStyle.textStyle.copy(color = color)
        )
        style = style.copy(positiveButtonStyle = newStyle)
        return this
    }

    fun setNegativeButtonTextColor(color: Int): SceytDialog {
        val negativeButtonStyle = style.negativeButtonStyle
        val newStyle = negativeButtonStyle.copy(
            textStyle = negativeButtonStyle.textStyle.copy(color = color)
        )
        style = style.copy(negativeButtonStyle = newStyle)
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

    private fun SceytDialogViewBinding.applyStyle() {
        style.backgroundStyle.apply(root)
        style.titleStyle.apply(textTitle)
        style.subtitleStyle.apply(textDescription)
        style.positiveButtonStyle.apply(buttonAccept)
        style.negativeButtonStyle.apply(buttonCancel)
    }

    companion object {
        private var lastDialog: SceytDialog? = null

        fun showDialog(
            context: Context,
            title: CharSequence,
            description: CharSequence,
            positiveBtnTitle: CharSequence,
            negativeBtnTitle: CharSequence = context.getString(R.string.sceyt_cancel),
            replaceLastDialog: Boolean = true,
            negativeCb: (() -> Unit)? = null,
            positiveCb: (() -> Unit)? = null,
        ): SceytDialog {
            return showDialogImpl(
                context = context,
                title = title,
                description = description,
                positiveBtnTitle = positiveBtnTitle,
                negativeBtnTitle = negativeBtnTitle,
                replaceLastDialog = replaceLastDialog,
                negativeCb = negativeCb,
                positiveCb = positiveCb
            )
        }

        fun showDialog(
            context: Context,
            @StringRes titleId: Int = R.string.sceyt_empty_string,
            @StringRes descId: Int = R.string.sceyt_empty_string,
            @StringRes positiveBtnTitleId: Int,
            @StringRes negativeBtnTitleId: Int = R.string.sceyt_cancel,
            replaceLastDialog: Boolean = true,
            negativeCb: (() -> Unit)? = null,
            positiveCb: (() -> Unit)? = null,
        ): SceytDialog {
            return showDialogImpl(
                context = context,
                title = context.getString(titleId),
                description = context.getString(descId),
                positiveBtnTitle = context.getString(positiveBtnTitleId),
                negativeBtnTitle = context.getString(negativeBtnTitleId),
                replaceLastDialog = replaceLastDialog,
                negativeCb = negativeCb,
                positiveCb = positiveCb
            )
        }

        private fun showDialogImpl(
            context: Context,
            title: CharSequence,
            description: CharSequence,
            positiveBtnTitle: CharSequence,
            negativeBtnTitle: CharSequence,
            replaceLastDialog: Boolean,
            negativeCb: (() -> Unit)? = null,
            positiveCb: (() -> Unit)? = null,
        ): SceytDialog {

            if (replaceLastDialog)
                lastDialog?.dismissSafety()
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
                show()
                setOnDismissListener { lastDialog = null }
                lastDialog = this
            }
        }
    }
}