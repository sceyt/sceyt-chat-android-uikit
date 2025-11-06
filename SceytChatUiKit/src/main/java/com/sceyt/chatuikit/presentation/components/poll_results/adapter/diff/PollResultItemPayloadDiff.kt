package com.sceyt.chatuikit.presentation.components.poll_results.adapter.diff

data class PollResultItemPayloadDiff(
        val optionChanged: Boolean,
        val voteCountChanged: Boolean,
        val votersChanged: Boolean
) {
    fun hasDifference(): Boolean {
        return optionChanged || voteCountChanged || votersChanged
    }
}