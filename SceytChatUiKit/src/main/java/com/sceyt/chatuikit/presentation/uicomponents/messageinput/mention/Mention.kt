package com.sceyt.chatuikit.presentation.uicomponents.messageinput.mention

data class Mention(
        val recipientId: String,
        val name: String,
        val start: Int,
        val length: Int
) : Comparable<Mention> {

    override fun compareTo(other: Mention): Int {
        return start.compareTo(other.start)
    }
}