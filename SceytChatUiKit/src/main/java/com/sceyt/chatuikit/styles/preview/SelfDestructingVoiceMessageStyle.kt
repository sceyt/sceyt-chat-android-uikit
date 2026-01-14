package com.sceyt.chatuikit.styles.preview

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.presentation.components.channel.messages.preview.SelfDestructingVoiceMessageActivity
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.ToolbarStyle
import com.sceyt.chatuikit.styles.extensions.self_destructing_voice_message.buildToolbarStyle
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

/** Style for [SelfDestructingVoiceMessageActivity].
 * @property backgroundColor Background color of the voice message preview, default is [Color.BLACK].
 * @property toolbarStyle Style for the toolbar with self-destruct indicator.
 * @property messageItemStyle Style for voice message items, used for voice player styling.
 * */

data class SelfDestructingVoiceMessageStyle(
    @param:ColorInt val backgroundColor: Int,
    val toolbarStyle: ToolbarStyle,
    val messageItemStyle: MessageItemStyle
) {
    companion object {
        var styleCustomizer =
            StyleCustomizer<SelfDestructingVoiceMessageStyle> { _, style -> style }
    }

    internal class Builder(
        internal val context: Context,
        private val messageItemStyle: MessageItemStyle
    ) {
        fun build(): SelfDestructingVoiceMessageStyle {
            context.obtainStyledAttributes(null, R.styleable.SelfDestructingMediaPreview)
                .use { array ->
                    val backgroundColor = Color.BLACK

                    return SelfDestructingVoiceMessageStyle(
                        backgroundColor = backgroundColor,
                        toolbarStyle = buildToolbarStyle(array),
                        messageItemStyle = messageItemStyle
                    ).let { styleCustomizer.apply(context, it) }
                }
        }
    }
}