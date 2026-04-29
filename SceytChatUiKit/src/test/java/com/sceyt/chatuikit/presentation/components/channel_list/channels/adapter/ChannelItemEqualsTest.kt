package com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.createMessage
import org.junit.Test

class ChannelItemEqualsTest {

    // region equals

    @Test
    fun `equals - same instance - returns true`() {
        val item = ChannelListItem.ChannelItem(createChannel(1L, 0, 0))
        @Suppress("KotlinConstantConditions")
        assertThat(item == item).isTrue()
    }

    @Test
    fun `equals - same id same content - returns true`() {
        val item1 = ChannelListItem.ChannelItem(createChannel(1L, 0, 0))
        val item2 = ChannelListItem.ChannelItem(createChannel(1L, 0, 0))
        assertThat(item1 == item2).isTrue()
    }

    @Test
    fun `equals - different id - returns false`() {
        val item1 = ChannelListItem.ChannelItem(createChannel(1L, 0, 0))
        val item2 = ChannelListItem.ChannelItem(createChannel(2L, 0, 0))
        assertThat(item1 == item2).isFalse()
    }

    @Test
    fun `equals - same id different lastMessage - returns false`() {
        val item1 = ChannelListItem.ChannelItem(createChannel(1L, 0, 0, createMessage(createdAt = 1, tid = 1L)))
        val item2 = ChannelListItem.ChannelItem(createChannel(1L, 0, 0, createMessage(createdAt = 2, tid = 2L)))
        assertThat(item1 == item2).isFalse()
    }

    @Test
    fun `equals - same id different newMessageCount - returns false`() {
        val channel1 = createChannel(1L, 0, 0).copy(newMessageCount = 0)
        val channel2 = createChannel(1L, 0, 0).copy(newMessageCount = 5)
        assertThat(ChannelListItem.ChannelItem(channel1) == ChannelListItem.ChannelItem(channel2)).isFalse()
    }

    @Test
    fun `equals - same id different muted state - returns false`() {
        val channel1 = createChannel(1L, 0, 0).copy(muted = false)
        val channel2 = createChannel(1L, 0, 0).copy(muted = true)
        assertThat(ChannelListItem.ChannelItem(channel1) == ChannelListItem.ChannelItem(channel2)).isFalse()
    }

    @Test
    fun `equals - same id different pinnedAt - returns false`() {
        val item1 = ChannelListItem.ChannelItem(createChannel(1L, pinnedAt = 0, createdAt = 0))
        val item2 = ChannelListItem.ChannelItem(createChannel(1L, pinnedAt = 1000, createdAt = 0))
        assertThat(item1 == item2).isFalse()
    }

    @Test
    fun `equals - same id different unread state - returns false`() {
        val channel1 = createChannel(1L, 0, 0).copy(unread = false)
        val channel2 = createChannel(1L, 0, 0).copy(unread = true)
        assertThat(ChannelListItem.ChannelItem(channel1) == ChannelListItem.ChannelItem(channel2)).isFalse()
    }

    @Test
    fun `equals - compared to non-ChannelItem - returns false`() {
        val item = ChannelListItem.ChannelItem(createChannel(1L, 0, 0))
        assertThat(item.equals(ChannelListItem.LoadingMoreItem)).isFalse()
    }

    // endregion

    // region hashCode

    @Test
    fun `hashCode - same id same content - same hash`() {
        val item1 = ChannelListItem.ChannelItem(createChannel(1L, 0, 0))
        val item2 = ChannelListItem.ChannelItem(createChannel(1L, 0, 0))
        assertThat(item1.hashCode()).isEqualTo(item2.hashCode())
    }

    @Test
    fun `hashCode - same id different content - same hash`() {
        // hashCode is id-based, so content changes don't change hash (no rehashing in maps needed)
        val item1 = ChannelListItem.ChannelItem(createChannel(1L, 0, 0, createMessage(1)))
        val item2 = ChannelListItem.ChannelItem(createChannel(1L, 0, 0, createMessage(99)))
        assertThat(item1.hashCode()).isEqualTo(item2.hashCode())
    }

    // endregion
}
