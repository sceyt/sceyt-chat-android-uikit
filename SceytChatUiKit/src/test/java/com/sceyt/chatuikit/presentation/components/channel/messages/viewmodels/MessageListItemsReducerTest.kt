package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytMessageType
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.DateSeparatorItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.UnreadMessagesSeparatorItem
import org.junit.Test

class MessageListItemsReducerTest {

    @Test
    fun `replace reconciles duplicate tid and preserves local message fields`() {
        val pending = item(tid = 10, id = 0, createdAt = 1_000, body = "local")
            .withMessage {
                copy(
                    deliveryStatus = MessageDeliveryStatus.Pending,
                    isSelected = true,
                    isBodyExpanded = true,
                )
            }
        val server = item(tid = 10, id = 110, createdAt = 2_000, body = "server")
            .withMessage { copy(deliveryStatus = MessageDeliveryStatus.Sent) }

        val result = reducer().replace(listOf(pending, server))

        val message = result.messages().single().message
        assertThat(message.id).isEqualTo(110)
        assertThat(message.body).isEqualTo("server")
        assertThat(message.deliveryStatus).isEqualTo(MessageDeliveryStatus.Sent)
        assertThat(message.createdAt).isEqualTo(1_000)
        assertThat(message.isSelected).isTrue()
        assertThat(message.isBodyExpanded).isTrue()
    }

    @Test
    fun `stale pending copy cannot overwrite an established server message`() {
        val server = item(tid = 10, id = 110, createdAt = 1_000, body = "server")
            .withMessage { copy(deliveryStatus = MessageDeliveryStatus.Displayed) }
        val stalePending = item(tid = 10, id = 0, createdAt = 2_000, body = "stale")
            .withMessage { copy(deliveryStatus = MessageDeliveryStatus.Pending) }

        val result = reducer(enableDateSeparator = false).replace(listOf(server, stalePending))

        val message = result.messages().single().message
        assertThat(message.id).isEqualTo(110)
        assertThat(message.body).isEqualTo("server")
        assertThat(message.deliveryStatus).isEqualTo(MessageDeliveryStatus.Displayed)
    }

    @Test
    fun `replace derives one loader per edge and one separator per day`() {
        val first = item(tid = 1, createdAt = 1_000)
        val second = item(tid = 2, createdAt = 2_000)

        val result = reducer().replace(
            listOf(
                MessageListItem.LoadingNextItem,
                date(second),
                first,
                MessageListItem.LoadingPrevItem,
                second,
                MessageListItem.LoadingPrevItem,
                MessageListItem.LoadingNextItem,
            )
        )

        assertThat(result)
            .containsExactly(
                MessageListItem.LoadingPrevItem,
                date(first),
                first,
                second,
                MessageListItem.LoadingNextItem,
            )
            .inOrder()
    }

    @Test
    fun `replace keeps one unread separator at its message boundary`() {
        val first = item(tid = 1, createdAt = 1_000)
        val second = item(tid = 2, createdAt = 2_000)
        val firstUnread = unread(createdAt = first.message.createdAt, lastReadMessageId = 50)
        val duplicateUnread = unread(createdAt = second.message.createdAt, lastReadMessageId = 60)

        val result = reducer(enableDateSeparator = false).replace(
            listOf(firstUnread, first, duplicateUnread, second)
        )

        assertThat(result).containsExactly(firstUnread, first, second).inOrder()
    }

    @Test
    fun `replace removes date separators when disabled`() {
        val first = item(tid = 1, createdAt = 1_000)
        val second = item(tid = 2, createdAt = 86_401_000)

        val result = reducer(enableDateSeparator = false).replace(
            listOf(date(first), first, date(second), second)
        )

        assertThat(result).containsExactly(first, second).inOrder()
    }

