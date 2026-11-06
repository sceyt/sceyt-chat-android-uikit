package com.sceyt.chatuikit.filetransfer

import com.sceyt.chatuikit.filetransfer.defaults.DefaultFileTransferDestinationProvider
import com.sceyt.chatuikit.filetransfer.defaults.DefaultFileTransferProvider
import com.sceyt.chatuikit.filetransfer.encryption.ChatSdkFileEncryption
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryption
import com.sceyt.chatuikit.persistence.lazyVar

/** Configures attachment uploads, downloads, encryption, and local download destinations. */
class SceytChatUIKitFileTransfer {
    /**
     * Selects [FileTransferType.ENCRYPTED] for both upload and download requests when enabled.
     * Defaults to false, which selects [FileTransferType.PLAIN].
     * Encrypted downloads fail when required encryption data is missing or invalid.
     */
    var encryptionEnabled: Boolean = false

    /**
     * Cryptography facade available to [provider]. Defaults to [ChatSdkFileEncryption].
     * Custom encryption implementations require a provider that supports them.
     */
    var encryption: FileEncryption = ChatSdkFileEncryption()

    /**
     * Handles upload preparation, uploads, downloads, and transfer controls.
     * Defaults to [DefaultFileTransferProvider], which transfers files through the Chat SDK.
     * Replace it to use a custom backend while UIKit keeps coordination and attachment persistence.
     */
    var provider: FileTransferProvider by lazyVar {
        DefaultFileTransferProvider()
    }

    /**
     * Selects the final plaintext destination for downloaded attachments.
     * Defaults to [DefaultFileTransferDestinationProvider].
     * Paths must be stable for the same attachment and unique between attachments to support reuse and resume.
     */
    var destinationProvider: FileTransferDestinationProvider by lazyVar {
        DefaultFileTransferDestinationProvider
    }
}
