package com.sceyt.chatuikit.sceytstyles

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.SceytConversationInfoActivity

/**
 * Style for [SceytConversationInfoActivity] page.
 * @param navigationIcon - icon for navigation back, default value is [R.drawable.sceyt_ic_arrow_back]
 * @param moreIcon - icon for more actions, default value is [R.drawable.sceyt_ic_more_24]
 * @param editIcon - icon for edit actions, default value is [R.drawable.sceyt_ic_edit_stroked]
 * @param spaceBetweenSections - space between sections, default value is 16dp
 * */
data class ConversationInfoStyle(
        var navigationIcon: Drawable?,
        var moreIcon: Drawable?,
        var editIcon: Drawable?,
        var spaceBetweenSections: Int
) {

    companion object {
        var styleCustomizer = StyleCustomizer<ConversationInfoStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?
    ) {

        fun build(): ConversationInfoStyle {
            val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.ConversationInfoStyle)

            val navigationIcon = typedArray.getDrawable(R.styleable.ConversationInfoStyle_sceytUiNavigationIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_arrow_back)

            val moreIcon = typedArray.getDrawable(R.styleable.ConversationInfoStyle_sceytUiMoreIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_more_24)

            val editIcon = typedArray.getDrawable(R.styleable.ConversationInfoStyle_sceytUiEditIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_edit_stroked)

            val spaceBetweenSections = typedArray.getDimensionPixelSize(R.styleable.ConversationInfoStyle_sceytUiSpaceBetweenSections,
                dpToPx(16f))

            typedArray.recycle()

            return ConversationInfoStyle(
                navigationIcon = navigationIcon,
                moreIcon = moreIcon,
                editIcon = editIcon,
                spaceBetweenSections = spaceBetweenSections
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}