    @Test
    fun `prepend page upserts overlap and applies incoming loader state`() {
        val local = item(tid = 20, id = 0, createdAt = 2_000, body = "local")
            .withMessage { copy(isSelected = true) }
        val previous = item(tid = 10, id = 110, createdAt = 1_000)
        val server = item(tid = 20, id = 120, createdAt = 3_000, body = "server")

        val result = reducer(enableDateSeparator = false).prependPage(
            current = listOf(
                MessageListItem.LoadingPrevItem,
                local,
                MessageListItem.LoadingNextItem,
            ),
            incoming = listOf(previous, server),
        )

        assertThat(result)
            .containsExactly(previous, server.withMessage { copy(createdAt = 2_000, isSelected = true) }, MessageListItem.LoadingNextItem)
            .inOrder()
        assertThat(result.messages().map { it.message.tid }).containsExactly(10L, 20L).inOrder()
        val reconciled = result.messages().last().message
        assertThat(reconciled.id).isEqualTo(120)
        assertThat(reconciled.body).isEqualTo("server")
        assertThat(reconciled.createdAt).isEqualTo(2_000)
        assertThat(reconciled.isSelected).isTrue()
    }

    @Test
    fun `page and realtime mutations preserve established order without tid sorting`() {
        val first = item(tid = 900, createdAt = 3_000)
        val second = item(tid = 800, createdAt = 1_000)
        val prepended = item(tid = 5, createdAt = 9_000)
        val appended = item(tid = 2, createdAt = 500)
        val realtime = item(tid = 1, createdAt = 2_000)
        val overlap = item(tid = 800, id = 8_000, createdAt = 7_000, body = "updated")
        val reducer = reducer(enableDateSeparator = false)

        val replaced = reducer.replace(listOf(first, second))
        val withPrevious = reducer.prependPage(replaced, listOf(prepended, overlap))
        val withNext = reducer.appendPage(withPrevious, listOf(appended))
        val result = reducer.appendRealtime(withNext, listOf(realtime))

        assertThat(result.messages().map { it.message.tid })
            .containsExactly(5L, 900L, 800L, 2L, 1L)
            .inOrder()
        assertThat(result.messages()[2].message.id).isEqualTo(8_000)
        assertThat(result.messages()[2].message.createdAt).isEqualTo(1_000)
    }

    @Test
    fun `prepend page recalculates the old boundary group avatar`() {
        val user = SceytUser("same-user")
        val previous = item(tid = 1, createdAt = 1_000)
            .withMessage { copy(incoming = true, isGroup = true, user = user) }
        val current = item(tid = 2, createdAt = 2_000)
            .withMessage {
                copy(
                    incoming = true,
                    isGroup = true,
                    user = user,
                    shouldShowAvatarAndName = true,
                )
            }

        val result = reducer(enableDateSeparator = false).prependPage(
            current = listOf(current),
            incoming = listOf(previous),
        )

        assertThat(result.messages().last().message.shouldShowAvatarAndName).isFalse()
    }

    @Test
    fun `prepend derives date separators from adjacent message days`() {
        val dayOne = item(tid = 3, createdAt = 1_000)
        val dayTwoPrevious = item(tid = 2, createdAt = 86_401_000)
        val dayTwoCurrent = item(tid = 1, createdAt = 86_402_000)
        val reducer = reducer()
        val current = reducer.replace(listOf(dayTwoCurrent))

        val result = reducer.prependPage(current, listOf(dayOne, dayTwoPrevious))

        assertThat(result)
            .containsExactly(
                date(dayOne),
                dayOne,
                date(dayTwoPrevious),
                dayTwoPrevious,
                dayTwoCurrent,
            )
            .inOrder()
    }

    @Test
    fun `paging does not move an unread separator away from its target`() {
        val older = item(tid = 1, createdAt = 1_000)
        val target = item(tid = 2, createdAt = 2_000)
        val following = item(tid = 3, createdAt = 3_000)
        val newer = item(tid = 4, createdAt = 4_000)
        val unread = unread(createdAt = target.message.createdAt, lastReadMessageId = 50)
        val reducer = reducer(enableDateSeparator = false)
        val current = reducer.replace(listOf(unread, target, following))

        val prepended = reducer.prependPage(current, listOf(older))
        val appended = reducer.appendPage(prepended, listOf(newer))

        assertThat(appended)
            .containsExactly(older, unread, target, following, newer)
            .inOrder()
    }

