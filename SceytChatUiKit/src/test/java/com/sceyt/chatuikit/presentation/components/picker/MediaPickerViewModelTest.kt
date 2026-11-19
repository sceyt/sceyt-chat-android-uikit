package com.sceyt.chatuikit.presentation.components.picker

import android.content.ContentProvider
import android.content.ContentResolver
import android.database.MatrixCursor
import android.net.Uri
import android.os.Looper
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.provider.MediaStore.Video
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.presentation.components.picker.BottomSheetMediaPicker.PickerFilterType
import com.sceyt.chatuikit.presentation.components.picker.adapter.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MediaPickerViewModelTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val provider = mock<ContentProvider>()
    private val contentResolver: ContentResolver
        get() = RuntimeEnvironment.getApplication().contentResolver
    private var gallery = listOf<GalleryPhoto>()
    private var isQueryFailing = false

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        ShadowContentResolver.registerProviderInternal(MediaStore.AUTHORITY, provider)
        whenever(provider.query(any(), any(), any(), anyOrNull(), anyOrNull())).thenAnswer {
            if (isQueryFailing) null else galleryCursor()
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loading again does not query gallery again or duplicate media`() = runTest {
        gallery = listOf(photo(1), photo(2), photo(3))
        val viewModel = createViewModel()

        viewModel.loadMedia()
        advanceUntilIdle()
        viewModel.loadMedia()
        advanceUntilIdle()

        assertThat(viewModel.state.value.items.paths()).containsExactlyElementsIn(gallery.paths()).inOrder()
        verify(provider, times(1)).query(any(), any(), any(), anyOrNull(), any())
    }

    @Test
    fun `gallery change replaces media instead of appending`() = runTest {
        gallery = listOf(photo(1), photo(2))
        val viewModel = createViewModel()
        viewModel.loadMedia()
        advanceUntilIdle()

        gallery = listOf(photo(3)) + gallery
        notifyGalleryChanged()
        advanceUntilIdle()

        assertThat(viewModel.state.value.items.paths()).containsExactlyElementsIn(gallery.paths()).inOrder()
    }

    @Test
    fun `selection stops at max count`() = runTest {
        gallery = listOf(photo(1), photo(2))
        val viewModel = createViewModel(maxSelectCount = 1)
        viewModel.loadMedia()
        advanceUntilIdle()
        val (first, second) = viewModel.state.value.items.map { it.media }

        assertThat(viewModel.toggleSelection(first)).isTrue()
        assertThat(viewModel.toggleSelection(second)).isFalse()

        assertThat(second.selected).isFalse()
        assertThat(viewModel.state.value.selectedCount).isEqualTo(1)
        assertThat(viewModel.getSelectedMedia().map { it.realPath }).containsExactly(first.realPath)
    }

    @Test
    fun `deselected preselected media stays deselected after gallery change`() = runTest {
        val preselected = photo(1)
        gallery = listOf(preselected, photo(2))
        val viewModel = createViewModel(preselectedPaths = listOf(preselected.path))
        viewModel.loadMedia()
        advanceUntilIdle()
        val media = viewModel.state.value.items.first().media
        assertThat(media.selected).isTrue()

        viewModel.toggleSelection(media)
        gallery = listOf(photo(3)) + gallery
        notifyGalleryChanged()
        advanceUntilIdle()

        assertThat(viewModel.state.value.items.media(preselected.path).selected).isFalse()
        assertThat(viewModel.state.value.selectedCount).isEqualTo(0)
    }

    @Test
    fun `deselecting while gallery reloads is kept in reloaded media`() = runTest {
        val preselected = photo(1)
        gallery = listOf(preselected, photo(2))
        val viewModel = createViewModel(preselectedPaths = listOf(preselected.path))
        viewModel.loadMedia()
        advanceUntilIdle()
        val shownMedia = viewModel.state.value.items.first().media

        gallery = listOf(photo(3)) + gallery
        notifyGalleryChanged()
        viewModel.toggleSelection(shownMedia)
        advanceUntilIdle()

        assertThat(viewModel.state.value.items.paths()).containsExactlyElementsIn(gallery.paths()).inOrder()
        assertThat(viewModel.state.value.items.media(preselected.path).selected).isFalse()
        assertThat(viewModel.getSelectedMedia()).isEmpty()
    }

    @Test
    fun `selection is restored from saved state`() = runTest {
        gallery = listOf(photo(1), photo(2))
        val savedStateHandle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle = savedStateHandle)
        viewModel.loadMedia()
        advanceUntilIdle()
        viewModel.toggleSelection(viewModel.state.value.items[1].media)

        val restored = createViewModel(
            savedStateHandle = savedStateHandle,
            preselectedPaths = listOf(gallery[0].path),
        )
        restored.loadMedia()
        advanceUntilIdle()

        assertThat(restored.getSelectedMedia().map { it.realPath }).containsExactly(gallery[1].path)
        assertThat(restored.state.value.selectedCount).isEqualTo(1)
    }

    @Test
    fun `gallery is not marked loaded until query finishes`() = runTest {
        gallery = listOf(photo(1))
        val viewModel = createViewModel()

        viewModel.loadMedia()

        assertThat(viewModel.state.value.isLoaded).isFalse()
        advanceUntilIdle()
        assertThat(viewModel.state.value.isLoaded).isTrue()
    }

    @Test
    fun `empty gallery is loaded with no media`() = runTest {
        gallery = emptyList()
        val viewModel = createViewModel()

        viewModel.loadMedia()
        advanceUntilIdle()

        assertThat(viewModel.state.value.isLoaded).isTrue()
        assertThat(viewModel.state.value.items).isEmpty()
    }

    @Test
    fun `failed first load is not marked loaded`() = runTest {
        isQueryFailing = true
        val viewModel = createViewModel()

        viewModel.loadMedia()
        advanceUntilIdle()

        assertThat(viewModel.state.value.isLoaded).isFalse()
        assertThat(viewModel.state.value.items).isEmpty()
    }

    @Test
    fun `deleting all media while open leaves empty loaded gallery`() = runTest {
        gallery = listOf(photo(1), photo(2))
        val viewModel = createViewModel()
        viewModel.loadMedia()
        advanceUntilIdle()

        gallery = emptyList()
        notifyGalleryChanged()
        advanceUntilIdle()

        assertThat(viewModel.state.value.isLoaded).isTrue()
        assertThat(viewModel.state.value.items).isEmpty()
    }

    @Test
    fun `failed gallery refresh keeps previous media`() = runTest {
        gallery = listOf(photo(1), photo(2))
        val viewModel = createViewModel()
        viewModel.loadMedia()
        advanceUntilIdle()

        isQueryFailing = true
        notifyGalleryChanged()
        advanceUntilIdle()

        assertThat(viewModel.state.value.items.paths()).containsExactlyElementsIn(gallery.paths()).inOrder()
        verify(provider, times(2)).query(any(), any(), any(), anyOrNull(), any())
    }

    @Test
    fun `deleted selected media is unselected after gallery refresh`() = runTest {
        val deleted = photo(1)
        gallery = listOf(deleted, photo(2))
        val viewModel = createViewModel()
        viewModel.loadMedia()
        advanceUntilIdle()
        viewModel.toggleSelection(viewModel.state.value.items.media(deleted.path))

        gallery = gallery - deleted
        notifyGalleryChanged()
        advanceUntilIdle()

        assertThat(viewModel.state.value.selectedCount).isEqualTo(0)
        assertThat(viewModel.getSelectedMedia()).isEmpty()
    }

    @Test
    fun `refresh after empty load publishes only the complete gallery`() = runTest {
        gallery = emptyList()
        val viewModel = createViewModel()
        viewModel.loadMedia()
        advanceUntilIdle()
        val publishedSizes = mutableListOf<Int>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state
                .map { it.items.size }
                .distinctUntilChanged()
                .collect { size ->
                    publishedSizes.add(size)
                }
        }

        gallery = (1L..200L).map { photo(it) }
        notifyGalleryChanged()
        advanceUntilIdle()

        assertThat(publishedSizes).containsExactly(0, 200).inOrder()
    }

    @Test
    fun `selected media with deleted file is unselected after gallery refresh`() = runTest {
        val deleted = photo(1)
        gallery = listOf(deleted, photo(2))
        val viewModel = createViewModel()
        viewModel.loadMedia()
        advanceUntilIdle()
        viewModel.toggleSelection(viewModel.state.value.items.media(deleted.path))

        File(deleted.path).delete()
        notifyGalleryChanged()
        advanceUntilIdle()

        assertThat(viewModel.state.value.items.media(deleted.path).isWrong).isTrue()
        assertThat(viewModel.state.value.selectedCount).isEqualTo(0)
        assertThat(viewModel.getSelectedMedia()).isEmpty()
    }

    @Test
    fun `returning without gallery changes does not reload media`() = runTest {
        gallery = listOf(photo(1), photo(2))
        val viewModel = createViewModel()
        viewModel.loadMedia()
        advanceUntilIdle()

        viewModel.refreshIfGalleryChanged()
        advanceUntilIdle()

        verifyFullGalleryQueries(count = 1)
        verifySnapshotQueries(count = 1)
    }

    @Test
    fun `returning after delete reloads media without notification`() = runTest {
        val deleted = photo(1)
        gallery = listOf(photo(2), deleted)
        val viewModel = createViewModel()
        viewModel.loadMedia()
        advanceUntilIdle()

        gallery = gallery - deleted
        viewModel.refreshIfGalleryChanged()
        advanceUntilIdle()

        assertThat(viewModel.state.value.items.paths()).containsExactlyElementsIn(gallery.paths()).inOrder()
    }

    @Test
    fun `returning after replaced media reloads media without notification`() = runTest {
        gallery = listOf(photo(2), photo(1))
        val viewModel = createViewModel()
        viewModel.loadMedia()
        advanceUntilIdle()

        gallery = listOf(photo(3), gallery[0])
        viewModel.refreshIfGalleryChanged()
        advanceUntilIdle()

        assertThat(viewModel.state.value.items.paths()).containsExactlyElementsIn(gallery.paths()).inOrder()
    }

    @Test
    fun `returning when gallery check fails keeps media`() = runTest {
        gallery = listOf(photo(1), photo(2))
        val viewModel = createViewModel()
        viewModel.loadMedia()
        advanceUntilIdle()

        isQueryFailing = true
        viewModel.refreshIfGalleryChanged()
        advanceUntilIdle()

        assertThat(viewModel.state.value.items.paths()).containsExactlyElementsIn(gallery.paths()).inOrder()
        verifyFullGalleryQueries(count = 1)
    }

    @Test
    fun `returning before first load does not query gallery`() = runTest {
        gallery = listOf(photo(1))
        val viewModel = createViewModel()

        viewModel.refreshIfGalleryChanged()
        advanceUntilIdle()

        verify(provider, never()).query(any(), any(), any(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `returning after swapped media with same count and newest id reloads media`() = runTest {
        val kept = photo(9)
        gallery = listOf(kept, photo(1))
        val viewModel = createViewModel()
        viewModel.loadMedia()
        advanceUntilIdle()

        gallery = listOf(kept, photo(2))
        viewModel.refreshIfGalleryChanged()
        advanceUntilIdle()

        assertThat(viewModel.state.value.items.paths()).containsExactlyElementsIn(gallery.paths()).inOrder()
    }

    @Test
    fun `returning after failed first load retries loading`() = runTest {
        gallery = listOf(photo(1), photo(2))
        isQueryFailing = true
        val viewModel = createViewModel()
        viewModel.loadMedia()
        advanceUntilIdle()

        isQueryFailing = false
        viewModel.refreshIfGalleryChanged()
        advanceUntilIdle()

        assertThat(viewModel.state.value.isLoaded).isTrue()
        assertThat(viewModel.state.value.items.paths()).containsExactlyElementsIn(gallery.paths()).inOrder()
    }

    @Test
    fun `preselected media with deleted file is not selected after first load`() = runTest {
        val deleted = photo(1)
        gallery = listOf(deleted, photo(2))
        File(deleted.path).delete()
        val viewModel = createViewModel(preselectedPaths = listOf(deleted.path))

        viewModel.loadMedia()
        advanceUntilIdle()

        assertThat(viewModel.state.value.selectedCount).isEqualTo(0)
        assertThat(viewModel.getSelectedMedia()).isEmpty()
    }

    private fun verifyFullGalleryQueries(count: Int) {
        verify(provider, times(count)).query(
            any(),
            argThat { FileColumns.DATA in this },
            any(),
            anyOrNull(),
            anyOrNull()
        )
    }

    private fun verifySnapshotQueries(count: Int) {
        verify(provider, times(count)).query(
            any(),
            argThat { FileColumns.DATA !in this },
            any(),
            anyOrNull(),
            anyOrNull()
        )
    }

    private fun TestScope.createViewModel(
        maxSelectCount: Int = BottomSheetMediaPicker.MAX_SELECT_MEDIA_COUNT,
        preselectedPaths: List<String> = emptyList(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) = MediaPickerViewModel(
        contentResolver = contentResolver,
        savedStateHandle = savedStateHandle,
        filterType = PickerFilterType.All,
        maxSelectCount = maxSelectCount,
        preselectedPaths = preselectedPaths,
        ioDispatcher = StandardTestDispatcher(testScheduler),
    )

    private fun notifyGalleryChanged() {
        val changedUri = Uri.withAppendedPath(MediaStore.Files.getContentUri("external"), "42")
        contentResolver.notifyChange(changedUri, null)
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun photo(id: Long) = GalleryPhoto(id, tempFolder.newFile("$id.jpg").path)

    private fun galleryCursor(): MatrixCursor {
        val columns = arrayOf(
            FileColumns._ID,
            FileColumns.DATA,
            FileColumns.DATE_ADDED,
            FileColumns.MEDIA_TYPE,
            Video.Media.DURATION
        )
        return MatrixCursor(columns).apply {
            gallery.forEach {
                addRow(arrayOf<Any>(it.id, it.path, it.id, FileColumns.MEDIA_TYPE_IMAGE, 0))
            }
        }
    }

    private fun List<MediaItem>.paths() = map { it.media.realPath }

    private fun List<MediaItem>.media(path: String) = single { it.media.realPath == path }.media

    @JvmName("galleryPaths")
    private fun List<GalleryPhoto>.paths() = map { it.path }

    private data class GalleryPhoto(val id: Long, val path: String)
}
