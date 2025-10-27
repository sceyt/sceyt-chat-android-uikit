package com.sceyt.chatuikit.persistence.differs

import com.sceyt.chatuikit.data.models.messages.PollOptionUiModel

data class PollOptionDiff(
        val textChanged: Boolean,
        val voteCountChanged: Boolean,
        val votersChanged: Boolean,
        val selectedChanged: Boolean,
) {
    fun hasDifference(): Boolean {
        return textChanged || voteCountChanged || votersChanged || selectedChanged
    }

    companion object {
        val DEFAULT = PollOptionDiff(
            textChanged = true,
            voteCountChanged = true,
            votersChanged = true,
            selectedChanged = true,
        )
        val DEFAULT_FALSE = PollOptionDiff(
            textChanged = false,
            voteCountChanged = false,
            votersChanged = false,
            selectedChanged = false,
        )
    }

    override fun toString(): String {
        return "textChanged: $textChanged, voteCountChanged: $voteCountChanged, votersChanged: $votersChanged, selectedChanged: $selectedChanged"
    }
}

fun PollOptionUiModel.diff(other: PollOptionUiModel): PollOptionDiff {
    return PollOptionDiff(
        textChanged = text != other.text,
        voteCountChanged = voteCount != other.voteCount,
        votersChanged = voters != other.voters,
        selectedChanged = selected != other.selected,
    )
}