    @Test
    fun `unread separator starts a new visible group sender boundary`() {
        val user = SceytUser("same-user")
        val read = item(tid = 1, createdAt = 1_000).withMessage {
            copy(incoming = true, isGroup = true, user = user)
        }
        val firstUnread = item(tid = 2, createdAt = 2_000).withMessage {
            copy(incoming = true, isGroup = true, user = user)
        }
        val unread = unread(createdAt = firstUnread.message.createdAt, lastReadMessageId = 50)

        val result = reducer(enableDateSeparator = false)
            .replace(listOf(read, unread, firstUnread))

        assertThat(result.messages().map { it.message.shouldShowAvatarAndName })
            .containsExactly(true, true)
            .inOrder()
    }

    @Test
    fun `append page removes next loader for a duplicate-only response`() {
        val current = item(tid = 10, id = 100, createdAt = 1_000, body = "old")
        val refreshed = item(tid = 10, id = 100, createdAt = 2_000, body = "new")

        val result = reducer(enableDateSeparator = false).appendPage(
            current = listOf(
                MessageListItem.LoadingPrevItem,
                current,
                MessageListItem.LoadingNextItem,
            ),
            incoming = listOf(refreshed),
        )

        assertThat(result).containsExactly(
            MessageListItem.LoadingPrevItem,
            refreshed.withMessage { copy(createdAt = 1_000) },
        ).inOrder()
    }

    @Test
    fun `empty pages update only their owned loader edge`() {
        val message = item(tid = 10)
        val current = listOf(
            MessageListItem.LoadingPrevItem,
            message,
            MessageListItem.LoadingNextItem,
        )
        val reducer = reducer(enableDateSeparator = false)

        val prependResult = reducer.prependPage(current, emptyList())
        val appendResult = reducer.appendPage(current, emptyList())

        assertThat(prependResult)
            .containsExactly(message, MessageListItem.LoadingNextItem)
            .inOrder()
        assertThat(appendResult)
            .containsExactly(MessageListItem.LoadingPrevItem, message)
            .inOrder()
    }

    @Test
    fun `realtime across a next gap updates existing tids but does not insert new ones`() {
        val current = item(tid = 1, id = 0, createdAt = 1_000, body = "pending")
        val serverVersion = item(tid = 1, id = 10, createdAt = 2_000, body = "server")
        val missingFromWindow = item(tid = 2, id = 20, createdAt = 3_000)

        val result = reducer(enableDateSeparator = false).appendRealtime(
            current = listOf(
                MessageListItem.LoadingPrevItem,
                current,
                MessageListItem.LoadingNextItem,
            ),
            incoming = listOf(serverVersion, missingFromWindow),
        )

        assertThat(result.messages().map { it.message.tid }).containsExactly(1L)
        assertThat(result.messages().single().message.id).isEqualTo(10)
        assertThat(result.messages().single().message.body).isEqualTo("server")
        assertThat(result.first()).isEqualTo(MessageListItem.LoadingPrevItem)
        assertThat(result.last()).isEqualTo(MessageListItem.LoadingNextItem)

        val ignored = reducer(enableDateSeparator = false).appendRealtime(
            current = result,
            incoming = listOf(
                unread(createdAt = 4_000, lastReadMessageId = 10),
                item(tid = 3, id = 30, createdAt = 4_000),
            ),
        )
        assertThat(ignored).isSameInstanceAs(result)
        assertThat(ignored.filterIsInstance<UnreadMessagesSeparatorItem>()).isEmpty()
    }

    @Test
    fun `append realtime preserves established order when timestamps are equal`() {
        val current = item(tid = 900, createdAt = 1_000)
        val incoming = item(tid = 1, createdAt = 1_000)

        val result = reducer(enableDateSeparator = false).appendRealtime(
            current = listOf(current),
            incoming = listOf(incoming),
        )

        assertThat(result.messages().map { it.message.tid }).containsExactly(900L, 1L).inOrder()
    }

