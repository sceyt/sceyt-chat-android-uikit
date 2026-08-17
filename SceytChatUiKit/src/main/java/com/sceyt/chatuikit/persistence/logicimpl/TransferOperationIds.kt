package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chatuikit.filetransfer.TransferRole

/**
 * Operation ids are derived, not stored, so pause and resume can find a running transfer from an
 * attachment alone, and so a transport can keep matching a transfer after a process restart.
 * The message and the role together identify an operation, which lets one attachment transfer
 * several files at once.
 */
internal fun uploadOperationId(
    messageTid: Long,
    role: TransferRole = TransferRole.Main,
): String = "upload:$messageTid:${role.key}"

internal fun downloadOperationId(
    messageTid: Long,
    role: TransferRole = TransferRole.Main,
): String = "download:$messageTid:${role.key}"