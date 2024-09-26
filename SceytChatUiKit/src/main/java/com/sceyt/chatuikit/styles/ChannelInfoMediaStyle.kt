package com.sceyt.chatuikit.styles

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.formatters.date.MediaDateFormatter
import com.sceyt.chatuikit.providers.VisualProvider

/**
 * Style for conversation info media
 * @param videoDurationIcon icon for video duration, default is [R.drawable.sceyt_ic_video]
 * @param mediaDateSeparatorFormat date separator format, default is [MediaDateFormatter]
 * */
data class ChannelInfoMediaStyle(
        var videoDurationIcon: Drawable?,
        var mediaDateSeparatorFormat: MediaDateFormatter,
    //TODO
        var attachmentIconProvider: VisualProvider<SceytAttachment, Drawable?>
) {

    companion object {
        var styleCustomizer = StyleCustomizer<ChannelInfoMediaStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): ChannelInfoMediaStyle {
            context.obtainStyledAttributes(attributeSet, R.styleable.ConversationInfoMediaStyle).use { typedArray ->
                val mediaDateSeparatorFormat = MediaDateFormatter()

                val videoDurationIcon = typedArray.getDrawable(R.styleable.ConversationInfoMediaStyle_sceytUiInfoVideoDurationIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_video)

                return ChannelInfoMediaStyle(
                    videoDurationIcon = videoDurationIcon,
                    mediaDateSeparatorFormat = mediaDateSeparatorFormat,
                    attachmentIconProvider = SceytChatUIKit.providers.attachmentIconProvider
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}
