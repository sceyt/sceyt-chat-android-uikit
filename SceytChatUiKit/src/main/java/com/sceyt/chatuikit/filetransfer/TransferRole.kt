package com.sceyt.chatuikit.filetransfer

/**
 * Which file of an attachment a transfer moves. An attachment can own more than one file, like a
 * video and the poster frame shown before the video is downloaded, and those transfer separately.
 *
 * A transport must treat two operations of the same attachment but a different role as unrelated,
 * and must not reuse one backend identifier for both.
 */
enum class TransferRole(val key: String) {
    Main("main"),
    Thumbnail("thumb"),
}