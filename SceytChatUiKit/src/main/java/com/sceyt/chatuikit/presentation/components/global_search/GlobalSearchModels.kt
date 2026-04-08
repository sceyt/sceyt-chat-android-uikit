package com.sceyt.chatuikit.presentation.components.global_search

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser

enum class GlobalSearchTab(@param:StringRes val titleRes: Int) {
    Chats(R.string.sceyt_chats),
    Channels(R.string.sceyt_channels),
    Media(R.string.sceyt_media),
    Files(R.string.sceyt_files),
    Voice(R.string.sceyt_voice),
    Links(R.string.sceyt_links)
}

enum class GlobalSearchAttachmentKind {
    Media, File, Voice, Link
}

data class GlobalSearchHeaderState(
    val activeTab: GlobalSearchTab = GlobalSearchTab.Chats,
    val query: String = "",
    val selectedMember: SceytUser? = null,
    val memberSuggestions: List<SceytUser> = emptyList(),
    val isSelectedMemberRemovalPending: Boolean = false,
) {
    val showSuggestions: Boolean
        get() = query.isNotBlank() && selectedMember == null && memberSuggestions.isNotEmpty()
}

data class GlobalSearchRequestKey(
    val tab: GlobalSearchTab,
    val query: String = "",
    val selectedMemberId: String? = null,
)

data class GlobalSearchTabState(
    val tab: GlobalSearchTab,
    val requestKey: GlobalSearchRequestKey? = null,
    val query: String = "",
    val listItems: List<GlobalSearchListItem> = emptyList(),
    val mediaGridItems: List<GlobalSearchMediaGridItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val emptyState: GlobalSearchEmptyState? = null,
    val scrollState: Parcelable? = null,
    val isLoaded: Boolean = false,
    val showMessageChannel: Boolean = true,
) {
    val showMediaGrid: Boolean
        get() = tab == GlobalSearchTab.Media && query.isBlank() && mediaGridItems.isNotEmpty()

    val hasResults: Boolean
        get() = listItems.isNotEmpty() || mediaGridItems.isNotEmpty()

    val showEmptyState: Boolean
        get() = !isLoading && emptyState != null && !hasResults
}

sealed interface GlobalSearchListItem {
    data class SectionHeader(@param:StringRes val titleRes: Int) : GlobalSearchListItem
    data class ChannelItem(val channel: SceytChannel) : GlobalSearchListItem
    data class MessageItem(val result: GlobalSearchMessageResult) : GlobalSearchListItem
    data class AttachmentItem(val result: GlobalSearchAttachmentResult) : GlobalSearchListItem
}

data class GlobalSearchMessageResult(
    val message: SceytMessage,
    val channel: SceytChannel,
)

data class GlobalSearchAttachmentResult(
    val attachment: SceytAttachment,
    val message: SceytMessage,
    val channel: SceytChannel,
    val sender: SceytUser?,
    val kind: GlobalSearchAttachmentKind,
)

data class GlobalSearchMediaGridItem(
    val result: GlobalSearchAttachmentResult,
)

data class GlobalSearchEmptyState(
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val titleRes: Int,
    @param:StringRes val subtitleRes: Int,
)

data class GlobalSearchPage<T>(
    val data: List<T>,
    val hasMore: Boolean,
) {
    companion object {
        fun <T> empty() = GlobalSearchPage<T>(emptyList(), false)
    }
}

fun SceytUser.displayName(): String {
    val fullName = fullName
    return when {
        fullName.isNotBlank() -> fullName
        username.isNotBlank() -> username
        else -> id
    }
}
