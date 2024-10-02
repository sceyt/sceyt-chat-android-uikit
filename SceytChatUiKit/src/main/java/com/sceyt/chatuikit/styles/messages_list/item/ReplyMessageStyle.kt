package com.sceyt.chatuikit.styles.messages_list.item

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

data class ReplyMessageStyle(
        @ColorInt val borderColor: Int,
        @ColorInt val voiceDurationColor: Int,
        val titleTextStyle: TextStyle,
        val subtitleTextStyle: TextStyle,
        val deletedMessageTextStyle: TextStyle,
        val mentionTextStyle: TextStyle,
        val attachmentDurationTextStyle: TextStyle,
        val attachmentDurationFormatter: Formatter<Long>,
        val attachmentNameFormatter: Formatter<SceytAttachment>,
        val senderNameFormatter: Formatter<SceytUser>,
        val mentionUserNameFormatter: Formatter<SceytUser>,
        val attachmentIconProvider: VisualProvider<SceytAttachment, Drawable?>
) {
    internal class Builder(
            private val typedArray: TypedArray
    ) {
        @ColorInt
        private var borderColor: Int = UNSET_COLOR

        @ColorInt
        private var voiceDurationColor: Int = UNSET_COLOR
        private var titleStyle: TextStyle = TextStyle()
        private var subtitleTextStyle: TextStyle = TextStyle()
        private var deletedMessageTextStyle: TextStyle = TextStyle()
        private var mentionTextStyle: TextStyle = TextStyle()
        private var attachmentDurationTextStyle: TextStyle = TextStyle()

        fun borderColor(@StyleableRes index: Int, @ColorInt defValue: Int = borderColor) = apply {
            this.borderColor = typedArray.getColor(index, defValue)
        }

        fun voiceDurationColor(@StyleableRes index: Int, @ColorInt defValue: Int = voiceDurationColor) = apply {
            this.voiceDurationColor = typedArray.getColor(index, defValue)
        }

        fun titleTextStyle(titleStyle: TextStyle) = apply {
            this.titleStyle = titleStyle
        }

        fun subtitleTextStyle(descriptionStyle: TextStyle) = apply {
            this.subtitleTextStyle = descriptionStyle
        }

        fun deletedMessageTextStyle(deletedMessageStyle: TextStyle) = apply {
            this.deletedMessageTextStyle = deletedMessageStyle
        }

        fun mentionTextStyle(mentionStyle: TextStyle) = apply {
            this.mentionTextStyle = mentionStyle
        }

        fun attachmentDurationTextStyle(attachmentDurationStyle: TextStyle) = apply {
            this.attachmentDurationTextStyle = attachmentDurationStyle
        }

        fun build() = ReplyMessageStyle(
            borderColor = borderColor,
            voiceDurationColor = voiceDurationColor,
            titleTextStyle = titleStyle,
            subtitleTextStyle = subtitleTextStyle,
            deletedMessageTextStyle = deletedMessageTextStyle,
            mentionTextStyle = mentionTextStyle,
            attachmentDurationTextStyle = attachmentDurationTextStyle,
            attachmentDurationFormatter = SceytChatUIKit.formatters.mediaDurationFormatter,
            attachmentNameFormatter = SceytChatUIKit.formatters.attachmentNameFormatter,
            senderNameFormatter = SceytChatUIKit.formatters.userNameFormatterNew,
            mentionUserNameFormatter = SceytChatUIKit.formatters.mentionUserNameFormatter,
            attachmentIconProvider = SceytChatUIKit.providers.attachmentIconProvider
        )
    }
}
