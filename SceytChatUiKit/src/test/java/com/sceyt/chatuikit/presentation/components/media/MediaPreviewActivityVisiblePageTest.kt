package com.sceyt.chatuikit.presentation.components.media

import android.os.Bundle
import android.os.Looper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.logger.SceytLogLevel
import com.sceyt.chatuikit.persistence.di.CoroutineContextType
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.interactor.AttachmentInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.presentation.components.media.adapter.MediaAdapter
import com.sceyt.chatuikit.presentation.components.media.adapter.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.time.Duration
import kotlin.coroutines.CoroutineContext

@RunWith(RobolectricTestRunner::class)
class MediaPreviewActivityVisiblePageTest {

    private val attachmentInteractor = mock<AttachmentInteractor>()
    private val messageInteractor = mock<MessageInteractor>()
    private val fileTransferService = mock<FileTransferService>()
    private val nearResponses = Channel<PaginationResponse<AttachmentWithUserData>>(Channel.UNLIMITED)
    private val prevResponses = Channel<PaginationResponse<AttachmentWithUserData>>(Channel.UNLIMITED)

    @Before
    fun setUp() {
        SceytLog.setLogger(SceytLogLevel.Verbose) { _, _, _, _ -> }
        MediaPreviewTransferHolder.consume()
        stopKoin()
        SceytKoinApp.koinApp = startKoin {
            modules(module {
                single<AttachmentInteractor> { attachmentInteractor }
                single<MessageInteractor> { messageInteractor }
                single<FileTransferService> { fileTransferService }
                single<CoroutineContext>(named(CoroutineContextType.SingleThreaded)) { Dispatchers.Unconfined }
            })
        }
        runBlocking {
            whenever(
                attachmentInteractor.getNearAttachments(
                    conversationId = any(),
                    attachmentId = any(),
                    types = any(),
                    offset = any(),
                    ignoreDb = any(),
                    loadKeyData = anyOrNull(),
                )
            ).thenReturn(nearResponses.receiveAsFlow())
            whenever(
                attachmentInteractor.getPrevAttachments(
                    conversationId = any(),
                    lastAttachmentId = any(),
                    types = any(),
                    offset = any(),
                    ignoreDb = any(),
                    loadKeyData = anyOrNull(),
                )
            ).thenReturn(prevResponses.receiveAsFlow())
        }
    }

    @After
    fun tearDown() {
        nearResponses.close()
        prevResponses.close()
        stopKoin()
        SceytKoinApp.koinApp = null
    }

    private fun attachment(
        name: String,
        id: Long?,
        messageTid: Long = id ?: 0L,
        createdAt: Long = (id ?: 0L) * 1000L,
    ) = SceytAttachment(
        id = id,
        messageId = id ?: 0L,
        messageTid = messageTid,
        userId = null,
        name = name,
        type = AttachmentTypeEnum.Image.value,
        metadata = null,
        fileSize = 0L,
        createdAt = createdAt,
        url = "https://cdn.example/$name",
        filePath = null,
        transferState = null,
        progressPercent = 0f,
        originalFilePath = null,
        linkPreviewDetails = null,
    )

    private fun image(id: Long) = attachment(name = "image-$id", id = id)

    private fun images(range: LongRange) = range.map(::image)

    private fun List<SceytAttachment>.withUser() = map { AttachmentWithUserData(it, null) }

    private fun dbResponse(
        items: List<SceytAttachment>,
        loadType: LoadType = LoadType.LoadNear,
    ) = PaginationResponse.DBResponse(
        data = items.withUser(),
        loadKey = LoadKeyData(),
        offset = 0,
        hasNext = false,
        hasPrev = false,
        loadType = loadType,
    )

    private fun serverResponse(
        items: List<SceytAttachment>,
        loadType: LoadType = LoadType.LoadNear,
    ) = PaginationResponse.ServerResponse(
        data = SceytResponse.Success(items.withUser()),
        cacheData = items.withUser(),
        loadKey = LoadKeyData(),
        offset = 0,
        hasDiff = true,
        hasNext = false,
        hasPrev = false,
        loadType = loadType,
        ignoredDb = false,
    )

    private fun <T : MediaPreviewActivity> launch(
        opened: SceytAttachment,
        activityClass: Class<T>,
    ): T {
        val context = RuntimeEnvironment.getApplication()
        val intent = MediaPreviewActivity.createIntent(
            context = context,
            attachment = opened,
            from = null,
            channelId = 100L,
        ).setClass(context, activityClass)
        val activity = Robolectric.buildActivity(activityClass, intent).setup().get()
        settle()
        return activity
    }

    private fun launch(opened: SceytAttachment) = launch(opened, MediaPreviewActivity::class.java)

