# File transfer architecture: UIKit, Chat SDK and Waafi

Audience: UIKit, Chat SDK, Waafi Android and backend developers.

UIKit supports opt-in encrypted uploads. Waafi implements multipart uploads with presigned URLs
and resumable HTTP range downloads. UIKit still coordinates decryption after the transport has
downloaded the ciphertext. The native SDK download operation and the transport ownership switch
are planned work described in section 10.

## 1. Purpose

UIKit provides one file-transfer interface. Its default implementation uses the Chat SDK's native transfers. Applications such as Waafi can replace that implementation to use their own backend and multipart protocol.

Waafi accesses encryption through a UIKit interface. UIKit's internal adapter delegates cryptography to the Chat SDK. Waafi does not need to import Chat SDK encryption types.

The Sceyt chat server currently supports a single upload request, not S3 multipart uploads. Waafi's backend provides multipart initiation, presigned part URLs, completion and abort. Those are separate server capabilities.

## 2. Ownership

| Owner | Responsibilities |
| --- | --- |
| UIKit | Attachment preparation, transport selection, transfer scheduling, displayed progress/state, attachment database records and encryption metadata assembly. |
| UIKit's Chat SDK adapter | Map UIKit's encryption contracts to Chat SDK types and calls. |
| Chat SDK | File keys, encryption format, ciphertext range generation, authentication, decryption and native transfer execution. |
| Waafi transport | Custom upload/download execution, multipart planning, HTTP requests, retries, saved sessions, pause/resume, abort and temporary-file cleanup. |
| Waafi backend | Generate object keys, initiate multipart uploads, sign URLs, complete uploads and abort uploads. |
| S3 | Accept part PUTs, return ETags and assemble the object after completion. |

UIKit keeps attachment records. The executing transport keeps its own technical recovery state. For example, UIKit saves the completed local file path, while Waafi saves its upload ID and accepted part ETags.

## 3. Upload call graph

```text
UIKit
  AttachmentUploadCoordinator / AttachmentDownloadCoordinator
                         |
                         v
              AttachmentTransferRouter
                         |
                         v
        SceytChatUIKitFileTransfer.transport
                         |
             +-----------+----------------+
             |                            |
             v                            v
 DefaultFileTransferTransport    WaafiFileTransferTransport
          [UIKit]                        [Waafi]
             |                            |
             v                            +--> FileEncryption [UIKit interface]
 Chat SDK native transfers                |          |
             |                            |          v
             v                            |  ChatSdkFileEncryption [UIKit internal]
 Sceyt chat server                        |          |
 Single upload request                    |          v
                                          |  Chat SDK cryptography
                                          |
                                          +--> WaafiMultipartUploader
                                                     |
                                                     +--> Waafi API: init
                                                     +--> S3: PUT parts
                                                     +--> Waafi API: complete/abort
```

Both transports implement the same `FileTransferTransport`. UIKit does not branch on the application name or know S3 part numbers.

## 4. Shared transfer contract

All types in this section belong to UIKit. Upload requests carry the prepared file and its metadata. Download requests carry a remote reference, destination, optional context and HTTP headers.

```kotlin
import java.io.File

interface FileTransferContext

fun interface FileTransferCallback {
    // Percentage from 0f to 100f.
    fun onProgress(progressPercent: Float)
}

enum class FileUploadType {
    PLAIN,
    ENCRYPTED,
}

// sourceFile is the final prepared plaintext file in both modes.
data class FileUploadRequest(
    val operationId: String,
    val sourceFile: File,
    val fileName: String,
    val mimeType: String?,
    val context: FileTransferContext? = null,
    val type: FileUploadType = FileUploadType.PLAIN,
)

data class FileUploadResult(
    // May be a URL or the remote reference understood by this transport.
    val url: String,
    val encryptionInfo: FileEncryptionInfo? = null,
)

data class FileDownloadRequest(
    val operationId: String,
    val url: String,
    val destinationFile: File,
    val context: FileTransferContext? = null,
    val headers: Map<String, String> = emptyMap(),
)

sealed interface FileTransferResult<out T> {
    data class Success<T>(val result: T) : FileTransferResult<T>
    data class Failure(val error: Throwable) : FileTransferResult<Nothing>
    data class Interrupted(val error: Throwable) : FileTransferResult<Nothing>
}

interface FileTransferTransport {
    suspend fun upload(
        request: FileUploadRequest,
        callback: FileTransferCallback,
    ): FileTransferResult<FileUploadResult>

    suspend fun download(
        request: FileDownloadRequest,
        callback: FileTransferCallback,
    ): FileTransferResult<String>

    fun pause(operationId: String): Boolean = false
    fun resume(operationId: String): Boolean = false

    // Explicit abandonment; uploads use this now. Download migration is pending.
    fun cancel(operationId: String)
}
```

