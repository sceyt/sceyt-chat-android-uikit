package com.sceyt.chatuikit.filetransfer

import com.sceyt.chatuikit.filetransfer.defaults.DefaultFileTransferDestinationProvider
import com.sceyt.chatuikit.filetransfer.defaults.DefaultFileTransferTransport
import com.sceyt.chatuikit.filetransfer.encryption.ChatSdkFileEncryption
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryption
import com.sceyt.chatuikit.persistence.lazyVar

class SceytChatUIKitFileTransfer {
    /** Applies to new uploads. Existing encrypted downloads follow attachment metadata. */
    var encryptUploads: Boolean = true

    var encryption: FileEncryption = ChatSdkFileEncryption()

    var transport: FileTransferTransport by lazyVar {
        DefaultFileTransferTransport()
    }

    var destinationProvider: FileTransferDestinationProvider by lazyVar {
        DefaultFileTransferDestinationProvider
    }
}
