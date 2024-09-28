package com.sceyt.chatuikit.styles.input

import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.common.TextStyle

data class InputReplyMessageStyle(
        @ColorInt var backgroundColor: Int,
        var replyIcon: Drawable?,
        var titleTextStyle: TextStyle,
        var senderNameTextStyle: TextStyle,
        var bodyTextStyle: TextStyle,
        var mentionTextStyle: TextStyle,
        var attachmentDurationTextStyle: TextStyle,
        var attachmentDurationFormatter: Formatter<Long>,
        var attachmentNameFormatter: Formatter<SceytAttachment>,
        var senderNameFormatter: Formatter<SceytUser>,
        var attachmentIconProvider: VisualProvider<SceytAttachment, Drawable?>
) {
    internal class Builder(
            private val typedArray: TypedArray
    ) {
        @ColorInt
        private var backgroundColor: Int = UNSET_COLOR
        private var replyIcon: Drawable? = null
        private var titleTextStyle: TextStyle = TextStyle()
        private var senderNameTextStyle: TextStyle = TextStyle()
        private var bodyTextStyle: TextStyle = TextStyle()
        private var mentionTextStyle: TextStyle = TextStyle()
        private var attachmentDurationTextStyle: TextStyle = TextStyle()

        fun backgroundColor(@StyleableRes index: Int, defValue: Int = backgroundColor) = apply {
            this.backgroundColor = typedArray.getColor(index, defValue)
        }

        fun replyIcon(@StyleableRes index: Int, defValue: Drawable? = replyIcon) = apply {
            this.replyIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun titleTextStyle(titleTextStyle: TextStyle) = apply {
            this.titleTextStyle = titleTextStyle
        }

        fun senderNameTextStyle(senderNameTextStyle: TextStyle) = apply {
            this.senderNameTextStyle = senderNameTextStyle
        }

        fun bodyTextStyle(bodyTextStyle: TextStyle) = apply {
            this.bodyTextStyle = bodyTextStyle
        }

        fun mentionTextStyle(mentionTextStyle: TextStyle) = apply {
            this.mentionTextStyle = mentionTextStyle
        }

        fun attachmentDurationTextStyle(attachmentDurationTextStyle: TextStyle) = apply {
            this.attachmentDurationTextStyle = attachmentDurationTextStyle
        }

        fun build() = InputReplyMessageStyle(
            backgroundColor = backgroundColor,
            replyIcon = replyIcon,
            titleTextStyle = titleTextStyle,
            senderNameTextStyle = senderNameTextStyle,
            bodyTextStyle = bodyTextStyle,
            mentionTextStyle = mentionTextStyle,
            attachmentDurationTextStyle = attachmentDurationTextStyle,
            attachmentDurationFormatter = SceytChatUIKit.formatters.mediaDurationFormatter,
            attachmentNameFormatter = SceytChatUIKit.formatters.attachmentNameFormatter,
            senderNameFormatter = SceytChatUIKit.formatters.userNameFormatterNew,
            attachmentIconProvider = SceytChatUIKit.providers.attachmentIconProvider
        )
    }
}