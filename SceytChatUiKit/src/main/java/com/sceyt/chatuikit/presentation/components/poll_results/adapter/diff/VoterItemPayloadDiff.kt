package com.sceyt.chatuikit.presentation.components.poll_results.adapter.diff

data class VoterItemPayloadDiff(
        val userChanged: Boolean,
        val createdAtChanged: Boolean
) {
    fun hasDifference(): Boolean {
        return userChanged || createdAtChanged
    }
}