### Execution rules

- `upload` receives the final prepared plaintext file. The transport owns any encryption session it creates. An encrypted success includes the encryption information for the uploaded bytes.
- `download` returns the path where the transport wrote the remote bytes. For an encrypted attachment, UIKit uses that file to authenticate and publish plaintext before reporting attachment success.
- `operationId` identifies a logical transfer. Retries use that identity to locate transport-owned state. Upload and download operation IDs must remain distinct.
- `pause` returning `true` means the same invocation remains available for `resume`. Returning `false` allows UIKit to stop the attempt and retry later.
- `resume` returning `true` means that paused invocation continues. After a process restart there is no invocation to resume; UIKit calls `upload` or `download` again, and the transport restores its saved state if supported.
- Coroutine cancellation stops reads, writes and network work. It propagates as `CancellationException`. On its own, it does not mean the user has abandoned the file; it can also be UIKit's pause fallback.
- Explicit upload cancellation calls `cancel(operationId)` and stops the calling coroutine. The implementation must handle either order, repeated calls and calls after an interrupted attempt has already returned. Download cancellation keeps its existing implementation in this phase.
- `cancel` schedules cleanup without blocking the caller. Waafi aborts abandoned multipart uploads and can retry a failed remote abort. UIKit orders encrypted download staging cleanup after the current attempt stops.
- `Interrupted` means recoverable work is retained for another attempt. `Failure` means this attempt cannot complete normally. Neither result is a successful uploaded or downloaded attachment.
- A transport must stop its current byte operations before its suspended method returns or cancellation completes. Old callbacks must not update a replacement attempt.

The transport owns upload recovery and decides how long to retain completed upload results. UIKit calls `cancel(operationId)` for explicit upload abandonment.

## 5. UIKit encryption contract

This interface supplies cryptography to custom transports. It does not initiate HTTP requests or store multipart sessions.

```kotlin
import java.io.File

interface FileEncryption {
    suspend fun prepareEncryption(file: File): FileEncryptionSession

    suspend fun restoreEncryption(
        file: File,
        checkpoint: FileEncryptionCheckpoint,
    ): FileEncryptionSession

    suspend fun prepareDecryption(
        encryptedFile: File,
        encryptionInfo: FileEncryptionInfo,
        plaintextSize: Long,
    ): FileDecryptionSession
}

interface FileEncryptionSession : AutoCloseable {
    val sourceId: String
    val plaintextSize: Long
    val ciphertextSize: Long
    val encryptionInfo: FileEncryptionInfo

    suspend fun read(offset: Long, length: Int): ByteArray

    fun checkpoint(): FileEncryptionCheckpoint

    override fun close()
}

interface FileDecryptionSession : AutoCloseable {
    val sourceId: String
    val plaintextSize: Long

    suspend fun read(offset: Long, length: Int): ByteArray

    override fun close()
}

data class FileEncryptionInfo(
    val algorithm: String,
    val attachmentId: String,
    val key: String,
)

data class FileEncryptionCheckpoint(
    val encryptionInfo: FileEncryptionInfo,
    val plaintextSize: Long,
    val data: String,
)
```

### Session rules

- `prepareEncryption` creates a new session and key. The SDK generates ciphertext when the caller reads a range.
- `restoreEncryption` preserves the key, attachment ID and encryption parameters so accepted S3 parts remain compatible with later parts.
- `prepareDecryption` opens a completed local ciphertext file. The decryption session reads authenticated plaintext ranges and closes its resources when finished.
- A changed source file must not silently continue an old multipart session. The SDK checks the source; Waafi handles the failure by abandoning that multipart session and starting a new one when appropriate.
- `sourceId` identifies the encrypted source and remains stable when the same session is restored.
- `read` takes a ciphertext offset and byte count, including encryption overhead. It returns exactly that range. Bounds are `offset >= 0`, `length >= 0`, and `length <= ciphertextSize - offset`.
- Repeated reads of the same valid range produce identical bytes. The adapter preserves SDK source validation and cancellation behavior.
- `checkpoint` returns a UIKit-owned `FileEncryptionCheckpoint`: typed encryption information, the original plaintext size and opaque SDK `data`. Waafi serializes this object in its private journal. It does not parse the SDK-specific `data` or depend on SDK model classes.
- Waafi saves the updated encryption checkpoint after a read and before submitting that range to S3. This preserves SDK source-validation information even if the application stops immediately after the PUT.
- `close` releases the session's resources. It does not call S3 abort or delete Waafi's journal.
- Blocking SDK calls run off the main thread inside the UIKit adapter. No production `runBlocking` bridge is required.

