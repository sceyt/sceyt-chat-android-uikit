package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root

import android.util.Size
import android.view.View
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.file_transfer.ThumbData
import com.sceyt.chatuikit.persistence.file_transfer.ThumbFor
import com.sceyt.chatuikit.presentation.custom_views.CircularProgressView
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BaseMediaMessageViewHolderThumbValidationTest {

    @Test
    fun `message list thumb is valid when target matches even if size changed`() {
        val holder = TestMediaMessageViewHolder()

        val valid = holder.isThumbValid(
            ThumbData(
                key = ThumbFor.MessagesLisView.value,
                filePath = "/uploads/image.jpg",
                size = Size(120, 120)
            )
        )

        assertThat(valid).isTrue()
    }

    @Test
    fun `thumb is invalid when target differs`() {
        val holder = TestMediaMessageViewHolder()

        val valid = holder.isThumbValid(
            ThumbData(
                key = ThumbFor.ChannelInfo.value,
                filePath = "/uploads/image.jpg",
                size = Size(240, 240)
            )
        )

        assertThat(valid).isFalse()
    }

    private class TestMediaMessageViewHolder : BaseMediaMessageViewHolder(
        view = View(RuntimeEnvironment.getApplication()),
        style = mock<MessageItemStyle>(),
        messageListeners = null,
        needMediaDataCallback = { _: NeedMediaInfoData -> },
    ) {
        fun isThumbValid(data: ThumbData): Boolean = isValidThumb(data)

        override fun getThumbSize(): Size = Size(240, 240)

        override val loadingProgressView: CircularProgressView =
            CircularProgressView(RuntimeEnvironment.getApplication())

        override val selectMessageView: View? = null

        override val incoming: Boolean = false
    }
}
