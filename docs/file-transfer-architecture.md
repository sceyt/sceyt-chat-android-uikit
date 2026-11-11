# File transfer architecture

UIKit owns attachments and their persistence. A configured provider owns uploading, downloading,
and the technical state needed to retry its work. Applications can use the default Chat SDK
provider or implement their own.

## Responsibilities

| Component | Owns |
| --- | --- |
| UIKit coordinators | Queueing, shared uploads, attachment state, and callbacks. |
| UploadPreparer | Existing checksum lookup and resize/transcode preparation. |
| UIKit persistence | Saving the prepared path, size, and app metadata before upload starts. |
| AttachmentTransferRouter | Building requests and calling the configured provider. |
| FileTransferProvider | Network execution, encryption/decryption choices, recovery, and cancellation. |
| FileEncryption | The UIKit facade for Chat SDK cryptography, available to custom providers. |
| Chat SDK native downloader | HTTP downloads, authenticated decryption, partial files, and download checkpoints. |

UIKit does not interpret a custom application's encryption data. The provider returns metadata
entries from its preparation hook and reads its own keys from the attachment on later calls.

## Public requests

Both requests contain the attachment. Providers read their saved values from
`request.attachment.metadata`; neither request duplicates metadata in a separate field.

```kotlin
enum class FileTransferType {
    PLAIN,
    ENCRYPTED,
}

data class FileUploadRequest(
    val operationId: String,
    val sourceFile: File,
    val attachment: SceytAttachment,
    val type: FileTransferType = FileTransferType.PLAIN,
    val isSharedUpload: Boolean = false,
)

data class FileDownloadRequest(
    val operationId: String,
    val url: String,
    val destinationFile: File,
    val attachment: SceytAttachment,
    val type: FileTransferType = FileTransferType.PLAIN,
    val headers: Map<String, String> = emptyMap(),
)
```

The actual request classes exclude attachment metadata from `toString()`.

- `sourceFile` is the explicit prepared plaintext file for this upload. It can differ from `attachment.filePath`.
- The file name comes from `attachment.name`; the request does not duplicate the name or MIME type.
- `url` is the remote reference for this download; a provider can replace it with a resolved URL without changing `attachment.url`.
- `destinationFile` is the final usable plaintext path. The provider must finish decryption before success.
- `operationId` identifies a logical transfer across attempts. Upload and download IDs are distinct.
- The attachment's `fileSize` is its plaintext size; there is no separate download size field.
- `encryptionEnabled` selects `FileTransferType.ENCRYPTED` for both uploads and downloads when true,
  and `PLAIN` when false. Direct requests default to `PLAIN`.
- Encrypted downloads require valid encryption metadata. Missing or malformed data fails before
  downloading; there is no plaintext fallback. Older plain attachments cannot be downloaded in this mode.
- Both requests use the same `FileTransferType`; there is no nullable transfer context.

## Pre-upload hook

```kotlin
interface FileTransferProvider {
    suspend fun prepareUpload(request: FileUploadRequest): Map<String, String> = emptyMap()

    // upload(), download(), pause(), resume(), cancel()
}

SceytChatUIKit.fileTransfer.provider = AppFileTransferProvider()
```

The default method returns an empty map, so UIKit does not perform an app-data write. There is no separate
preparer property on `SceytChatUIKitFileTransfer`.

The method runs after resizing/transcoding, so `request.sourceFile` is the file that will be uploaded.
Preparation and upload must use the same source and matching app data; retries must restore that pairing.
On first use, the attachment may already contain captions, dimensions, or other metadata. On another
attempt, including after restarting the app, `request.attachment.metadata` also contains the saved
provider entries. The provider reads and restores its own values.

The hook returns data; it does not write attachment metadata itself. It can create encryption
parameters, reserve an upload slot, or return other information its provider needs.

### Save before upload

```text
AttachmentUploadCoordinator.performUpload()
    |
    v
AttachmentTransferRouter.upload()
    |
    v
UploadPreparer.prepare()             existing resize/transcode and prepared-file reuse
    |
    v
FileTransferProvider.prepareUpload(request)
    |
    | returned Map<String, String>
    v
AttachmentUploadCoordinator.savePreparedUpload()
    |
    +--> savePreparedAttachment() for every shared caller
    |
    +--> PersistenceAttachmentLogic.updateAttachmentFilePathAndMetadata()
               |
               v
          attachment database
    |
    | database write has completed
    v
FileTransferProvider.upload(request)
    |
    v
FileTransferResult.Success(url)
```

