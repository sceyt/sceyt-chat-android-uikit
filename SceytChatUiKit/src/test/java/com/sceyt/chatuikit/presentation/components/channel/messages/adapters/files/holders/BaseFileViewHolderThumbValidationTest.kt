package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders

import android.util.Size
import android.view.View
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.file_transfer.ThumbData
import com.sceyt.chatuikit.persistence.file_transfer.ThumbFor
import com.sceyt.chatuikit.presentation.components.channel.messages.events.AttachmentDataProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BaseFileViewHolderThumbValidationTest {

    @Test
    fun `thumb is valid when target matches even if size changed`() {
        val holder = TestFileViewHolder()

        val valid = holder.isThumbValid(
            ThumbData(
                key = ThumbFor.ChannelInfo.value,
                filePath = "/thumbs/image.jpg",
                size = Size(120, 120)
            )
        )

        assertThat(valid).isTrue()
    }

    @Test
    fun `thumb is invalid when target differs`() {
        val holder = TestFileViewHolder()

        val valid = holder.isThumbValid(
            ThumbData(
                key = ThumbFor.MessagesLisView.value,
                filePath = "/thumbs/image.jpg",
                size = Size(240, 240)
            )
        )

        assertThat(valid).isFalse()
    }

    private class TestFileViewHolder : BaseFileViewHolder<AttachmentDataProvider>(
        itemView = View(RuntimeEnvironment.getApplication()),
        needMediaDataCallback = { _: NeedMediaInfoData -> },
    ) {
        fun isThumbValid(data: ThumbData): Boolean = isValidThumb(data)

        override fun needThumbFor(): ThumbFor = ThumbFor.ChannelInfo

        override fun getThumbSize(): Size = Size(240, 240)
    }
}
