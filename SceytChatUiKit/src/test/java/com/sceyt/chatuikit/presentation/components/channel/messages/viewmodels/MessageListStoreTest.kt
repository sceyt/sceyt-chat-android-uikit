package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MessageListStoreTest {
    private fun store() = MessageListStore().apply { enableDateSeparator = false }

    private fun item(id: Long) = MessageItem(createMessage(createdAt = id, id = id, tid = id))

    private fun ids(items: List<MessageListItem>) =
        items.filterIsInstance<MessageItem>().map { it.message.id }

    private fun CoroutineScope.collectEffects(store: MessageListStore): Pair<MutableList<MessageListRenderEffect>, Job> {
        val effects = mutableListOf<MessageListRenderEffect>()
        val job = launch { store.renderEffects.collect { effects.add(it) } }
        return effects to job
    }

    @Test
    fun `replace sets items and emits Replace`() = runTest {
        val store = store()
        val (effects, job) = collectEffects(store)
        advanceUntilIdle()

        store.replace(listOf(item(1), item(2)), force = true)
        advanceUntilIdle()
        job.cancel()

        assertThat(ids(store.items)).containsExactly(1L, 2L).inOrder()
        val replace = effects.single() as MessageListRenderEffect.Replace
        assertThat(replace.force).isTrue()
    }

    @Test
    fun `addRealtime appends a new message and reports it added`() = runTest {
        val store = store()
        store.replace(listOf(item(1)), force = true)
        val (effects, job) = collectEffects(store)
        advanceUntilIdle()

        val added = store.addRealtime(listOf(item(2)), isOutgoing = true)
        advanceUntilIdle()
        job.cancel()

        assertThat(added).isTrue()
        assertThat(ids(store.items)).containsExactly(1L, 2L).inOrder()
        val effect = effects.single() as MessageListRenderEffect.AppendRealtime
        assertThat(effect.scroll).isEqualTo(AppendRealtimeScroll.Always)
    }

    @Test
    fun `addRealtime is a no-op when nothing changes`() = runTest {
        val store = store()
        store.replace(listOf(item(1)), force = true)

        val added = store.addRealtime(listOf(item(1)), isOutgoing = false)

        assertThat(added).isFalse()
        assertThat(ids(store.items)).containsExactly(1L)
    }

    @Test
    fun `updateItem replaces the matching message and emits UpdateItem with its index`() = runTest {
        val store = store()
        store.replace(listOf(item(1), item(2)), force = true)
        val (effects, job) = collectEffects(store)
        advanceUntilIdle()

        store.updateItem(
            predicate = { it.message.tid == 2L },
            update = { it.copy(message = it.message.copy(body = "edited")) }
        )
        advanceUntilIdle()
        job.cancel()

        val update = effects.single() as MessageListRenderEffect.UpdateItem
        assertThat(update.index).isEqualTo(1)
        assertThat(update.item.message.body).isEqualTo("edited")
    }

    @Test
    fun `deleteByTids removes items and reports emptiness`() = runTest {
        val store = store()
        store.replace(listOf(item(1), item(2)), force = true)
        val (effects, job) = collectEffects(store)
        advanceUntilIdle()

        val first = store.deleteByTids(listOf(1L))
        val second = store.deleteByTids(listOf(2L))
        advanceUntilIdle()
        job.cancel()

        assertThat(first.changed).isTrue()
        assertThat(first.isEmpty).isFalse()
        assertThat(second.isEmpty).isTrue()
        assertThat(ids(store.items)).isEmpty()
        assertThat(effects.filterIsInstance<MessageListRenderEffect.DeleteTids>()).hasSize(2)
    }

    @Test
    fun `deleting the header-owning message keeps the day header for same-day survivors`() = runTest {
        val store = MessageListStore().apply { enableDateSeparator = true }
        // Header owned by message 1; message 2 is the same day.
        store.replace(
            listOf(
                MessageListItem.DateSeparatorItem(createdAt = 999, messageTid = 1, messageId = 1),
                item(1),
                item(2)
            ),
            force = true
        )
        val (effects, job) = collectEffects(store)
        advanceUntilIdle()

        val outcome = store.deleteByTids(listOf(1L))
        advanceUntilIdle()
        job.cancel()

        assertThat(outcome.changed).isTrue()
        assertThat(outcome.isEmpty).isFalse()
        val separators = store.items.filterIsInstance<MessageListItem.DateSeparatorItem>()
        assertThat(separators).hasSize(1)
        assertThat(separators.single().messageTid).isEqualTo(2L) // reassigned to survivor
        assertThat(ids(store.items)).containsExactly(2L)
        // Reassignment can't be expressed incrementally -> full Replace.
        assertThat(effects.single()).isInstanceOf(MessageListRenderEffect.Replace::class.java)
    }

    @Test
    fun `deleting the last same-day message drops its header via incremental delete`() = runTest {
        val store = MessageListStore().apply { enableDateSeparator = true }
        store.replace(
            listOf(MessageListItem.DateSeparatorItem(createdAt = 999, messageTid = 1, messageId = 1), item(1)),
            force = true
        )
        val (effects, job) = collectEffects(store)
        advanceUntilIdle()

        val outcome = store.deleteByTids(listOf(1L))
        advanceUntilIdle()
        job.cancel()

        assertThat(outcome.isEmpty).isTrue()
        assertThat(store.items).isEmpty()
        assertThat(effects.single()).isInstanceOf(MessageListRenderEffect.DeleteTids::class.java)
    }

    @Test
    fun `deleteByTids uses Replace when normalization also removes duplicate headers`() = runTest {
        val store = MessageListStore().apply { enableDateSeparator = false }
        store.replace(
            listOf(
                MessageListItem.DateSeparatorItem(createdAt = 1, messageTid = 1, messageId = 1),
                item(1),
                MessageListItem.DateSeparatorItem(createdAt = 2, messageTid = 2, messageId = 2),
                item(2),
                item(3)
            ),
            force = true
        )
        store.enableDateSeparator = true
        val (effects, job) = collectEffects(store)
        advanceUntilIdle()

        store.deleteByTids(listOf(3L))
        advanceUntilIdle()
        job.cancel()

        assertThat(store.items.filterIsInstance<MessageListItem.DateSeparatorItem>()).hasSize(1)
        assertThat(ids(store.items)).containsExactly(1L, 2L).inOrder()
        assertThat(effects.single()).isInstanceOf(MessageListRenderEffect.Replace::class.java)
    }

    @Test
    fun `removeItems drops matching items without emitting`() = runTest {
        val store = store()
        store.replace(listOf(item(1), item(2)), force = true)
        val (effects, job) = collectEffects(store)
        advanceUntilIdle()

        store.removeItems { it is MessageItem && it.message.id == 1L }
        advanceUntilIdle()
        job.cancel()

        assertThat(ids(store.items)).containsExactly(2L)
        assertThat(effects).isEmpty()
    }

    @Test
    fun `clear empties the list and emits Clear`() = runTest {
        val store = store()
        store.replace(listOf(item(1)), force = true)
        val (effects, job) = collectEffects(store)
        advanceUntilIdle()

        store.clear()
        advanceUntilIdle()
        job.cancel()

        assertThat(store.items).isEmpty()
        assertThat(effects.single()).isEqualTo(MessageListRenderEffect.Clear)
    }

    @Test
    fun `concurrent updates on real threads do not lose writes`() = runBlocking {
        val store = store()
        val count = 200
        store.replace((1..count).map { item(it.toLong()) }, force = true)

        (1..count).map { id ->
            launch(Dispatchers.Default) {
                store.updateItem(
                    predicate = { it.message.id == id.toLong() },
                    update = { it.copy(message = it.message.copy(body = "edited-$id")) }
                )
            }
        }.joinAll()

        val bodyById = store.items.filterIsInstance<MessageItem>()
            .associate { it.message.id to it.message.body }
        (1..count).forEach { id ->
            assertThat(bodyById[id.toLong()]).isEqualTo("edited-$id")
        }
    }

    @Test
    fun `mergeSynced collapses duplicate same-day separators into one`() = runTest {
        val store = MessageListStore().apply { enableDateSeparator = true }
        // Existing day group: one header owned by message 1.
        store.replace(
            listOf(MessageListItem.DateSeparatorItem(createdAt = 999, messageTid = 1, messageId = 1), item(1)),
            force = true
        )
        val (effects, job) = collectEffects(store)
        advanceUntilIdle()

        // Synced middle same-day message arrives carrying its own separator.
        val changed = store.mergeSynced(
            listOf(MessageListItem.DateSeparatorItem(createdAt = 1499, messageTid = 2, messageId = 2), item(2))
        )
        advanceUntilIdle()
        job.cancel()

        assertThat(changed).isTrue()
        assertThat(store.items.filterIsInstance<MessageListItem.DateSeparatorItem>()).hasSize(1)
        assertThat(ids(store.items)).containsExactly(1L, 2L).inOrder()
        assertThat(effects.single()).isInstanceOf(MessageListRenderEffect.Sort::class.java)
    }

    @Test
    fun `mergeSynced de-duplicates message items by tid`() = runTest {
        val store = MessageListStore().apply { enableDateSeparator = false }
        store.replace(listOf(item(1), item(2)), force = true)

        // tid 2 already present; only tid 3 is genuinely new.
        store.mergeSynced(listOf(item(2), item(3)))

        assertThat(ids(store.items)).containsExactly(1L, 2L, 3L).inOrder()
    }

    @Test
    fun `mergeSynced preserves prev loader and removes next loader`() = runTest {
        val store = MessageListStore().apply { enableDateSeparator = false }
        store.replace(
            listOf(MessageListItem.LoadingPrevItem, item(1), MessageListItem.LoadingNextItem),
            force = true
        )
        val (effects, job) = collectEffects(store)
        advanceUntilIdle()

        store.mergeSynced(listOf(item(2)))
        advanceUntilIdle()
        job.cancel()

        assertThat(store.items).contains(MessageListItem.LoadingPrevItem)
        assertThat(store.items).doesNotContain(MessageListItem.LoadingNextItem)
        assertThat(ids(store.items)).containsExactly(1L, 2L).inOrder()
        assertThat(effects.single()).isInstanceOf(MessageListRenderEffect.Sort::class.java)
    }

    @Test
    fun `mergeAroundCenter merges missing items through store normalization`() = runTest {
        val store = MessageListStore().apply { enableDateSeparator = true }
        store.replace(
            listOf(MessageListItem.DateSeparatorItem(createdAt = 1, messageTid = 1, messageId = 1), item(1), item(3)),
            force = true
        )
        val (effects, job) = collectEffects(store)
        advanceUntilIdle()

        val foundCenter = store.mergeAroundCenter(
            centerMessageId = 3,
            newItems = listOf(
                MessageListItem.DateSeparatorItem(createdAt = 2, messageTid = 2, messageId = 2),
                item(2),
                item(3)
            )
        )
        advanceUntilIdle()
        job.cancel()

        assertThat(foundCenter).isTrue()
        assertThat(store.items.filterIsInstance<MessageListItem.DateSeparatorItem>()).hasSize(1)
        assertThat(ids(store.items)).containsExactly(1L, 2L, 3L).inOrder()
        assertThat(effects.single()).isInstanceOf(MessageListRenderEffect.Replace::class.java)
    }

    @Test
    fun `sort orders messages and emits Sort`() = runTest {
        val store = store()
        store.replace(listOf(item(2), item(1)), force = true)
        val (effects, job) = collectEffects(store)
        advanceUntilIdle()

        store.sort()
        advanceUntilIdle()
        job.cancel()

        assertThat(ids(store.items)).containsExactly(1L, 2L).inOrder()
        assertThat(effects.single()).isInstanceOf(MessageListRenderEffect.Sort::class.java)
    }
}
