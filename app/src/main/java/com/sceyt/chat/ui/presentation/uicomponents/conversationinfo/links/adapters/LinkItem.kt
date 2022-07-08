package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.links.adapters

import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.shared.helpers.LinkPreviewHelper

sealed class LinkItem {
    data class Link(val message: SceytMessage,
                    var linkPreviewMetaData: LinkPreviewHelper.PreviewMetaData? = null) : LinkItem()

    object LoadingMore : LinkItem()
}