    @Test
    fun `delivery status transitions are monotonic after reaching the server`() {
        val cases = listOf(
            Triple(MessageDeliveryStatus.Pending, MessageDeliveryStatus.Failed, MessageDeliveryStatus.Failed),
            Triple(MessageDeliveryStatus.Pending, MessageDeliveryStatus.Sent, MessageDeliveryStatus.Sent),
            Triple(MessageDeliveryStatus.Failed, MessageDeliveryStatus.Pending, MessageDeliveryStatus.Pending),
            Triple(MessageDeliveryStatus.Failed, MessageDeliveryStatus.Sent, MessageDeliveryStatus.Sent),
            Triple(MessageDeliveryStatus.Sent, MessageDeliveryStatus.Failed, MessageDeliveryStatus.Sent),
            Triple(MessageDeliveryStatus.Sent, MessageDeliveryStatus.Pending, MessageDeliveryStatus.Sent),
            Triple(MessageDeliveryStatus.Received, MessageDeliveryStatus.Sent, MessageDeliveryStatus.Received),
            Triple(MessageDeliveryStatus.Displayed, MessageDeliveryStatus.Received, MessageDeliveryStatus.Displayed),
            Triple(MessageDeliveryStatus.Sent, MessageDeliveryStatus.Received, MessageDeliveryStatus.Received),
            Triple(MessageDeliveryStatus.Received, MessageDeliveryStatus.Displayed, MessageDeliveryStatus.Displayed),
        )
        val reducer = reducer(enableDateSeparator = false)

        cases.forEachIndexed { index, (currentStatus, incomingStatus, expected) ->
            val tid = index.toLong() + 1
            val current = item(tid = tid, id = tid).withMessage {
                copy(deliveryStatus = currentStatus)
            }
            val incoming = item(tid = tid, id = tid).withMessage {
                copy(body = "updated", deliveryStatus = incomingStatus)
            }

            val message = reducer.appendRealtime(listOf(current), listOf(incoming))
                .messages().single().message

            assertThat(message.body).isEqualTo("updated")
            assertThat(message.deliveryStatus).isEqualTo(expected)
        }
    }

    @Test
    fun `delete reassigns date and unread separators to the next same-day message`() {
        val first = item(tid = 1, createdAt = 1_000)
        val second = item(tid = 2, createdAt = 2_000)
        val unread = unread(createdAt = first.message.createdAt, lastReadMessageId = 50)
        val current = reducer().replace(listOf(date(first), unread, first, second))

        val result = reducer().deleteByTids(current, setOf(first.message.tid))

        assertThat(result).containsExactly(date(second), unread.copy(createdAt = second.message.createdAt), second).inOrder()
    }

    @Test
    fun `delete drops a day separator when its last message is removed`() {
        val first = item(tid = 1, createdAt = 1_000)
        val second = item(tid = 2, createdAt = 86_401_000)
        val current = reducer().replace(listOf(first, second))

        val result = reducer().deleteByTids(current, setOf(second.message.tid))

        assertThat(result).containsExactly(date(first), first).inOrder()
    }

    @Test
    fun `unread separator follows source order when its original target is deleted`() {
        val first = item(tid = 1, createdAt = 3_000)
        val second = item(tid = 2, createdAt = 1_000)
        val unread = unread(createdAt = first.message.createdAt, lastReadMessageId = 50)
        val reducer = reducer(enableDateSeparator = false)
        val current = reducer.replace(listOf(unread, first, second))

        val result = reducer.deleteByTids(current, setOf(first.message.tid))

        assertThat(result)
            .containsExactly(unread.copy(createdAt = second.message.createdAt), second)
            .inOrder()
    }

    @Test
    fun `canonicalization derives group avatar boundaries in one pass`() {
        val firstUser = SceytUser("first")
        val secondUser = SceytUser("second")
        fun groupItem(
            tid: Long,
            user: SceytUser,
            createdAt: Long = tid,
            incoming: Boolean = true,
            disabled: Boolean = false,
            type: String = "",
        ) = item(tid = tid, createdAt = createdAt).withMessage {
            copy(
                incoming = incoming,
                isGroup = true,
                user = user,
                disabledShowAvatarAndName = disabled,
                shouldShowAvatarAndName = false,
                type = type,
            )
        }
        val items = listOf(
            groupItem(tid = 1, user = firstUser, createdAt = 1_000),
            groupItem(tid = 2, user = firstUser, createdAt = 2_000),
            groupItem(tid = 3, user = firstUser, createdAt = 3_000, type = SceytMessageType.System.value),
            groupItem(tid = 4, user = firstUser, createdAt = 4_000),
            groupItem(tid = 5, user = secondUser, createdAt = 5_000),
            groupItem(tid = 6, user = secondUser, createdAt = 6_000, incoming = false),
            groupItem(tid = 7, user = secondUser, createdAt = 7_000, disabled = true),
            groupItem(tid = 8, user = secondUser, createdAt = 86_401_000),
        )

        val result = reducer(enableDateSeparator = false).replace(items)

        assertThat(result.messages().map { it.message.shouldShowAvatarAndName })
            .containsExactly(true, false, false, true, true, false, false, true)
            .inOrder()
    }

