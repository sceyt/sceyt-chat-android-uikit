package com.sceyt.sceytchatuikit.persistence.filetransfer

import android.util.Size
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem

sealed class NeedMediaInfoData(val item: FileListItem) {
    class NeedDownload(fileListItem: FileListItem) : NeedMediaInfoData(fileListItem)
    class NeedThumb(fileListItem: FileListItem, val size: Size) : NeedMediaInfoData(fileListItem)
}