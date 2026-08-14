package com.sceyt.chatuikit.filetransfer.defaults

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.SceytChatUIKitConfig
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.persistence.logicimpl.attachment
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DefaultFileTransferDestinationProviderTest {
    private lateinit var context: Context
    private lateinit var previousConfig: SceytChatUIKitConfig

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        previousConfig = SceytChatUIKit.config
        SceytChatUIKit.config = SceytChatUIKitConfig().apply {
            attachmentTransferConfig.imageDownloadDirectoryName = "Test Images"
            attachmentTransferConfig.videoDownloadDirectoryName = "Test Videos"
            attachmentTransferConfig.fileDownloadDirectoryName = "Test Files"
        }
    }

    @After
    fun tearDown() {
        SceytChatUIKit.config = previousConfig
        listOf("Test Images", "Test Videos", "Test Files").forEach { directoryName ->
            context.filesDir.resolve(directoryName).deleteRecursively()
        }
    }

    @Test
    fun `destination uses configured directory for each attachment type`() {
        val typesAndDirectories = mapOf(
            AttachmentTypeEnum.Image.value to "Test Images",
            AttachmentTypeEnum.Video.value to "Test Videos",
            AttachmentTypeEnum.File.value to "Test Files",
        )

        typesAndDirectories.forEach { (type, directoryName) ->
            val attachment = attachment(messageTid = 25L, type = type, name = "media.bin")
            val destination = DefaultFileTransferDestinationProvider
                .provideDestination(context, attachment)

            assertThat(destination.name).isEqualTo("media.bin")
            assertThat(destination.parentFile?.name).isEqualTo("25")
            assertThat(destination.parentFile?.parentFile?.name).isEqualTo(directoryName)
            assertThat(destination.parentFile?.exists()).isTrue()
        }
    }

    @Test
    fun `blank attachment name produces a unique file name`() {
        val attachment = attachment(name = "", type = AttachmentTypeEnum.File.value)

        val first = DefaultFileTransferDestinationProvider.provideDestination(context, attachment)
        val second = DefaultFileTransferDestinationProvider.provideDestination(context, attachment)

        assertThat(first.name).isNotEmpty()
        assertThat(second.name).isNotEmpty()
        assertThat(first.name).isNotEqualTo(second.name)
    }
}
