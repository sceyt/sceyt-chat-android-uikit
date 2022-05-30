package com.sceyt.chat.ui.presentation.uicomponents.conversationheader

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.SceytConversationHeaderViewBinding
import com.sceyt.chat.ui.utils.binding.BindingUtil

class ConversationHeaderView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: SceytConversationHeaderViewBinding

    init {
        binding = SceytConversationHeaderViewBinding.inflate(LayoutInflater.from(context), this, true)

        if (!isInEditMode)
            BindingUtil.themedBackgroundColor(this, R.color.whiteThemed)

        /*  if (attrs != null) {
              val a = context.obtainStyledAttributes(attrs, R.styleable.ConversationHeaderView)
              ConversationHeaderViewStyle.updateWithAttributes(context, a)
              a.recycle()
          }*/
        init()
    }

    private fun init() {
        // binding.setUpStyle()
    }
}