UIKit adds each returned key and string value directly to the attachment's JSON metadata. Matching
keys are replaced; unrelated fields, including nested objects, keep their existing values. There is
no wrapper key. Each shared caller keeps its own unrelated metadata.

```json
{
  "caption": "example",
  "szw": 1280,
  "szh": 720,
  "provider": "my-app",
  "app_encryption": "application-defined serialized parameters"
}
```

The provider chooses its keys. Map values are stored as JSON strings. A provider can serialize
structured parameters into a string and parse them itself. Existing metadata fields keep their
JSON types; serialization whitespace is not part of the contract.

If preparation or saving fails, the provider is not called. When a shared caller joins after
preparation, UIKit saves the same app data for that caller before completing its transfer.

### Retry and restart

```text
First attempt:
    prepareUpload(request.attachment with its existing metadata)
        -> app creates parameters
        -> UIKit merges and saves the returned map
        -> provider.upload(request), reading request.attachment.metadata

Retry after interruption or app restart:
    load attachment from UIKit database
        -> prepareUpload(request.attachment with saved provider entries)
        -> app returns its existing parameters
        -> UIKit merges and saves the returned map
        -> provider.upload(request), reading request.attachment.metadata
        -> provider restores its own progress

Network returns after an offline failure (app closed or restarted):
    upload worker fails without a validated network
        -> PendingAttachmentRetryWorker queued once (unique, NetworkType.CONNECTED)
        -> network returns: it schedules UploadAndSendAttachmentWorker (KEEP) per pending,
           unpaused, non-forwarded file message
        -> each upload worker uploads, connects chat if needed, and sends
```

The app must create new parameters if the prepared source changes. It must not reuse an old key
and nonce to encrypt changed plaintext. UIKit cannot validate an arbitrary app encryption format.

The method runs before each actual upload attempt. It should return existing data without repeating
network work while that data is valid. Completed checksum matches skip both provider methods when
the cached upload used the same mode. An encrypted match restores the metadata saved with its URL;
set `encryptionEnabled = true` when using custom encryption. Prepared media files can still be
reused. Disable `preventDuplicateAttachmentUpload` if a send requires fresh provider preparation
every time.

The saved entries travel in attachment metadata when the message is sent and forwarded. Do not put
credentials for a storage service, signed upload URLs, or large transfer journals in it.

## Upload execution

### Default Chat SDK provider

```text
DefaultFileTransferProvider.prepareUpload()
    -> PLAIN: empty map
    -> ENCRYPTED: create or reuse a native encryption checkpoint
         -> keep recovery data locally in NativeUploadCheckpointStore
         -> return mapOf("sceyt.enc" to checkpoint.attachmentKey)

UIKit merges and saves the returned entries

DefaultFileTransferProvider.upload()
    -> PLAIN: FileTransferUtility.uploadFile()
    -> ENCRYPTED: restoreEncryption() using the saved attachment key, header, and recovery data
         -> FileTransferUtility.uploadEncryptedFile()
    -> return FileTransferResult.Success(url)

UIKit saves attachment.url
```

The native uploader is a single upload request; the Sceyt chat server does not provide multipart
uploads. A failed native upload restarts that request. It does not offer S3 part recovery.

The default encrypted uploader requires a session from the Chat SDK adapter. A custom encryption
implementation belongs with a provider that understands it.

### Custom provider

A custom provider reads and validates its own keys in `request.attachment.metadata`. It supplies
encryption information through `prepareUpload()` and returns `FileTransferResult.Success(url)` from
`upload()`. UIKit keeps the saved metadata without interpreting it. There is no `FileUploadResult`
or encryption-specific result handling in the router or executor.

UIKit does not append file keys to uploaded bytes. The encryption implementation defines the
ciphertext format. Attachment metadata currently carries keys directly, so its confidentiality
depends on the protection of chat messages and the attachment database.

## Encryption facade

Custom providers can use UIKit's SDK-backed cryptography without importing Chat SDK types.

