package com.sceyt.chatuikit.styles.invite_link

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.styles.SceytComponentStyle
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.ButtonStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.extensions.invite_link.buildBackgroundStyle
import com.sceyt.chatuikit.styles.extensions.invite_link.buildQrDescriptionTextStyle
import com.sceyt.chatuikit.styles.extensions.invite_link.buildQrTitleTextStyle
import com.sceyt.chatuikit.styles.extensions.invite_link.buildShareQrButtonStyle

data class BottomSheetShareInviteQRStyle(
        val backgroundStyle: BackgroundStyle,
        val titleText: String,
        val descriptionText: String,
        val shareButtonText: String,
        val titleTextStyle: TextStyle,
        val descriptionTextStyle: TextStyle,
        val shareButtonStyle: ButtonStyle,
) : SceytComponentStyle() {

    companion object {
        var styleCustomizer = StyleCustomizer<BottomSheetShareInviteQRStyle> { _, style -> style }
    }

    internal class Builder(
            internal val context: Context,
            private val attrs: AttributeSet?,
    ) {
        fun build(): BottomSheetShareInviteQRStyle {
            context.obtainStyledAttributes(attrs, R.styleable.ShareInviteQR).use { array ->
                val titleText = context.getString(R.string.sceyt_qr_code_for_invites)
                val descriptionText = context.getString(R.string.share_channel_link_desc)
                val shareButtonText = context.getString(R.string.share_qr_code)

                return BottomSheetShareInviteQRStyle(
                    backgroundStyle = buildBackgroundStyle(array),
                    titleText = titleText,
                    descriptionText = descriptionText,
                    shareButtonText = shareButtonText,
                    titleTextStyle = buildQrTitleTextStyle(array),
                    descriptionTextStyle = buildQrDescriptionTextStyle(array),
                    shareButtonStyle = buildShareQrButtonStyle(array)
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}

