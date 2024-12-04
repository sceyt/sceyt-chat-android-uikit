package com.sceyt.chatuikit.presentation.common

import android.util.Log
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import kotlinx.coroutines.test.runTest
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
    private lateinit var asyncListDiffer: AsyncListDiffer<String>

    @Before
    fun setup() {
        diffUtil = mock()
        updateCallback = mock()
        asyncListDiffer = AsyncListDiffer(updateCallback, diffUtil)
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
                commitCallback = null
            )

            // Assert
            assertEquals(listOf("A", "B_UPDATED", "C"), asyncListDiffer.currentList)
            verify(updateCallback).onChanged(1, 1, null)
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
                commitCallback = null
            )

            // Assert
            assertEquals(listOf("A", "C"), asyncListDiffer.currentList)
            verify(updateCallback).onRemoved(1, 1)
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
                commitCallback = null
            )

            // Assert
            assertEquals(listOf("A", "NEW_ITEM", "B", "C"), asyncListDiffer.currentList)
            verify(updateCallback).onInserted(1, 1)
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

/*    @Test
    fun `addListListener should notify listener on list change`() = runTest {
        // Arrange
        val listener = mock<AsyncListDiffer.ListListener<String>>()
        asyncListDiffer.submitList(emptyList()){
            asyncListDiffer.addListListener(listener)
            val newList = listOf("A", "B", "C")

            // Act
            asyncListDiffer.submitList(newList) {

                // Assert
                verify(listener).onCurrentListChanged(emptyList(), newList)
            }
        }
    }*/
}