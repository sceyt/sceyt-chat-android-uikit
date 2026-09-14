package com.sceyt.chatuikit.presentation.components.pinned_messages

import android.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.pinned_messages.PinnedMessagesStyle
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PinnedMessagesStyleTest {
    @Test
    fun `XML attributes configure the screen and toolbar`() {
        val context = RuntimeEnvironment.getApplication()
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sceytUiToolbarTitle, "Custom pins")
            .addAttribute(R.attr.sceytUiToolbarTitleTextColor, "#ff0000")
            .addAttribute(R.attr.sceytUiPinnedMessagesEmptyText, "Nothing saved")
            .addAttribute(R.attr.sceytUiPinnedMessagesScreenBackgroundColor, "#0000ff")
            .build()

        val style = PinnedMessagesStyle.Builder(context, attrs).build()

        assertThat(style.backgroundColor).isEqualTo(Color.BLUE)
        assertThat(style.emptyStateText).isEqualTo("Nothing saved")
        assertThat(style.toolbarTitle).isEqualTo("Custom pins")
        assertThat(style.toolbarStyle.titleTextStyle.color).isEqualTo(Color.RED)
    }

    @Test
    fun `customizer is applied after XML attributes`() {
        val previous = PinnedMessagesStyle.styleCustomizer
        try {
            PinnedMessagesStyle.styleCustomizer = StyleCustomizer { _, style ->
                style.copy(toolbarTitle = "Customized pins")
            }
            val attrs = Robolectric.buildAttributeSet()
                .addAttribute(R.attr.sceytUiToolbarTitle, "XML title")
                .build()
            val style = PinnedMessagesStyle.Builder(RuntimeEnvironment.getApplication(), attrs).build()
            assertThat(style.toolbarTitle).isEqualTo("Customized pins")
        } finally {
            PinnedMessagesStyle.styleCustomizer = previous
        }
    }
}