    @Test
    fun `deleting a group boundary recalculates the next message avatar`() {
        val user = SceytUser("same-user")
        val first = item(tid = 1).withMessage {
            copy(incoming = true, isGroup = true, user = user)
        }
        val second = item(tid = 2).withMessage {
            copy(incoming = true, isGroup = true, user = user)
        }
        val reducer = reducer(enableDateSeparator = false)
        val current = reducer.replace(listOf(first, second))

        val result = reducer.deleteByTids(current, setOf(first.message.tid))

        assertThat(result.messages().single().message.shouldShowAvatarAndName).isTrue()
    }

    @Test
    fun `update changes matching tid and absent tid is a no-op`() {
        val first = item(tid = 1, createdAt = 1_000)
        val current = reducer(enableDateSeparator = false).replace(listOf(first))

        val updated = reducer(enableDateSeparator = false).updateByTid(current, first.message.tid) {
            it.withMessage {
                copy(
                    body = "edited",
                    isSelected = true,
                    deliveryStatus = MessageDeliveryStatus.Pending,
                )
            }
        }
        val unchanged = reducer(enableDateSeparator = false).updateByTid(updated, 999) {
            it.withMessage { copy(body = "never used") }
        }

        assertThat(updated.messages().single().message.body).isEqualTo("edited")
        assertThat(updated.messages().single().message.isSelected).isTrue()
        assertThat(updated.messages().single().message.deliveryStatus)
            .isEqualTo(MessageDeliveryStatus.Displayed)
        assertThat(unchanged).isSameInstanceAs(updated)
    }

    @Test
    fun `update cannot regress a server message to a pending identity`() {
        val current = reducer(enableDateSeparator = false).replace(
            listOf(item(tid = 1, id = 100, body = "server"))
        )

        val result = reducer(enableDateSeparator = false).updateByTid(current, tid = 1) {
            item(tid = 1, id = 0, body = "stale")
                .withMessage { copy(deliveryStatus = MessageDeliveryStatus.Pending) }
        }

        assertThat(result).isSameInstanceAs(current)
        assertThat(result.messages().single().message.id).isEqualTo(100)
        assertThat(result.messages().single().message.body).isEqualTo("server")
    }

    @Test
    fun `center merge rejects missing anchor`() {
        val current = reducer(enableDateSeparator = false).replace(listOf(item(tid = 1)))

        val result = reducer(enableDateSeparator = false).mergeAroundCenter(
            current = current,
            incoming = listOf(item(tid = 2)),
            centerMessageId = 999,
        )

        assertThat(result).isSameInstanceAs(current)
    }

    @Test
    fun `center merge sorts by timestamp and remains unique by tid`() {
        val current = reducer(enableDateSeparator = false).replace(
            listOf(
                MessageListItem.LoadingPrevItem,
                item(tid = 30, id = 30, createdAt = 3_000),
                MessageListItem.LoadingNextItem,
            )
        )

        val result = reducer(enableDateSeparator = false).mergeAroundCenter(
            current = current,
            incoming = listOf(
                item(tid = 20, id = 20, createdAt = 2_000),
                item(tid = 10, id = 10, createdAt = 1_000),
                item(tid = 30, id = 300, createdAt = 4_000, body = "updated"),
            ),
            centerMessageId = 30,
        )

        assertThat(result.messages().map { it.message.tid }).containsExactly(10L, 20L, 30L).inOrder()
        assertThat(result.messages().last().message.id).isEqualTo(300)
        assertThat(result.first()).isEqualTo(MessageListItem.LoadingPrevItem)
        assertThat(result.last()).isEqualTo(MessageListItem.LoadingNextItem)
    }

