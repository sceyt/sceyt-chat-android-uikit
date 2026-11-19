package com.sceyt.chatuikit.styles

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MessageItemStyleOverlayIconsTest {

    private val context = RuntimeEnvironment.getApplication()
    private val style = MessageItemStyle.Builder(context, null).build()

    @Test
    fun `overlay pending sent and received icons are drawn in onPrimary color`() {
        val onPrimary = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
        val overlay = style.overlayMessageDeliveryStatusIcons

        assertThat(overlay.pendingIcon.opaqueColors()).containsExactly(onPrimary)
        assertThat(overlay.sentIcon.opaqueColors()).containsExactly(onPrimary)
        assertThat(overlay.receivedIcon.opaqueColors()).containsExactly(onPrimary)
    }

    @Test
    fun `overlay displayed and failed icons keep their own colors`() {
        val normal = style.messageDeliveryStatusIcons
        val overlay = style.overlayMessageDeliveryStatusIcons

        assertThat(overlay.displayedIcon.opaqueColors()).isEqualTo(normal.displayedIcon.opaqueColors())
        assertThat(overlay.failedIcon.opaqueColors()).isEqualTo(normal.failedIcon.opaqueColors())
    }

    @Test
    fun `building overlay icons does not tint the normal icons`() {
        val iconSecondary = context.getCompatColor(SceytChatUIKit.theme.colors.iconSecondaryColor)

        assertThat(style.messageDeliveryStatusIcons.sentIcon.opaqueColors()).containsExactly(iconSecondary)
    }

    private fun Drawable?.opaqueColors(): Set<Int> {
        val drawable = requireNotNull(this)
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, SIZE, SIZE)
        drawable.draw(Canvas(bitmap))
        val pixels = IntArray(SIZE * SIZE)
        bitmap.getPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE)
        return pixels.filter { Color.alpha(it) == 255 }.toSet()
    }

    private companion object {
        const val SIZE = 48
    }
}
