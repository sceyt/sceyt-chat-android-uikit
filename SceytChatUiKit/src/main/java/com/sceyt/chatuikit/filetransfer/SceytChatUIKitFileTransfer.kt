package com.sceyt.chatuikit.filetransfer

import com.sceyt.chatuikit.filetransfer.defaults.DefaultFileTransferDestinationProvider
import com.sceyt.chatuikit.filetransfer.defaults.DefaultFileTransferTransport
import com.sceyt.chatuikit.persistence.lazyVar

class SceytChatUIKitFileTransfer {
    var transport: FileTransferTransport by lazyVar {
        DefaultFileTransferTransport()
    }

    var destinationProvider: FileTransferDestinationProvider by lazyVar {
        DefaultFileTransferDestinationProvider
    }
}