### Mapping to the current SDK

```text
UIKit FileEncryption.prepareEncryption(file)
  -> ChatSdkFileEncryption
  -> SDK FileEncryptionProvider.prepareUpload(file, generatedAttachmentId)

UIKit FileEncryption.restoreEncryption(file, checkpoint)
  -> ChatSdkFileEncryption
  -> SDK FileEncryptionProvider.restoreUpload(file, mappedTypedCheckpoint)

UIKit FileEncryptionSession.read(offset, length)
  -> SDK PreparedUpload.readCiphertextRange(offset, length)

UIKit encryptionInfo.algorithm    <- SDK encryptionInfo.format
UIKit encryptionInfo.attachmentId <- SDK encryptionInfo.data.uniqueId
UIKit encryptionInfo.key          <- SDK encryptionInfo.data.key
```

The UIKit `FileEncryptionInfo` is a separate public type from the SDK class with the same name. Waafi works with the UIKit types.

## 6. Configuration and routing

`SceytChatUIKit.fileTransfer` provides the upload encryption setting, transport, destination provider and SDK-backed `FileEncryption` facade.

`AttachmentTransferRouter` prepares files, selects the configured transport and translates attachment information into requests. It does not create encrypted readers, persist SDK checkpoints, calculate multipart parts or manage upload IDs.

```text
New upload:
  encryptUploads = false -> FileUploadRequest(type = FileUploadType.PLAIN) [default]
  encryptUploads = true  -> FileUploadRequest(type = FileUploadType.ENCRYPTED)

Incoming download:
  no enc metadata -> transport downloads directly to the destination
  enc metadata    -> UIKit passes encryption info and plaintext size to its download helper
```

Apps opt in by setting `SceytChatUIKit.fileTransfer.encryptUploads = true`. Incoming encrypted files follow their attachment metadata.

Encrypted upload retries reuse an existing prepared media file from the active upload, saved attachment or local checksum cache. UIKit uploads that file again through the transport without reusing a remote URL. Keeping the same source file lets Waafi restore the same multipart session and skip accepted parts.

Coordinators continue managing scheduling, shared callers, progress and attachment state. `TransferTask` refactoring is outside this change.

## 7. Native upload flow

```text
AttachmentUploadCoordinator
  -> AttachmentTransferRouter prepares/resizes/transcodes the file
  -> DefaultFileTransferTransport.upload(request)
       Plain     -> ChatClient.uploadCancellable(...)
       Encrypted -> prepare SDK session, then ChatClient.uploadEncrypted(...)
  <- FileUploadResult(remoteReference, encryptionInfo?)
  -> UIKit saves URL, plaintext file size and enc metadata
  -> Existing pending-message flow sends the attachment
```

The default transport may use SDK types internally. Applications do not need those types to implement the public transport interface.

A single HTTP upload can stream encrypted records. It does not require a complete encrypted temporary file or server multipart support. Since the chat server has no multipart-upload protocol, a failed native upload restarts as a whole upload; do not promise S3-style part resume for this route.

## 8. Waafi multipart upload flow

```text
AttachmentTransferRouter
  -> WaafiFileTransferTransport.upload(request)
       -> WaafiMultipartUploader
            -> FileEncryption.restoreEncryption(file, checkpoint) or prepareEncryption(file)
            -> Calculate part count from ciphertextSize
            -> POST /chat/upload/multipart/init?parts=N
            -> Save objectKey, uploadId, URLs and encryption checkpoint
            -> For each unfinished part, up to three in parallel:
                 session.read(offset, length)
                 save updated encryption checkpoint
                 PUT bytes to that part's presigned URL
                 save partNumber + exact ETag
            -> POST /chat/upload/multipart/complete
            -> Save confirmed completed result in Waafi's journal
       <- FileUploadResult(remoteReference, encryptionInfo)
  -> UIKit saves the returned attachment information
```

