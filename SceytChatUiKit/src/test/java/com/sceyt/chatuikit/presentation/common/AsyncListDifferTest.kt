package com.sceyt.chatuikit.presentation.common

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class AsyncListDifferTest {
    private lateinit var diffUtil: DiffUtil.ItemCallback<String>
    private lateinit var updateCallback: ListUpdateCallback
    private lateinit var listUpdateListener: AsyncListDiffer.ListListener<String>
    private lateinit var asyncListDiffer: AsyncListDiffer<String>
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        diffUtil = mock()
        updateCallback = mock()
        listUpdateListener = mock()
        asyncListDiffer = AsyncListDiffer(updateCallback, diffUtil, testDispatcher, testDispatcher)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submitList with new list should trigger insertions`() = runTest {
        // Arrange
        val newList = listOf("A", "B", "C")
        whenever(diffUtil.areItemsTheSame(any(), any())).thenReturn(true)
        whenever(diffUtil.areContentsTheSame(any(), any())).thenReturn(true)

        // Act
        asyncListDiffer.submitList(newList) {
            // Assert
            assertEquals(newList, asyncListDiffer.currentList)
            verify(updateCallback).onInserted(0, newList.size)
        }
    }

    @Test
    fun `submitList with null should clear the list`() = runTest {
        // Arrange
        val initialList = listOf("A", "B", "C")
        asyncListDiffer.submitList(initialList) {
            // Act
            asyncListDiffer.submitList(null) {
                // Assert
                assertEquals(emptyList<String>(), asyncListDiffer.currentList)
                verify(updateCallback).onRemoved(0, initialList.size)
            }
        }
    }

    @Test
    fun `updateItem should update the correct item`() = runTest {
        // Arrange
        val initialList = listOf("A", "B", "C")
        asyncListDiffer.submitList(initialList)
        // Act
        asyncListDiffer.updateItem(
            predicate = { it == "B" },
            newItem = "B_UPDATED",
        ) {
            // Assert
            assertEquals(listOf("A", "B_UPDATED", "C"), asyncListDiffer.currentList)
            verify(updateCallback).onChanged(1, 1, null)
        }
    }

    @Test
    fun `removeItem should remove the correct item`() = runTest {
        // Arrange
        val initialList = listOf("A", "B", "C")
        asyncListDiffer.submitList(initialList)

        // Act
        asyncListDiffer.removeItem(
            predicate = { it == "B" },
            commitCallback = {
                // Assert
                assertEquals(listOf("A", "C"), asyncListDiffer.currentList)
                verify(updateCallback).onRemoved(1, 1)
            }
        )

    }

    @Test
    fun `addItem should add item at specified position`() = runTest {
        // Arrange
        val initialList = listOf("A", "B", "C")
        asyncListDiffer.submitList(initialList)

        // Act
        asyncListDiffer.addItem(
            position = 1,
            item = "NEW_ITEM",
        ) {
            // Assert
            assertEquals(listOf("A", "NEW_ITEM", "B", "C"), asyncListDiffer.currentList)
            verify(updateCallback).onInserted(1, 1)
        }
    }

    @Test
    fun `addItems should add items at specified position`() = runTest {
        // Arrange
        val initialList = listOf("A", "B", "C")
        val asyncListDiffer = AsyncListDiffer(updateCallback, diffUtil)
        asyncListDiffer.submitList(initialList)

        // Act
        asyncListDiffer.addItems(
            position = 1,
            items = listOf("X", "Y"),
        ) {
            // Assert
            assertEquals(listOf("A", "X", "Y", "B", "C"), asyncListDiffer.currentList)
            verify(updateCallback).onInserted(1, 2)
        }
    }

    @Test
    fun `addItems should append items when position is not specified`() = runTest {
        // Arrange
        val initialList = listOf("A", "B", "C")
        asyncListDiffer.submitList(initialList)

        // Act
        asyncListDiffer.addItems(
            items = listOf("X", "Y"),
        ) {
            // Assert
            assertEquals(listOf("A", "B", "C", "X", "Y"), asyncListDiffer.currentList)
            verify(updateCallback).onInserted(3, 2)
        }
    }

    @Test
    fun `submitList cancels previous submission if a new list is submitted`() = runTest {
        // Arrange
        val firstList = listOf("A", "B", "C")
        val secondList = listOf("X", "Y", "Z")

        whenever(diffUtil.areItemsTheSame(any(), any())).thenReturn(true)
        whenever(diffUtil.areContentsTheSame(any(), any())).thenReturn(true)

        // Act
        asyncListDiffer.submitList(firstList)
        asyncListDiffer.submitList(secondList)

        // Wait for completion
        delay(500)
        // Assert
        assertEquals(secondList, asyncListDiffer.currentList)
        // verify(listUpdateListener).onCurrentListChanged(emptyList(), secondList)
    }

    @Test
    fun `addItems waits until the current submission finishes`() = runTest {
        // Arrange
        val initialList = buildList {
            repeat(1000) { add(it.toString()) }
        }
        val newItems = listOf("X", "Y", "Z")

        whenever(diffUtil.areItemsTheSame(any(), any())).thenReturn(true)
        whenever(diffUtil.areContentsTheSame(any(), any())).thenReturn(true)

        // Simulate delay during submission
        asyncListDiffer.submitList(initialList)

        // Act
        asyncListDiffer.addItems(newItems)

        // Wait for completion
        delay(500)

        // Assert
        assertEquals(initialList.plus(newItems), asyncListDiffer.currentList)
        verify(updateCallback).onInserted(initialList.size, newItems.size)
    }

    @Test
    fun `addItems will ignore if called before submit`() = runTest {
        // Arrange
        val initialList = listOf("A", "B", "C")
        val newItems = listOf("X", "Y", "Z")

        asyncListDiffer.addItems(newItems)
        // Simulate delay during submission
        asyncListDiffer.submitList(initialList)

        // Wait for completion
        delay(500)

        // Assert
        assertEquals(initialList, asyncListDiffer.currentList)
    }


    @Test
    fun `multiple add operations should be work sequentially`() = runTest {
        // Arrange
        val initialList = listOf("A", "B", "C")
        val newList = listOf("X", "Y", "Z")
        val newList2 = listOf("X", "Y", "Z")

        // Act
        asyncListDiffer.addItems(initialList)
        asyncListDiffer.addItems(1, newList)
        asyncListDiffer.addItems(newList2)

        // Wait for completion
        delay(1000)

        // Assert
        val shouldBe = buildList {
            addAll(initialList)
            addAll(1, newList)
            addAll(newList2)
        }
        assertEquals(shouldBe, asyncListDiffer.currentList)
    }

    @Test
    fun `multiple remove operations should be work sequentially`() = runTest {
        // Arrange
        val initialList = listOf("A", "B", "C", "D", "E")

        // Act
        asyncListDiffer.submitList(initialList)
        asyncListDiffer.removeItem { it == "B" }
        asyncListDiffer.removeItem { it == "C" }
        asyncListDiffer.removeItem { it == "A" }

        // Wait for completion
        delay(1000)

        assertEquals(listOf("D", "E"), asyncListDiffer.currentList)
    }

    @Test
    fun `multiple update operations should be work sequentially`() = runTest {
        // Arrange
        val initialList = listOf("A", "B", "C", "D", "E")

        // Act
        asyncListDiffer.submitList(initialList)
        asyncListDiffer.updateItem({ it == "B" }, "B_UPDATED")
        asyncListDiffer.updateItem({ it == "C" }, "C_UPDATED")
        asyncListDiffer.updateItem({ it == "A" }, "A_UPDATED")

        // Wait for completion
        delay(100)

        assertEquals(listOf("A_UPDATED", "B_UPDATED", "C_UPDATED", "D", "E"), asyncListDiffer.currentList)
    }


    @Test
    fun `get current list should be unmodifiable list`() {
        // Arrange
        val initialList = arrayListOf("A", "B", "C")
        asyncListDiffer.submitList(initialList)

        // Act
        val currentList = asyncListDiffer.currentList

        // Assert
        assertThrows(ClassCastException::class.java) {
            (currentList.iterator() as MutableIterator<String>)
        }
    }

    private suspend fun delay(time: Long) = withContext(testDispatcher) {
        kotlinx.coroutines.delay(time)
    }
}