package com.sceyt.chatuikit.styles

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.formatters.ConversationMediaDateFormatter

/**
 * Style for conversation info media
 * @param videoDurationIcon icon for video duration, default is [R.drawable.sceyt_ic_video]
 * @param mediaDateSeparatorFormat date separator format, default is [ConversationMediaDateFormatter]
 * */
data class ConversationInfoMediaStyle(
        var videoDurationIcon: Drawable?,
        var mediaDateSeparatorFormat: ConversationMediaDateFormatter,
) {

    companion object {
        var styleCustomizer = StyleCustomizer<ConversationInfoMediaStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): ConversationInfoMediaStyle {
            val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.ConversationInfoMediaStyle)

            val mediaDateSeparatorFormat = ConversationMediaDateFormatter()

            val videoDurationIcon = typedArray.getDrawable(R.styleable.ConversationInfoMediaStyle_sceytUiInfoVideoDurationIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_video)

            typedArray.recycle()

            return ConversationInfoMediaStyle(
                videoDurationIcon = videoDurationIcon,
                mediaDateSeparatorFormat = mediaDateSeparatorFormat
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}
