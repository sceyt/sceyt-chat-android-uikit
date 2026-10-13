package com.sceyt.chatuikit.data.models.channels

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CreateChannelDataTest {

    @Test
    fun `userIds constructor gives every user the role and keeps the other fields`() {
        val avatar = ChannelAvatar.Remote("avatars/remote.png")

        val data = CreateChannelData(
            type = "group",
            userIds = listOf("alice", "bob"),
            roleName = "admin",
            uri = "team-x",
            subject = "Team X",
            avatar = avatar,
            metadata = "{}"
        )

        assertThat(data.members.map { it.id }).containsExactly("alice", "bob").inOrder()
        assertThat(data.members.map { it.role.name }).containsExactly("admin", "admin")
        assertThat(data.type).isEqualTo("group")
        assertThat(data.uri).isEqualTo("team-x")
        assertThat(data.subject).isEqualTo("Team X")
        assertThat(data.avatar).isEqualTo(avatar)
        assertThat(data.metadata).isEqualTo("{}")
    }

    @Test
    fun `userIds constructor creates no members for an empty list`() {
        val data = CreateChannelData(type = "group", userIds = emptyList(), roleName = "admin")

        assertThat(data.members).isEmpty()
    }
}
