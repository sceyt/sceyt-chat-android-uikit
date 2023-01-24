package com.sceyt.sceytchatuikit.presentation.common

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.annotation.StringRes
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.SceytDialogViewBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class SceytDialog(
        context: Context,
        private val positiveClickListener: (() -> Unit)? = null,
        private val negativeClickListener: (() -> Unit)? = null,
) : Dialog(context, R.style.SceytDialogNoTitle) {
    private val binding: SceytDialogViewBinding by lazy { SceytDialogViewBinding.inflate(LayoutInflater.from(context)) }

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

    companion object {
        fun showSceytDialog(context: Context,
                            @StringRes titleId: Int = R.string.sceyt_empty_string,
                            @StringRes descId: Int = R.string.sceyt_empty_string,
                            @StringRes positiveBtnTitleId: Int, positiveCb: () -> Unit): SceytDialog {

            return SceytDialog(context, positiveCb).apply {
                setTitle(context.getString(titleId))
                setDescription(context.getString(descId))
                setPositiveButtonTitle(context.getString(positiveBtnTitleId))
                setPositiveButtonTextColor(context.getCompatColor(SceytKitConfig.sceytColorAccent))
                setNegativeButtonTextColor(context.getCompatColor(SceytKitConfig.sceytColorAccent))
                show()
            }
        }
    }
}