### Part sizes and bounded memory

The agreed SDK format uses 64 KiB encryption records. Start S3 planning with 5 MiB ciphertext parts: `5 * 1024 * 1024 = 5,242,880` bytes. These sizes are independent; S3 part boundaries may cut across encryption records. The reader must return exact ciphertext slices, rather than appending an extra encryption header to an already full S3 part.

For a positive ciphertext size:

```kotlin
val partSize = 5 * 1024 * 1024
val partCount = ((ciphertextSize - 1) / partSize + 1).toInt()

// partNumber is 1-based.
val offset = (partNumber - 1L) * partSize
val length = minOf(partSize.toLong(), ciphertextSize - offset).toInt()
```

Plan before calling `init` and retain that part size for the session. Increase it for files that would exceed the backend's 10,000-part limit. Every S3 part except the last must be at least 5 MiB. Waafi uploads up to three parts at a time and reads each part into a buffer. It does not create a full ciphertext file for upload.

### HTTP contract

| Operation | Request | Authentication/result |
| --- | --- | --- |
| Initiate | `POST /chat/upload/multipart/init?parts=N` | Waafi JWT; returns server-generated `objectKey`, `uploadId`, and `{partNumber, uploadUrl}` entries. |
| Upload part | `PUT <uploadUrl>` | Direct S3 request with ciphertext bytes and no Waafi Authorization header; capture `ETag`. |
| Complete | `POST /chat/upload/multipart/complete` | Waafi JWT; body contains `payload.objectKey`, `payload.uploadId` and `payload.parts`. |
| Abort | `POST /chat/upload/multipart/abort` | Waafi JWT; body contains `payload.objectKey` and `payload.uploadId`. |

Check the Waafi response envelope as well as HTTP status: the supplied contract identifies `payload.replyCode == 2001` as success. Submit each `{partNumber, etag}` pair exactly as received, preserving ETag quotes. Let Gson escape the JSON string.

Use Retrofit for the backend endpoints and a dedicated OkHttp client for S3 PUTs. The PUT client must not inherit Waafi's JWT interceptor. Forward any headers required by the signed request unchanged.

This upload path does not require AWS client credentials, client-side signing or a low-level AWS SDK. Existing AWS dependencies used by other Waafi features or downloads remain until those uses are separately migrated.

The backend also provides `GET /chat/upload/presigned-url` for a single-object PUT. Selecting that route for small files is Waafi policy and does not change UIKit's interface. Its request body should also stream when appropriate.

### Saved Waafi state

Keep the operation ID, source identity/path, SDK checkpoint, chosen part size, object key, upload ID, part URL expiry information, accepted part numbers/ETags and phase together in Waafi-owned storage. Persist initiation before the first part and each accepted receipt before moving to the next part.

Use the existing app-private file journal for the demo. A database migration can follow without exposing storage details through UIKit's public interfaces.

After successful completion, retain the completed remote reference and encryption information so a retry after a lost UIKit database write can return that same result. Waafi owns bounded retention and cleanup of completed records. There is no UIKit `uploadPersisted` acknowledgement. The retention period and cleanup trigger must be selected during implementation; recovery lasts only while the completed record is retained.

### Pause, retry and cancellation

| Event | Waafi behavior |
| --- | --- |
| Pause | Stop active byte work and retain the encryption session and accepted part receipts. Do not abort. |
| Temporary network failure | Retain state and retry unfinished work. If a PUT may have succeeded but its response was lost, submit the same part number and identical bytes again. |
| Resume after restart | Restore the same encryption session and multipart plan when the source and URLs remain usable. |
| Presigned URL expiry | The supplied URLs expire after 15 minutes. The contract has no renewal endpoint: abandon/abort the old multipart session, initiate another one and upload all parts again. Do not mix old receipts with the new upload ID. |
| Explicit `cancel(operationId)` | Stop active work, abort an unfinished remote upload and remove its local recovery state after cleanup is handled. This also works for a saved, inactive transfer. |
| Lost completion response | Keep an unconfirmed/completing state. Do not report success just because every part PUT succeeded. The backend contract does not yet specify idempotent completion or a status lookup. |

Expired URLs do not themselves mean the S3 upload was aborted. Failed or abandoned remote cleanup needs retry handling and the bucket's incomplete-upload cleanup policy.

## 9. Encryption metadata and renditions

The agreed outgoing wire format is:

