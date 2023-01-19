package com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.CustomVideoToolbarBinding

class CustomMediaToolbar @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
) :
        FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: CustomVideoToolbarBinding
    private var navigationIconId = R.drawable.sceyt_media_view_ic_arrow_back
    private var title = ""

    init {

        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SceytCustomToolbar)
            navigationIconId = typedArray.getResourceId(R.styleable.SceytCustomToolbar_navigationIcon, navigationIconId)
            title = typedArray.getString(R.styleable.SceytCustomToolbar_title) ?: title
            typedArray.recycle()
        }

        setupViews()
    }

    private fun setupViews() {
        binding = CustomVideoToolbarBinding.inflate(LayoutInflater.from(context), this, true)
        binding.tvTitle.text = title
        binding.imvBack.setImageResource(navigationIconId)
    }

    val navigationIcon get() = binding.imvBack
    val navigationShareIcon get() = binding.imvShare

    fun setTitle(title: String) {
        binding.tvTitle.text = title
    }

    fun setDate(date: String) {
        binding.tvDate.text = date
    }
}