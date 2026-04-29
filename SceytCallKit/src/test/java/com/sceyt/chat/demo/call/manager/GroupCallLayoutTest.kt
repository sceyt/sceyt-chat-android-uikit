package com.sceyt.chat.demo.call.manager

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GroupCallLayoutTest {

    @Test
    fun `one participant uses single full row`() {
        assertThat(rowSizes(buildPageRows(participants(1)))).isEqualTo(listOf(1))
    }

    @Test
    fun `two participants use stacked full rows`() {
        assertThat(rowSizes(buildPageRows(participants(2)))).isEqualTo(listOf(1, 1))
    }

    @Test
    fun `three participants use pair plus trailing full row`() {
        assertThat(rowSizes(buildPageRows(participants(3)))).isEqualTo(listOf(2, 1))
    }

    @Test
    fun `four participants use paired rows`() {
        assertThat(rowSizes(buildPageRows(participants(4)))).isEqualTo(listOf(2, 2))
    }

    @Test
    fun `five participants use paired rows plus trailing full row`() {
        assertThat(rowSizes(buildPageRows(participants(5)))).isEqualTo(listOf(2, 2, 1))
    }

    @Test
    fun `eight participants stay on one page with paired rows`() {
        val pages = paginateParticipants(participants(8))

        assertThat(pages).hasSize(1)
        assertThat(rowSizes(buildPageRows(pages.first()))).isEqualTo(listOf(2, 2, 2, 2))
    }

    @Test
    fun `nine participants split into eight and one`() {
        val pages = paginateParticipants(participants(9))

        assertThat(pages.map { it.size }).isEqualTo(listOf(8, 1))
        assertThat(rowSizes(buildPageRows(pages.last()))).isEqualTo(listOf(1))
    }

    private fun participants(count: Int): List<CallParticipantUiState> {
        return (1..count).map { index ->
            CallParticipantUiState(userId = "user$index", name = "User $index")
        }
    }

    private fun rowSizes(rows: List<List<CallParticipantUiState>>): List<Int> {
        return rows.map { it.size }
    }
}
