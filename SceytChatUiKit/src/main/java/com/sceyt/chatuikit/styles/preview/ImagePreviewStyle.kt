package com.sceyt.chatuikit.styles.preview

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.ToolbarStyle
import com.sceyt.chatuikit.styles.extensions.image_preview.buildToolbarStyle
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

data class ImagePreviewStyle(
        @param:ColorInt val backgroundColor: Int,
        val toolbarStyle: ToolbarStyle,
) {
    companion object {
        var styleCustomizer = StyleCustomizer<ImagePreviewStyle> { _, style -> style }
    }

    internal class Builder(
            internal val context: Context,
            private val attributeSet: AttributeSet?,
    ) {
        fun build(): ImagePreviewStyle {
            context.obtainStyledAttributes(attributeSet, R.styleable.ImagePreview).use { array ->
                val backgroundColor = context.getCompatColor(SceytChatUIKitTheme.colors.backgroundColor)
                val toolbarStyle = buildToolbarStyle(array)

                return ImagePreviewStyle(
                    backgroundColor = backgroundColor,
                    toolbarStyle = toolbarStyle,
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}