```json
{
  "enc": {
    "v": 1,
    "alg": "tink-aes256-gcm-hkdf-64k+commit-v1",
    "attachmentId": "<main-file-binding-id>",
    "key": "<main-file-key-base64url>",
    "renditions": [
      {
        "role": "thumbnail",
        "attachmentId": "<thumbnail-binding-id>",
        "key": "<thumbnail-file-key-base64url>"
      }
    ]
  }
}
```

All displayed IDs and keys are placeholders. `attachmentId` is the encryption binding ID generated before encrypting a file; it need not equal a later server/database attachment ID. Keep the name `attachmentId` directly in Kotlin and JSON.

The default Chat SDK adapter generates this ID with `StreamingAeadFileEncryptionProvider.newUniqueId()`:
16 random bytes encoded as 22 base64url characters without padding. A UUID string is not accepted by
this SDK format. Restoring a session reuses its saved ID.

```kotlin
import com.google.gson.annotations.SerializedName

data class FileEncryptionMetadata(
    @SerializedName("v") val version: Int = 1,
    @SerializedName("alg") val algorithm: String,
    val attachmentId: String,
    val key: String,
    val renditions: List<FileEncryptionRendition> = emptyList(),
)

data class FileEncryptionRendition(
    val role: String,
    val attachmentId: String,
    val key: String,
)
```

Each session encrypts one file. A thumbnail has its own session, ID and key. UIKit knows which files belong together and assembles their upload results into `enc.renditions`; an individual file upload does not invent thumbnail keys.

`FileEncryptionInfo` describes one encrypted file. `FileEncryptionMetadata` describes the attachment group and wire format. These are different responsibilities now that renditions exist.

Preserve unrelated metadata such as dimensions and blur thumbnails when writing `enc`. Store the returned algorithm in `alg` and use it when reconstructing decryption information; do not silently replace it with a hardcoded format.

The main attachment's normal file-size field carries its original plaintext size, not its ciphertext size. For every separately encrypted rendition, developers must also agree where its remote reference and plaintext size live. The `enc.renditions` entries above contain neither; the download integration needs that information elsewhere in the attachment model. Renditions inherit the root algorithm in this wire format, so a different rendition algorithm cannot be silently introduced.

The `key` field currently carries the file key directly. Wrapping/envelope-provider work is not part of this proposal. Treat metadata and private checkpoints as key-bearing data and do not log their contents. Encryption keys are not appended to the S3 object bytes.

## 10. Download flow and native SDK work

Current flow for encrypted attachments:

```text
AttachmentTransferRouter.download()
  -> EncryptedFileDownloader.download(request, encryptionInfo, ...)
       -> FileTransferTransport.download() writes temporary ciphertext
       -> FileEncryption.prepareDecryption() opens the completed ciphertext
       -> FileDecryptionSession.read() supplies verified plaintext
  <- final plaintext path
```

Planned flow once the Chat SDK provides a complete encrypted download operation:

```text
AttachmentDownloadCoordinator
  -> AttachmentTransferRouter
       -> Parse enc into UIKit FileEncryptionInfo
       -> Select destination and supply original plaintext size
       -> configured FileTransferTransport.download(request)
            Default transport:
              -> native Chat SDK plain/encrypted downloader
            Waafi transport:
              -> Waafi-owned download execution
              -> access SDK decryption through UIKit's adapter
       <- final verified local plaintext path
  -> UIKit saves local path, downloaded state and displayed progress
```

In the planned flow, both transports return a complete usable file. The SDK or transport owns technical checkpoints, temporary files and authentication failures. UIKit updates the attachment database.

The native operation needs to cover network download, decryption, verification, file publication and cancellation before the default route switches. Waafi will continue to fetch its own bytes and use the SDK-backed decryption facade.

The first download implementation may keep the existing full encrypted download followed by local decryption. That temporarily needs both ciphertext and plaintext storage. The executing downloader cleans up staging files and publishes the destination only after verification. Changing to a single staging file or streaming playback is separate work.

The current product flow downloads the whole file before playback. Random-access encrypted reads remain available as a cryptographic capability, but video-player seeking integration is outside this implementation step.

## 11. Current code versus target design

