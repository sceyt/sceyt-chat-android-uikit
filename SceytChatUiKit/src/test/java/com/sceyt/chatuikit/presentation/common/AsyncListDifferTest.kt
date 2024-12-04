package com.sceyt.chatuikit.presentation.common

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

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
        asyncListDiffer = AsyncListDiffer(updateCallback, diffUtil)
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
        val updateCallback = mock<ListUpdateCallback>()
        val diffUtil = mock<DiffUtil.ItemCallback<String>>()
        val asyncListDiffer = AsyncListDiffer(updateCallback, diffUtil)
        asyncListDiffer.submitList(initialList) {
            println("AsyncListDifferTest111, currentList: ${asyncListDiffer.currentList}")
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
        asyncListDiffer.submitList(initialList) {
            // Act
            asyncListDiffer.updateItem(
                predicate = { it == "B" },
                newItem = "B_UPDATED",
                payloads = null,
            ) {
                // Assert
                assertEquals(listOf("A", "B_UPDATED", "C"), asyncListDiffer.currentList)
                verify(updateCallback).onChanged(1, 1, null)
            }
        }
    }

    @Test
    fun `removeItem should remove the correct item`() = runTest {
        // Arrange
        val initialList = listOf("A", "B", "C")
        asyncListDiffer.submitList(initialList) {

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
    }

    @Test
    fun `addItem should add item at specified position`() = runTest {
        // Arrange
        val initialList = listOf("A", "B", "C")
        asyncListDiffer.submitList(initialList) {

            // Act
            asyncListDiffer.addItem(
                position = 1,
                item = "NEW_ITEM",
            ){
                // Assert
                assertEquals(listOf("A", "NEW_ITEM", "B", "C"), asyncListDiffer.currentList)
                verify(updateCallback).onInserted(1, 1)
            }
        }
    }

    @Test
    fun `addItems should add multiple items at specified position`() = runTest {
        // Arrange
        val initialList = listOf("A", "B", "C")
        val asyncListDiffer = AsyncListDiffer(updateCallback, diffUtil)
        asyncListDiffer.submitList(initialList) {

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
    }

    @Test
    fun `addItems should append items when position is not specified`() = runTest {
        // Arrange
        val initialList = listOf("A", "B", "C")
        asyncListDiffer.submitList(initialList) {

            // Act
            asyncListDiffer.addItems(
                items = listOf("X", "Y"),
            ) {
                // Assert
                assertEquals(listOf("A", "B", "C", "X", "Y"), asyncListDiffer.currentList)
                verify(updateCallback).onInserted(3, 2)
            }
        }
    }

    @Test
    fun `submitList cancels previous submission if a new list is submitted`() = runTest {
        // Arrange
        val firstList = listOf("A", "B", "C")
        val secondList = listOf("X", "Y", "Z")
        val submissionDelay = 500L // Simulate a delay in processing

        whenever(diffUtil.areItemsTheSame(any(), any())).thenReturn(true)
        whenever(diffUtil.areContentsTheSame(any(), any())).thenReturn(true)

        // Act
        launch(Dispatchers.IO) {
            asyncListDiffer.submitListInternal(firstList, delay = submissionDelay)
        }

        launch(Dispatchers.Default) {
            asyncListDiffer.submitList(secondList)
        }

        // Wait for completion
        delay(submissionDelay * 2)
        // Assert
        assertEquals(secondList, asyncListDiffer.currentList)
       // verify(listUpdateListener).onCurrentListChanged(emptyList(), secondList)
    }

    @Test
    fun `addItems waits until the current submission finishes`() = runBlocking {
        // Arrange
        val initialList = listOf("A", "B", "C")
        val newItems = listOf("X", "Y", "Z")
        val submissionDelay = 200L // Simulate a delay in processing

        whenever(diffUtil.areItemsTheSame(any(), any())).thenReturn(true)
        whenever(diffUtil.areContentsTheSame(any(), any())).thenReturn(true)

        // Simulate delay during submission
        asyncListDiffer.submitListInternal(initialList, delay = submissionDelay)

        // Act
        launch {
            asyncListDiffer.addItems(newItems)
        }

        // Wait for completion
        delay(submissionDelay * 2)

        // Assert
        assertEquals(listOf("A", "B", "C", "X", "Y", "Z"), asyncListDiffer.currentList)
        verify(updateCallback).onInserted(3, newItems.size)
    }


    @Test
    fun `concurrent submitList calls do not corrupt state`() = runBlocking {
        // Arrange
        val listA = listOf("A", "B", "C")
        val listB = listOf("X", "Y", "Z")
        val listC = listOf("1", "2", "3")
        val submissionDelay = 200L // Simulate a delay in processing

        whenever(diffUtil.areItemsTheSame(any(), any())).thenReturn(true)
        whenever(diffUtil.areContentsTheSame(any(), any())).thenReturn(true)

        launch { asyncListDiffer.submitListInternal(listA, delay = submissionDelay) }
        launch { asyncListDiffer.submitListInternal(listB, delay = submissionDelay) }
        asyncListDiffer.submitList(listC) // Should overwrite others

        // Wait for completion
        delay(submissionDelay * 3)

        // Assert
        assertEquals(listC, asyncListDiffer.currentList)
        verify(updateCallback).onInserted(0, listC.size)
    }

    @Test
    fun `mutex ensures only one operation is active at a time`() = runBlocking {
        // Arrange
        val initialList = listOf("A", "B", "C")
        val newList = listOf("X", "Y", "Z")


        // Act
        launch {
            withContext(Dispatchers.Main) {
                asyncListDiffer.submitList(initialList)
            }
        }
        launch {
            withContext(Dispatchers.Main) {
                asyncListDiffer.submitList(newList)
            }
        }

        // Wait for completion
        delay(500)

        // Assert
        assertEquals(newList, asyncListDiffer.currentList)
    }
}