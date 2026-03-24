package com.sceyt.chat.demo.call.manager

internal const val GROUP_CALL_PAGE_SIZE = 8

internal fun paginateParticipants(
    participants: List<CallParticipantUiState>,
    pageSize: Int = GROUP_CALL_PAGE_SIZE,
): List<List<CallParticipantUiState>> {
    return participants.chunked(pageSize).ifEmpty { listOf(emptyList()) }
}

internal fun buildPageRows(
    participants: List<CallParticipantUiState>,
): List<List<CallParticipantUiState>> {
    if (participants.isEmpty()) return emptyList()
    return when (participants.size) {
        1 -> listOf(participants)
        2 -> participants.map(::listOf)
        else -> {
            if (participants.size % 2 == 1) {
                participants.dropLast(1).chunked(2) + listOf(listOf(participants.last()))
            } else {
                participants.chunked(2)
            }
        }
    }
}
