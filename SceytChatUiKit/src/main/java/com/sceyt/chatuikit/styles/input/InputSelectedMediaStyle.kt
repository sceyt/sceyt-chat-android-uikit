package com.sceyt.chatuikit.styles.input

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.TextStyle

data class InputSelectedMediaStyle(
        @ColorInt val backgroundColor: Int,
        @ColorInt val fileAttachmentBackgroundColor: Int,
        val removeAttachmentIcon: Drawable?,
        val attachmentDurationTextStyle: TextStyle,
        val fileAttachmentNameTextStyle: TextStyle,
        val fileAttachmentSizeTextStyle: TextStyle,
        val fileAttachmentSizeFormatter: Formatter<SceytAttachment>,
        val fileAttachmentIconProvider: VisualProvider<SceytAttachment, Drawable?>,
        val mediaDurationFormatter: Formatter<Long>
) {
    companion object {
        var styleCustomizer = StyleCustomizer<InputSelectedMediaStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val typedArray: TypedArray
    ) {
        @ColorInt
        private var backgroundColor: Int = UNSET_COLOR

        @ColorInt
        private var fileAttachmentBackgroundColor: Int = UNSET_COLOR
        private var removeAttachmentIcon: Drawable? = null
        private var attachmentDurationTextStyle: TextStyle = TextStyle()
        private var fileAttachmentNameTextStyle: TextStyle = TextStyle()
        private var fileAttachmentSizeTextStyle: TextStyle = TextStyle()
        private var fileAttachmentSizeFormatter = SceytChatUIKit.formatters.attachmentSizeFormatter
        private var fileAttachmentIconProvider = SceytChatUIKit.providers.attachmentIconProvider
        private var mediaDurationFormatter = SceytChatUIKit.formatters.mediaDurationFormatter

        fun backgroundColor(@StyleableRes index: Int, @ColorInt defValue: Int = backgroundColor) = apply {
            this.backgroundColor = typedArray.getColor(index, defValue)
        }

        fun fileAttachmentBackgroundColor(@StyleableRes index: Int, @ColorInt defValue: Int = fileAttachmentBackgroundColor) = apply {
            this.fileAttachmentBackgroundColor = typedArray.getColor(index, defValue)
        }

        fun removeAttachmentIcon(@StyleableRes index: Int, defValue: Drawable? = removeAttachmentIcon) = apply {
            this.removeAttachmentIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun attachmentDurationTextStyle(attachmentDurationTextStyle: TextStyle) = apply {
            this.attachmentDurationTextStyle = attachmentDurationTextStyle
        }

        fun fileAttachmentNameTextStyle(fileAttachmentNameTextStyle: TextStyle) = apply {
            this.fileAttachmentNameTextStyle = fileAttachmentNameTextStyle
        }

        fun fileAttachmentSizeTextStyle(fileAttachmentSizeTextStyle: TextStyle) = apply {
            this.fileAttachmentSizeTextStyle = fileAttachmentSizeTextStyle
        }

        fun build() = InputSelectedMediaStyle(
            backgroundColor = backgroundColor,
            fileAttachmentBackgroundColor = fileAttachmentBackgroundColor,
            removeAttachmentIcon = removeAttachmentIcon,
            attachmentDurationTextStyle = attachmentDurationTextStyle,
            fileAttachmentNameTextStyle = fileAttachmentNameTextStyle,
            fileAttachmentSizeTextStyle = fileAttachmentSizeTextStyle,
            fileAttachmentSizeFormatter = fileAttachmentSizeFormatter,
            fileAttachmentIconProvider = fileAttachmentIconProvider,
            mediaDurationFormatter = mediaDurationFormatter
        ).let { styleCustomizer.apply(context, it) }
    }
}