```kotlin
interface FileEncryption {
    suspend fun prepareEncryption(file: File): FileEncryptionSession

    suspend fun restoreEncryption(
        file: File,
        checkpoint: FileEncryptionCheckpoint,
    ): FileEncryptionSession

    suspend fun prepareDecryption(
        encryptedFile: File,
        attachmentKey: String,
    ): FileDecryptionSession
}

interface FileEncryptionSession : AutoCloseable {
    val sourceId: String
    val plaintextSize: Long
    val ciphertextSize: Long
    val attachmentKey: String

    suspend fun read(offset: Long, length: Int): ByteArray
    fun checkpoint(): FileEncryptionCheckpoint
}

interface FileDecryptionSession : AutoCloseable {
    val sourceId: String
    val plaintextSize: Long

    suspend fun read(offset: Long, length: Int): ByteArray
}

class FileEncryptionCheckpoint(
    val attachmentKey: String,
    val header: ByteArray,
    val data: String,
)
```

Encryption reads use ciphertext offsets and byte counts, including format overhead. Restoring the
same session against the unchanged source produces identical bytes. Close sessions after use.
The adapter runs blocking SDK calls on the I/O dispatcher.

The SDK's `PreparedUpload.cipher.attachmentKey` is already Base64URL. The adapter exposes it as
`session.attachmentKey`. UIKit passes that string through as metadata.

A checkpoint contains the attachment key, the 72-byte header, and opaque SDK recovery data.
The default provider saves the header and recovery data locally with a key fingerprint. It reads
the key from attachment metadata when restoring the upload and retains recovery after interruption.
It removes the local record on success or explicit cancellation.

Native downloads accept the attachment key directly and fetch the header from the stored object.
Local decryption uses `AttachmentCipher.restore(attachmentKey, header)` and its
`newDecryptingRandomAccessStream()` view. SDK views own their sources after successful creation.

## Download execution

```text
AttachmentDownloadCoordinator
    -> AttachmentTransferRouter.download()
         -> build FileDownloadRequest(type selected by encryptionEnabled)
         -> configured FileTransferProvider.download()
              |
              +--> DefaultFileTransferProvider
              |      -> ChatSdkFileDownloader
              |           -> PLAIN: MediaDownload.download()
              |           -> ENCRYPTED: read required enc, then MediaDownload.downloadEncrypted()
              |           -> native HTTP, verification, decryption, and file writing
              |
              +--> AppFileTransferProvider
                     -> follow request.type and read request.attachment.metadata
                     -> fetch and decrypt through its chosen implementation
         <- final plaintext path
    -> UIKit updates local path and attachment state
```

The router selects the requested mode from configuration without parsing encryption algorithms or
keys. The default provider reads the SDK attachment key from `sceyt.enc`. Custom providers can use their
own keys and schema in attachment metadata.

Native downloads write verified plaintext into `destination.part` and keep their range map in
`destination.part.json`. Completion publishes `destination`. There is no complete ciphertext
temporary file in this route. An incomplete final file left by older versions is removed before
native download begins; complete files are reused by the coordinator.

On coroutine cancellation, the adapter stops native writers and keeps resumable partial data.
Explicit cancellation discards the native download by operation ID. If native reports
`WAITING_FOR_NETWORK` after its retries, the adapter returns `Interrupted` and pauses native writers,
retaining verified ranges. UIKit shows the existing download icon; the user taps it to retry.

Playback still waits for the complete local file. Player integration for streaming and seeking
is separate work.

## Video previews

```text
UploadAndSendAttachmentWorker
    -> AttachmentPreviewTransfer.upload(video)          op: upload:<messageTid>:preview
         -> frame from the original video (JPEG)
         -> AttachmentTransferRouter.upload()           prepareUpload + upload, preparer skips previews
         -> save the preview row (url, metadata)
    -> existing video upload                            op: upload:<messageTid>
    -> send [video with metadata "pvw" = preview url, preview]

Video message holder (video not downloaded yet)
    -> AttachmentThumbCoordinator
         -> AttachmentPreviewTransfer.loadPreviewPath(video)  op: download:<messageTid>:preview
              -> preview row whose url equals the video's "pvw"
              -> AttachmentTransferRouter.download()
              -> save the preview row's filePath
         -> ThumbLoaded for the video
```

