package com.sceyt.chatuikit.styles.input

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.defaults.MessageAndMentionTextStylePair
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.TextStyle

data class InputReplyMessageStyle(
        @ColorInt val backgroundColor: Int,
        val replyIcon: Drawable?,
        val titleTextStyle: TextStyle,
        val senderNameTextStyle: TextStyle,
        val bodyTextStyle: TextStyle,
        val mentionTextStyle: TextStyle,
        val attachmentDurationTextStyle: TextStyle,
        val attachmentDurationFormatter: Formatter<Long>,
        val senderNameFormatter: Formatter<SceytUser>,
        val messageBodyFormatter: Formatter<MessageAndMentionTextStylePair>,
        val attachmentIconProvider: VisualProvider<SceytAttachment, Drawable?>,
) {
    companion object {
        var styleCustomizer = StyleCustomizer<InputReplyMessageStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
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
            senderNameFormatter = SceytChatUIKit.formatters.userNameFormatter,
            messageBodyFormatter = SceytChatUIKit.formatters.messageBodyFormatter,
            attachmentIconProvider = SceytChatUIKit.providers.attachmentIconProvider
        ).let { styleCustomizer.apply(context, it) }
    }
}