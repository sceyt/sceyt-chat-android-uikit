package com.sceyt.chatuikit.providers.defaults

import android.content.Context
import android.graphics.drawable.Drawable
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.providers.VisualProvider

data object DefaultChannelListAttachmentIconProvider : VisualProvider<SceytAttachment, Drawable?> {
    override fun provide(context: Context, from: SceytAttachment): Drawable? {
        val drawableId = when (from.type) {
            AttachmentTypeEnum.File.value() -> R.drawable.sceyt_ic_body_file_attachment
            AttachmentTypeEnum.Image.value() -> R.drawable.sceyt_ic_body_image_attachment
            AttachmentTypeEnum.Video.value() -> R.drawable.sceyt_ic_body_video_attachment
            AttachmentTypeEnum.Voice.value() -> R.drawable.sceyt_ic_body_voice_attachment
            else -> return null
        }
        return context.getCompatDrawable(drawableId)
    }
}