The video's `pvw` metadata value is its preview attachment's URL, so each video finds its own
preview. The preview is stored like a link attachment: its own row, no transfer-state row, and it
is skipped by every transfer-state update keyed by `messageTid`. A failed preview upload fails the message
before the video starts. A retry uploads only the missing part. A video with no extractable frame
is sent without a preview.

## Waafi example

A Waafi adapter using the map-based hook follows this flow. Its preparation, upload, and download
implementations read their own keys from the attachment; the UIKit requests have no separate metadata field.

```text
WaafiFileTransferProvider.prepareUpload(request)
    -> WaafiMultipartUploader.prepareUpload(request)
    -> reuse the saved attachment key when it matches the source and upload journal
    -> otherwise FileEncryption.prepareEncryption()
    -> return its SDK attachment key under "waafi.enc"
    -> keep source identity and checkpoint data in its local upload journal
    -> UIKit merges the returned map into attachment metadata and saves it

WaafiMultipartUploader.upload()
    -> read the saved parameters from request.attachment.metadata
    -> restoreEncryption() using the saved key and current technical checkpoint
    -> init multipart upload
    -> read ciphertext parts; PUT up to three parts concurrently
    -> save each returned ETag
    -> complete multipart upload
    -> return FileTransferResult.Success(objectKey)

WaafiFileTransferProvider.download()
    -> for ENCRYPTED, read Waafi's required encryption information from request.attachment.metadata
    -> pass it as "sceyt.enc" on an in-memory copy for the default native downloader
    -> resolve the URL and add chat headers for the configured origin
    -> WaafiMultipartDownloader
         encrypted: delegate to the native downloader
         plaintext: parallel HTTP ranges, with native fallback when needed
    -> return the plaintext path
```

The stored Waafi attachment keeps its `waafi.enc` attachment key. The default provider reads the same
SDK attachment key format from `sceyt.enc`. Plain requests skip encryption metadata.

Waafi's private upload journal keeps the source identity, key hash, header, SDK recovery data, upload ID,
object key, part URLs, ETags, and phase. A different attachment key invalidates a previous multipart session.

Waafi uses its REST API for init, complete, and abort, and OkHttp for presigned PUTs. No low-level
AWS SDK is needed for that upload path. Its ciphertext part size starts at 5 MiB and increases if
necessary for the part-count limit; the last part may be smaller. Encryption's 64 KiB segments
are independent of S3 part boundaries.

After interruption, Waafi skips accepted parts and resends unfinished parts. It saves updated
checkpoint data before each PUT and the exact quoted ETag after each accepted response.
A lost completion response retains the completing phase so a retry attempts completion again.
The backend's completion recovery and URL-renewal capabilities still determine what can be
recovered without restarting multipart upload.

## Transfer lifecycle

`FileTransferProvider` exposes suspend `upload` and `download` methods and three controls:

- `pause(operationId)`: true means the invocation remains alive and can resume.
- `resume(operationId)`: true means that paused invocation continues.
- `cancel(operationId)`: explicit abandonment, including saved inactive work.

A false pause/resume result allows UIKit to stop the attempt and call the suspend method again.
After an app restart, UIKit calls the suspend method again because the original invocation no
longer exists.

`FileTransferResult.Success` contains the uploaded reference or final plaintext path.
`Interrupted` retains recoverable work; `Failure` reports an unsuccessful attempt.
Coroutine cancellation propagates as cancellation and stops active byte work before the invocation
finishes. It does not itself mean the user has abandoned the transfer.

## Verification

- JVM tests cover preparation order, persistence failures, shared callers, metadata preservation,
  request routing, native handoff, and multipart recovery against local HTTP fixtures.
- Device tests run native cryptography and HTTP download, reject tampering and wrong keys/sizes,
  and verify cancellation/resume.
- `AttachmentUploadPreparationAndroidTest` uses the real attachment database and supports
  `preUploadPhase=prepare` followed by `preUploadPhase=resume` in separate instrumentation
  processes. It verifies that the saved parameters produce identical encrypted bytes after restart.

Live backend interoperability and streaming-player integration are separate from these local tests.

The device regression `nativeRedirectDoesNotForwardChatCredentialsToAnotherOrigin` currently fails:
the native SDK forwards chat headers across an origin-changing redirect. This is a separate SDK
issue, reproduced on both test devices; the pre-upload change does not modify redirect handling.
