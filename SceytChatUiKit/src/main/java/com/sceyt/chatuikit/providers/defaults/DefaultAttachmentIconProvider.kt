package com.sceyt.chatuikit.providers.defaults

import android.content.Context
import android.graphics.drawable.Drawable
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.providers.VisualProvider

open class DefaultAttachmentIconProvider(
        private val context: Context
) : VisualProvider<SceytAttachment, Drawable?> {
    override fun provide(from: SceytAttachment): Drawable? {
        val drawableId = when (from.type) {
            AttachmentTypeEnum.File.value() -> R.drawable.sceyt_ic_file_filled
            AttachmentTypeEnum.Link.value() -> R.drawable.sceyt_ic_link_attachment
            AttachmentTypeEnum.Voice.value() -> R.drawable.sceyt_ic_voice_white
            else -> return null
        }
        return context.getCompatDrawable(drawableId)
    }
}