package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.adapters

import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.shared.helpers.LinkPreviewHelper

sealed class LinkItem {
    data class Link(val message: SceytMessage,
                    var linkPreviewMetaData: LinkPreviewHelper.PreviewMetaData? = null) : LinkItem()

    object LoadingMore : LinkItem()
}
