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
        fun showSceytDialog(context: Context, title: String, description: String,
                            positiveBtnTitle: String,
                            negativeBtnTitle: String = context.getString(R.string.sceyt_cancel),
                            negativeCb: (() -> Unit)? = null,
                            positiveCb: (() -> Unit)? = null): SceytDialog {
            return showDialog(context, title, description, negativeBtnTitle, positiveBtnTitle, negativeCb, positiveCb)
        }

        fun showSceytDialog(context: Context,
                            @StringRes titleId: Int = R.string.sceyt_empty_string,
                            @StringRes descId: Int = R.string.sceyt_empty_string,
                            @StringRes positiveBtnTitleId: Int,
                            @StringRes negativeBtnTitleId: Int = R.string.sceyt_cancel,
                            negativeCb: (() -> Unit)? = null,
                            positiveCb: (() -> Unit)? = null
        ): SceytDialog {

            return showDialog(context, context.getString(titleId), context.getString(descId),
                context.getString(positiveBtnTitleId), context.getString(negativeBtnTitleId), negativeCb, positiveCb)
        }

        private fun showDialog(
                context: Context, title: String, description: String,
                positiveBtnTitle: String,
                negativeBtnTitle: String,
                negativeCb: (() -> Unit)? = null,
                positiveCb: (() -> Unit)? = null,
        ): SceytDialog {
            return SceytDialog(context).apply {
                setTitle(title)
                setDescription(description)
                setPositiveButtonTitle(positiveBtnTitle)
                setNegativeButtonTitle(negativeBtnTitle)
                setPositiveButtonClickListener(positiveCb)
                setNegativeButtonClickListener(negativeCb)
                setPositiveButtonTextColor(context.getCompatColor(SceytKitConfig.sceytColorAccent))
                setNegativeButtonTextColor(context.getCompatColor(SceytKitConfig.sceytColorAccent))
                show()
            }
        }
    }
}