    @Test
    fun `center merge keeps pending messages below established messages`() {
        val anchor = item(tid = 20, id = 20, createdAt = 2_000)
            .withMessage { copy(deliveryStatus = MessageDeliveryStatus.Sent) }
        val pending = item(tid = 30, id = 0, createdAt = 500)
            .withMessage { copy(deliveryStatus = MessageDeliveryStatus.Pending) }
        val older = item(tid = 10, id = 10, createdAt = 1_000)
            .withMessage { copy(deliveryStatus = MessageDeliveryStatus.Sent) }
        val reducer = reducer(enableDateSeparator = false)
        val current = reducer.replace(listOf(anchor, pending))

        val result = reducer.mergeAroundCenter(
            current = current,
            incoming = listOf(older),
            centerMessageId = anchor.message.id,
        )

        assertThat(result.messages().map { it.message.tid })
            .containsExactly(10L, 20L, 30L)
            .inOrder()
    }

    @Test
    fun `window replacement keeps only incoming tids and reconciles stale overlaps`() {
        val current = item(tid = 1, id = 100, createdAt = 1_000, body = "current")
            .withMessage {
                copy(
                    deliveryStatus = MessageDeliveryStatus.Displayed,
                    isSelected = true,
                    isBodyExpanded = true,
                )
            }
        val outsideWindow = item(tid = 2, id = 200, createdAt = 2_000)
        val stale = item(tid = 1, id = 0, createdAt = 3_000, body = "stale")
            .withMessage { copy(deliveryStatus = MessageDeliveryStatus.Pending) }
        val newWindowItem = item(tid = 3, id = 300, createdAt = 4_000)
        val reducer = reducer(enableDateSeparator = false)

        val result = reducer.replaceWindow(
            current = reducer.replace(listOf(current, outsideWindow)),
            incoming = listOf(stale, newWindowItem),
        )

        assertThat(result.messages().map { it.message.tid }).containsExactly(1L, 3L).inOrder()
        val reconciled = result.messages().first().message
        assertThat(reconciled.id).isEqualTo(100)
        assertThat(reconciled.body).isEqualTo("current")
        assertThat(reconciled.deliveryStatus).isEqualTo(MessageDeliveryStatus.Displayed)
        assertThat(reconciled.isSelected).isTrue()
        assertThat(reconciled.isBodyExpanded).isTrue()
    }

    @Test
    fun `loader removals affect only their requested edge`() {
        val message = item(tid = 1)
        val current = listOf(
            MessageListItem.LoadingPrevItem,
            message,
            MessageListItem.LoadingNextItem,
        )
        val reducer = reducer(enableDateSeparator = false)

        val withoutPrev = reducer.hideLoadingPrev(current)
        val withoutNext = reducer.hideLoadingNext(current)

        assertThat(withoutPrev).containsExactly(message, MessageListItem.LoadingNextItem).inOrder()
        assertThat(withoutNext).containsExactly(MessageListItem.LoadingPrevItem, message).inOrder()
        assertThat(reducer.hideLoadingPrev(withoutPrev)).isSameInstanceAs(withoutPrev)
        assertThat(reducer.hideLoadingNext(withoutNext)).isSameInstanceAs(withoutNext)
    }

    @Test
    fun `removing unread separator repairs same-sender avatar boundary`() {
        val user = SceytUser("same-user")
        val first = item(tid = 1, createdAt = 1_000).withMessage {
            copy(incoming = true, isGroup = true, user = user)
        }
        val second = item(tid = 2, createdAt = 2_000).withMessage {
            copy(incoming = true, isGroup = true, user = user)
        }
        val separator = unread(createdAt = second.message.createdAt, lastReadMessageId = 10)
        val reducer = reducer(enableDateSeparator = false)
        val current = reducer.replace(listOf(first, separator, second))

        val result = reducer.removeUnreadSeparator(current)

        assertThat(result.filterIsInstance<UnreadMessagesSeparatorItem>()).isEmpty()
        assertThat(result.messages().map { it.message.shouldShowAvatarAndName })
            .containsExactly(true, false)
            .inOrder()
    }

