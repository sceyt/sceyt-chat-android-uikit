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

data class InputEditMessageStyle(
        @ColorInt val backgroundColor: Int,
        val editIcon: Drawable?,
        val titleTextStyle: TextStyle,
        val bodyTextStyle: TextStyle,
        val mentionTextStyle: TextStyle,
        val attachmentDurationTextStyle: TextStyle,
        val attachmentDurationFormatter: Formatter<Long>,
        val attachmentNameFormatter: Formatter<SceytAttachment>,
        val mentionUserNameFormatter: Formatter<SceytUser>,
        val attachmentIconProvider: VisualProvider<SceytAttachment, Drawable?>
) {
    internal class Builder(
            private val typedArray: TypedArray
    ) {
        @ColorInt
        private var backgroundColor: Int = UNSET_COLOR
        private var editIcon: Drawable? = null
        private var titleTextStyle: TextStyle = TextStyle()
        private var bodyTextStyle: TextStyle = TextStyle()
        private var mentionTextStyle: TextStyle = TextStyle()
        private var attachmentDurationTextStyle: TextStyle = TextStyle()

        fun backgroundColor(@StyleableRes index: Int, defValue: Int = backgroundColor) = apply {
            this.backgroundColor = typedArray.getColor(index, defValue)
        }

        fun editIcon(@StyleableRes index: Int, defValue: Drawable? = editIcon) = apply {
            this.editIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun titleTextStyle(titleTextStyle: TextStyle) = apply {
            this.titleTextStyle = titleTextStyle
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

        fun build() = InputEditMessageStyle(
            backgroundColor = backgroundColor,
            editIcon = editIcon,
            titleTextStyle = titleTextStyle,
            bodyTextStyle = bodyTextStyle,
            mentionTextStyle = mentionTextStyle,
            attachmentDurationTextStyle = attachmentDurationTextStyle,
            attachmentDurationFormatter = SceytChatUIKit.formatters.mediaDurationFormatter,
            attachmentNameFormatter = SceytChatUIKit.formatters.attachmentNameFormatter,
            mentionUserNameFormatter = SceytChatUIKit.formatters.mentionUserNameFormatter,
            attachmentIconProvider = SceytChatUIKit.providers.attachmentIconProvider
        )
    }
}