    private fun settle() {
        repeat(5) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
        }
    }

    private fun MediaPreviewActivity.pager(): RecyclerView = findViewById(R.id.rvMedia)

    private fun MediaPreviewActivity.committedNames(): List<String> =
        (pager().adapter as MediaAdapter).getData().map { it.attachment.name }

    private fun MediaPreviewActivity.visibleAttachment(): SceytAttachment? {
        val position = (pager().layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
        return (pager().adapter as MediaAdapter).getData().getOrNull(position)?.attachment
    }

    private fun MediaPreviewActivity.awaitCommitted(names: List<String>) {
        val deadline = System.currentTimeMillis() + 5_000
        while (committedNames() != names) {
            check(System.currentTimeMillis() < deadline) {
                "Committed list ${committedNames()} did not become $names"
            }
            Thread.sleep(10)
            settle()
        }
        settle()
    }

    private fun sendNear(response: PaginationResponse<AttachmentWithUserData>) {
        nearResponses.trySend(response)
    }

    private fun sendPrev(response: PaginationResponse<AttachmentWithUserData>) {
        prevResponses.trySend(response)
    }

    @Test
    fun `shows tapped image after db list and server list with extra older items`() {
        val activity = launch(image(10L))
        assertThat(activity.visibleAttachment()?.name).isEqualTo("image-10")

        sendNear(dbResponse(images(8L..12L)))
        activity.awaitCommitted(images(8L..12L).map { it.name })
        assertThat(activity.visibleAttachment()?.name).isEqualTo("image-10")

        sendNear(serverResponse(images(5L..12L)))
        activity.awaitCommitted(images(5L..12L).map { it.name })
        assertThat(activity.visibleAttachment()?.name).isEqualTo("image-10")
    }

    @Test
    fun `shows tapped image when db list commit is superseded by server list`() {
        val activity = launch(image(10L), SupersededDbCommitMediaPreviewActivity::class.java)

        sendNear(dbResponse(images(8L..12L)))
        sendNear(serverResponse(images(5L..12L)))
        activity.awaitCommitted(images(5L..12L).map { it.name })

        assertThat(activity.supersededLists).isEqualTo(1)
        assertThat(activity.visibleAttachment()?.name).isEqualTo("image-10")
    }

    @Test
    fun `shows tapped image when attachments share a message`() {
        val first = attachment(name = "shared-first", id = 10L, messageTid = 1000L, createdAt = 10_000L)
        val second = attachment(name = "shared-second", id = 11L, messageTid = 1000L, createdAt = 10_000L)
        val dbList = listOf(image(8L), image(9L), first, second, image(12L))
        val serverList = images(5L..7L) + dbList

        val activity = launch(second)
        sendNear(dbResponse(dbList))
        activity.awaitCommitted(dbList.map { it.name })
        assertThat(activity.visibleAttachment()?.name).isEqualTo("shared-second")

        sendNear(serverResponse(serverList))
        activity.awaitCommitted(serverList.map { it.name })
        assertThat(activity.visibleAttachment()?.name).isEqualTo("shared-second")
    }

    @Test
    fun `shows tapped pending image with zero id after prev page is prepended`() {
        val pending = attachment(name = "pending", id = 0L, messageTid = 2000L, createdAt = 20_000L)

        val activity = launch(pending)
        sendPrev(dbResponse(images(5L..7L), LoadType.LoadPrev))
        activity.awaitCommitted(images(5L..7L).map { it.name } + "pending")

        assertThat(activity.visibleAttachment()?.name).isEqualTo("pending")
    }

    @Test
    fun `shows tapped pending image with null id after prev page is prepended`() {
        val pending = attachment(name = "pending", id = null, messageTid = 2000L, createdAt = 20_000L)

        val activity = launch(pending)
        sendPrev(dbResponse(images(5L..7L), LoadType.LoadPrev))
        activity.awaitCommitted(images(5L..7L).map { it.name } + "pending")

        assertThat(activity.visibleAttachment()?.name).isEqualTo("pending")
    }

    @Test
    fun `shows sent version of tapped zero id image when server list replaces placeholder`() {
        assertSentVersionShownAfterPlaceholderReplaced(pendingId = 0L)
    }

    @Test
    fun `shows sent version of tapped null id image when server list replaces placeholder`() {
        assertSentVersionShownAfterPlaceholderReplaced(pendingId = null)
    }

    private fun assertSentVersionShownAfterPlaceholderReplaced(pendingId: Long?) {
        val pending = attachment(name = "pending", id = pendingId, messageTid = 2000L, createdAt = 20_000L)
        val sent = attachment(name = "sent", id = 20L, messageTid = 2000L, createdAt = 20_000L)
        val serverList = images(4L..7L) + sent + image(30L)

        val activity = launch(pending)
        sendPrev(dbResponse(images(5L..7L), LoadType.LoadPrev))
        activity.awaitCommitted(images(5L..7L).map { it.name } + "pending")
        sendPrev(serverResponse(serverList, LoadType.LoadPrev))
        activity.awaitCommitted(serverList.map { it.name })

        assertThat(activity.visibleAttachment()?.name).isEqualTo("sent")
    }
}

class SupersededDbCommitMediaPreviewActivity : MediaPreviewActivity() {
    private var changeCount = 0
    private var heldItems: List<MediaItem>? = null
    var supersededLists = 0
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.SceytPreviewThemeDark)
        super.onCreate(savedInstanceState)
    }

    override fun onMediaItemsChanged(items: List<MediaItem>) {
        changeCount++
        when (changeCount) {
            1 -> super.onMediaItemsChanged(items)
            2 -> heldItems = items
            else -> {
                heldItems?.let {
                    super.onMediaItemsChanged(it)
                    supersededLists++
                }
                heldItems = null
                super.onMediaItemsChanged(items)
            }
        }
    }
}