    @Test
    fun `history trim removes established messages and preserves pending messages`() {
        val oldSent = item(tid = 1, createdAt = 1_000)
            .withMessage { copy(deliveryStatus = MessageDeliveryStatus.Sent) }
        val oldPending = item(tid = 2, id = 0, createdAt = 2_000)
            .withMessage { copy(deliveryStatus = MessageDeliveryStatus.Pending) }
        val newSent = item(tid = 3, createdAt = 3_000)
            .withMessage { copy(deliveryStatus = MessageDeliveryStatus.Sent) }
        val unread = unread(createdAt = oldPending.message.createdAt, lastReadMessageId = 10)
        val reducer = reducer(enableDateSeparator = false)
        val current = reducer.replace(
            listOf(
                MessageListItem.LoadingPrevItem,
                oldSent,
                unread,
                oldPending,
                newSent,
                MessageListItem.LoadingNextItem,
            )
        )

        val result = reducer.deleteAtOrBeforePreservingPending(current, createdAt = 2_500)

        assertThat(result)
            .containsExactly(
                oldPending,
                newSent,
                MessageListItem.LoadingNextItem,
            )
            .inOrder()
    }

    @Test
    fun `clear selection updates all selected rows without rebuilding structure`() {
        val first = item(tid = 1).withMessage { copy(isSelected = true) }
        val second = item(tid = 2).withMessage { copy(isSelected = false) }
        val third = item(tid = 3).withMessage { copy(isSelected = true) }
        val current = listOf(MessageListItem.LoadingPrevItem, first, second, third)
        val reducer = reducer(enableDateSeparator = false)

        val result = reducer.clearSelection(current)

        assertThat(result.messages().map { it.message.isSelected })
            .containsExactly(false, false, false)
            .inOrder()
        assertThat(result[0]).isSameInstanceAs(current[0])
        assertThat(result[2]).isSameInstanceAs(second)
        assertThat(reducer.clearSelection(result)).isSameInstanceAs(result)
    }

    @Test
    fun `message reconciliation updates main rows and reply references atomically`() {
        val parent = item(tid = 1, id = 100, body = "old")
            .withMessage {
                copy(
                    deliveryStatus = MessageDeliveryStatus.Sent,
                    isSelected = true,
                )
            }
        val reply = item(tid = 2, id = 200).withMessage {
            copy(parentMessage = parent.message)
        }
        val updatedParent = item(tid = 99, id = 100, body = "edited")
            .withMessage { copy(deliveryStatus = MessageDeliveryStatus.Received) }
        val unseen = item(tid = 3, id = 300, body = "not inserted")
        val reducer = reducer(enableDateSeparator = false)
        val current = reducer.replace(listOf(parent, reply))

        val result = reducer.reconcileMessages(
            current = current,
            rowOnlyUpdates = emptyList(),
            rowAndReplyUpdates = listOf(updatedParent, unseen),
        )

        assertThat(result.messages().map { it.message.tid }).containsExactly(99L, 2L).inOrder()
        val main = result.messages()[0].message
        val replyParent = result.messages()[1].message.parentMessage
        assertThat(main.body).isEqualTo("edited")
        assertThat(main.deliveryStatus).isEqualTo(MessageDeliveryStatus.Received)
        assertThat(main.isSelected).isTrue()
        assertThat(replyParent?.body).isEqualTo("edited")
        assertThat(replyParent?.tid).isEqualTo(99L)
        assertThat(replyParent?.deliveryStatus).isEqualTo(MessageDeliveryStatus.Received)
    }

    @Test
    fun `main-only reconciliation does not rewrite reply references`() {
        val parent = item(tid = 1, id = 100, body = "old")
            .withMessage { copy(deliveryStatus = MessageDeliveryStatus.Sent) }
        val reply = item(tid = 2, id = 200).withMessage {
            copy(parentMessage = parent.message)
        }
        val statusUpdate = item(tid = 1, id = 100, body = "status update")
            .withMessage { copy(deliveryStatus = MessageDeliveryStatus.Received) }
        val reducer = reducer(enableDateSeparator = false)
        val current = reducer.replace(listOf(parent, reply))

        val result = reducer.reconcileMessages(
            current = current,
            rowOnlyUpdates = listOf(statusUpdate),
            rowAndReplyUpdates = emptyList(),
        )

        assertThat(result.messages()[0].message.body).isEqualTo("status update")
        assertThat(result.messages()[0].message.deliveryStatus)
            .isEqualTo(MessageDeliveryStatus.Received)
        assertThat(result.messages()[1].message.parentMessage?.body).isEqualTo("old")
        assertThat(result.messages()[1].message.parentMessage?.deliveryStatus)
            .isEqualTo(MessageDeliveryStatus.Sent)
    }

