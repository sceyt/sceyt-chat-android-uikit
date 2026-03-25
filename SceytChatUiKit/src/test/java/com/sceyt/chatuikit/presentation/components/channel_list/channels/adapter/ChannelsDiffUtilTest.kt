package com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import org.junit.Test

class ChannelsDiffUtilTest {

    // region areItemsTheSame

    @Test
    fun `areItemsTheSame - same id same content - returns true`() {
        val old = listOf(ChannelListItem.ChannelItem(createChannel(1L, 0, 0)))
        val new = listOf(ChannelListItem.ChannelItem(createChannel(1L, 0, 0)))
        val diffUtil = ChannelsDiffUtil(old, new)
        assertThat(diffUtil.areItemsTheSame(0, 0)).isTrue()
    }

    @Test
    fun `areItemsTheSame - same id different content - returns true`() {
        // Key regression test: areItemsTheSame must use id only, not content
        val old = listOf(ChannelListItem.ChannelItem(createChannel(1L, 0, 0).copy(newMessageCount = 0)))
        val new = listOf(ChannelListItem.ChannelItem(createChannel(1L, 0, 0).copy(newMessageCount = 5)))
        val diffUtil = ChannelsDiffUtil(old, new)
        assertThat(diffUtil.areItemsTheSame(0, 0)).isTrue()
    }

    @Test
    fun `areItemsTheSame - different id - returns false`() {
        val old = listOf(ChannelListItem.ChannelItem(createChannel(1L, 0, 0)))
        val new = listOf(ChannelListItem.ChannelItem(createChannel(2L, 0, 0)))
        val diffUtil = ChannelsDiffUtil(old, new)
        assertThat(diffUtil.areItemsTheSame(0, 0)).isFalse()
    }

    @Test
    fun `areItemsTheSame - both LoadingMoreItem - returns true`() {
        val old = listOf(ChannelListItem.LoadingMoreItem)
        val new = listOf(ChannelListItem.LoadingMoreItem)
        val diffUtil = ChannelsDiffUtil(old, new)
        assertThat(diffUtil.areItemsTheSame(0, 0)).isTrue()
    }

    @Test
    fun `areItemsTheSame - ChannelItem vs LoadingMoreItem - returns false`() {
        val old = listOf(ChannelListItem.ChannelItem(createChannel(1L, 0, 0)))
        val new = listOf(ChannelListItem.LoadingMoreItem)
        val diffUtil = ChannelsDiffUtil(old, new)
        assertThat(diffUtil.areItemsTheSame(0, 0)).isFalse()
    }

    // endregion

    // region areContentsTheSame

    @Test
    fun `areContentsTheSame - same content - returns true`() {
        val old = listOf(ChannelListItem.ChannelItem(createChannel(1L, 0, 0)))
        val new = listOf(ChannelListItem.ChannelItem(createChannel(1L, 0, 0)))
        val diffUtil = ChannelsDiffUtil(old, new)
        assertThat(diffUtil.areContentsTheSame(0, 0)).isTrue()
    }

    @Test
    fun `areContentsTheSame - unread count changed - returns false`() {
        val old = listOf(ChannelListItem.ChannelItem(createChannel(1L, 0, 0).copy(newMessageCount = 0)))
        val new = listOf(ChannelListItem.ChannelItem(createChannel(1L, 0, 0).copy(newMessageCount = 3)))
        val diffUtil = ChannelsDiffUtil(old, new)
        assertThat(diffUtil.areContentsTheSame(0, 0)).isFalse()
    }

    @Test
    fun `areContentsTheSame - mute state changed - returns false`() {
        val old = listOf(ChannelListItem.ChannelItem(createChannel(1L, 0, 0).copy(muted = false)))
        val new = listOf(ChannelListItem.ChannelItem(createChannel(1L, 0, 0).copy(muted = true)))
        val diffUtil = ChannelsDiffUtil(old, new)
        assertThat(diffUtil.areContentsTheSame(0, 0)).isFalse()
    }

    // endregion

    // region getChangePayload

    @Test
    fun `getChangePayload - returns ChannelDiff`() {
        val old = listOf(ChannelListItem.ChannelItem(createChannel(1L, 0, 0).copy(newMessageCount = 0, muted = false)))
        val new = listOf(ChannelListItem.ChannelItem(createChannel(1L, 0, 0).copy(newMessageCount = 2, muted = true)))
        val diffUtil = ChannelsDiffUtil(old, new)
        val payload = diffUtil.getChangePayload(0, 0)
        assertThat(payload).isInstanceOf(ChannelDiff::class.java)
        val diff = payload as ChannelDiff
        assertThat(diff.unreadCountChanged).isTrue()
        assertThat(diff.muteStateChanged).isTrue()
        assertThat(diff.lastMessageChanged).isFalse()
    }

    @Test
    fun `getChangePayload - ChannelItem vs LoadingMoreItem - returns null`() {
        val old = listOf(ChannelListItem.ChannelItem(createChannel(1L, 0, 0)))
        val new = listOf(ChannelListItem.LoadingMoreItem)
        val diffUtil = ChannelsDiffUtil(old, new)
        assertThat(diffUtil.getChangePayload(0, 0)).isNull()
    }

    // endregion
}
