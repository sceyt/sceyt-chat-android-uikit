package com.sceyt.chatuikit.persistence.differs

import com.sceyt.chatuikit.data.models.messages.PollOptionUiModel

data class PollOptionDiff(
    val textChanged: Boolean,
    val voteCountChanged: Boolean,
    val votersChanged: Boolean,
    val selectedChanged: Boolean,
    val totalVoteCountChanged: Boolean,
    val closedStatusChanged: Boolean
) {
    fun hasDifference(): Boolean {
        return textChanged || voteCountChanged || votersChanged || selectedChanged ||
                totalVoteCountChanged || closedStatusChanged
    }

    companion object {
        val DEFAULT = PollOptionDiff(
            textChanged = true,
            voteCountChanged = true,
            votersChanged = true,
            selectedChanged = true,
            totalVoteCountChanged = true,
            closedStatusChanged = true
        )
    }

    override fun toString(): String {
        return "textChanged: $textChanged, voteCountChanged: $voteCountChanged, " +
                "votersChanged: $votersChanged, selectedChanged: $selectedChanged " +
                "totalVoteCountChanged: $totalVoteCountChanged, closedStatusChanged: $closedStatusChanged"
    }
}

fun PollOptionUiModel.diff(other: PollOptionUiModel): PollOptionDiff {
    return PollOptionDiff(
        textChanged = text != other.text,
        voteCountChanged = voteCount != other.voteCount,
        votersChanged = voters != other.voters,
        selectedChanged = selected != other.selected,
        totalVoteCountChanged = totalVotesCount != other.totalVotesCount,
        closedStatusChanged = closed != other.closed
    )
}