| Area | Current UIKit phase | Remaining target work |
| --- | --- | --- |
| Upload | UIKit prepares the file and both transports accept `FileUploadRequest`. Encrypted success includes `FileUploadResult.encryptionInfo`. | Coordinate rollout before enabling uploads in release builds. |
| Encryption facade | Waafi uses UIKit's SDK-backed sessions and typed checkpoints. | Integrate the complete native download operation when available. |
| Upload recovery | UIKit reuses prepared files. Waafi owns backend checkpoints and completed-result retention. | Agree on backend completion recovery and URL renewal. |
| Cleanup contract | Waafi handles explicit upload abandonment. UIKit cleans encrypted download staging. | Move download lifecycle ownership to the transports. |
| Metadata | `{v, alg, attachmentId, key, renditions}` mapper; original file size and media fields preserved. | Coordinate the wire format across clients; integrate separately encrypted rendition files after their references/sizes are defined. |
| Waafi multipart lifecycle | Waafi REST handles init, complete and abort. Presigned PUTs send parts directly to S3. | Verify live backend interoperability. |
| Encrypted download | UIKit's router calls a helper that downloads ciphertext through the transport and publishes verified plaintext. | Switch both transports to return verified plaintext after the native SDK contract is ready. |

This is a targeted responsibility move. Preserve existing scheduling, sharing, persistence ordering, plain transfers and progress behavior. The previous implementation's passing tests do not validate the proposed API changes until they are implemented and the tests updated.

## 12. Implementation order and acceptance checks

1. Introduce the UIKit-owned encryption contracts and SDK adapter. Verify that prepare/restore return identical ciphertext for the same session and reject changed source files.
2. Change upload requests/results and move session ownership to the transports. Verify that the default encrypted route calls the native uploader, returns matching key information and closes its SDK session on success, error and cancellation.
3. Implement Waafi's REST multipart lifecycle with existing Retrofit/OkHttp conventions. Verify exact part ranges, quoted ETags, checkpoint order, response-envelope handling and JWT isolation with local fake HTTP servers.
4. Move recovery journals into Waafi and use common `pause`, `resume` and `cancel` controls. Verify interruption, process-style restore, source changes, URL expiry, explicit cancellation of saved transfers and reuse of retained completed results.
5. Update metadata assembly and coordinate the new wire format across clients. Verify round trips, preservation of unrelated metadata, plaintext sizes and independent rendition keys. Finalize rendition remote references and size storage before enabling encrypted thumbnails.
6. Integrate native encrypted download once its contract is available. Verify successful decryption, wrong key/ID/size, authentication failure, cancellation, temporary-file cleanup and publication ordering through both transports.
7. Run existing plain-transfer and coordinator regression tests. Then verify an actual send/download between two devices, including both directions and an interrupted large file.

Verification in this phase uses local tests and compilation. Native SDK encryption is mocked in JVM tests; real native cryptography and two-device interoperability require separate device verification. No live backend uploads, commits or pushes are part of this UIKit implementation step.

## 13. Backend details still needed

- How a returned `objectKey` becomes the remote reference used by downloads. The init API returns a key; the supplied complete contract does not define a final downloadable URL payload.
- Completion recovery when the complete request succeeds but the client loses its response. Confirm an idempotent retry contract or add a status lookup; until then the client cannot reliably declare that ambiguous case successful.
- URL renewal for an existing upload ID if long pauses must resume without reuploading every part. The current implementation can fall back to abort/new-init/full reupload.
- Ownership and timing for cleaning up unfinished uploads when an initiation response is lost, and the retention policy for Waafi's locally completed records.

## 14. Source references

- Waafi API contract supplied for this design: `chat-upload-api.txt`.
- Current UIKit implementation: `SceytChatUiKit/src/main/java/com/sceyt/chatuikit/filetransfer/` and `persistence/logicimpl/AttachmentTransferRouter.kt`.
- Current SDK contracts: `com.sceyt.chat.fileencryption.FileEncryptionProvider`, `PreparedUpload`, `FileEncryptionCheckpoint` and `FileEncryptionInfo`.
- Current Waafi implementation: `WaafiFileTransferTransport`, `WaafiMultipartUploader`, `ChatPartUploader` and `WaafiMultipartDownloader`.
- [AWS: S3 multipart upload limits](https://docs.aws.amazon.com/AmazonS3/latest/userguide/qfacts.html).
- [AWS: Multipart upload behavior and part ETags](https://docs.aws.amazon.com/AmazonS3/latest/userguide/mpuoverview.html).
- [AWS: Uploading through presigned URLs](https://docs.aws.amazon.com/AmazonS3/latest/userguide/PresignedUrlUploadObject.html).