    @Test
    fun `pending deleted reconciliation removes the main row and repairs separators`() {
        val pending = item(tid = 1, id = 0, createdAt = 1_000)
            .withMessage { copy(deliveryStatus = MessageDeliveryStatus.Pending) }
        val next = item(tid = 2, createdAt = 2_000)
        val deleted = pending.withMessage { copy(state = MessageState.Deleted) }
        val reducer = reducer()
        val current = reducer.replace(listOf(pending, next))

        val result = reducer.reconcileMessages(
            current = current,
            rowOnlyUpdates = emptyList(),
            rowAndReplyUpdates = listOf(deleted),
        )

        assertThat(result).containsExactly(date(next), next).inOrder()
    }

    @Test
    fun `pending deletion does not rewrite an existing reply preview`() {
        val pending = item(tid = 1, id = 0, body = "pending")
            .withMessage { copy(deliveryStatus = MessageDeliveryStatus.Pending) }
        val reply = item(tid = 2, id = 200).withMessage {
            copy(parentMessage = pending.message)
        }
        val deleted = pending.withMessage { copy(state = MessageState.Deleted) }
        val reducer = reducer(enableDateSeparator = false)
        val current = reducer.replace(listOf(pending, reply))

        val result = reducer.reconcileMessages(
            current = current,
            rowOnlyUpdates = emptyList(),
            rowAndReplyUpdates = listOf(deleted),
        )

        assertThat(result.messages().map { it.message.tid }).containsExactly(2L)
        assertThat(result.messages().single().message.parentMessage?.body).isEqualTo("pending")
        assertThat(result.messages().single().message.parentMessage?.state)
            .isNotEqualTo(MessageState.Deleted)
    }

    @Test
    fun `stale pending delete cannot remove an established server row`() {
        val current = item(tid = 1, id = 100, body = "server")
            .withMessage { copy(deliveryStatus = MessageDeliveryStatus.Displayed) }
        val staleDelete = item(tid = 1, id = 0, body = "stale")
            .withMessage {
                copy(
                    deliveryStatus = MessageDeliveryStatus.Pending,
                    state = MessageState.Deleted,
                )
            }
        val reducer = reducer(enableDateSeparator = false)
        val snapshot = reducer.replace(listOf(current))

        val result = reducer.reconcileMessages(
            current = snapshot,
            rowOnlyUpdates = emptyList(),
            rowAndReplyUpdates = listOf(staleDelete),
        )

        assertThat(result).isSameInstanceAs(snapshot)
        assertThat(result.messages().single().message.id).isEqualTo(100)
        assertThat(result.messages().single().message.body).isEqualTo("server")
    }

    private fun reducer(enableDateSeparator: Boolean = true) =
        MessageListItemsReducer(enableDateSeparator)

    private fun item(
        tid: Long,
        id: Long = tid,
        createdAt: Long = tid,
        body: String = "",
    ) = MessageItem(createMessage(createdAt = createdAt, id = id, tid = tid).copy(body = body))

    private fun MessageItem.withMessage(update: com.sceyt.chatuikit.data.models.messages.SceytMessage.() -> com.sceyt.chatuikit.data.models.messages.SceytMessage) =
        copy(message = message.update())

    private fun date(item: MessageItem) = DateSeparatorItem(
        createdAt = item.message.createdAt,
        messageTid = item.message.tid,
        messageId = item.message.id,
    )

    private fun unread(createdAt: Long, lastReadMessageId: Long) =
        UnreadMessagesSeparatorItem(createdAt = createdAt, msgId = lastReadMessageId)

    private fun List<MessageListItem>.messages() = filterIsInstance<MessageItem>()
}
