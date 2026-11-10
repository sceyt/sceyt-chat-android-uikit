# File transfer architecture

UIKit owns attachment scheduling, preparation and persistence. Upload providers receive a prepared
plaintext file and own encryption sessions and backend recovery. Download providers return the final
plaintext path and own decryption. An optional pre-upload hook lets applications return their own
parameters, which UIKit saves before the provider starts.

## Responsibilities

| UI kit | Customer provider |
| --- | --- |
| Local-file and checksum checks | Upload bytes to customer storage |
| Upload ordering and duplicate suppression | Download bytes from customer storage |
| Image resizing and video transcoding | Report normalized progress |
| Shared-upload coalescing | Pause or resume a native transfer when supported |
| Transfer state, attachment persistence and callbacks | Upload encryption, backend checkpoints and explicit-abandonment cleanup |
| Download destination selection | Return the remote reference or local destination path |

The provider must not create its own upload queue or implement shared-upload policy.

## Ownership

```text
FileTransferServiceImpl / TransferTask
  ├─ AttachmentUploadCoordinator
  │    upload FIFO, shared participants, pause/resume and attempt lifetime
  │    └─ AttachmentTransferRouter.upload
  │         ├─ UploadPreparer: checksum reuse, resizing, and transcoding
  │         ├─ FileTransferProvider.prepareUpload: app data for the prepared source
  │         ├─ Save the app object in attachment metadata before upload
  │         └─ FileTransferProvider.upload(preparedFile)
  │              ├─ Default: native Chat SDK upload
  │              └─ Custom: UIKit FileEncryption facade and customer backend
  ├─ AttachmentDownloadCoordinator
  │    file checks, destination, duplicate suppression and download attempts
  │    └─ AttachmentTransferRouter.download
  │         └─ FileTransferProvider.download
  │              ├─ Default: native HTTP download and authenticated decryption
  │              └─ Custom: interpret app metadata and return verified plaintext
  └─ AttachmentThumbCoordinator
       local thumbnail extraction and caching
```

The service calls the three attachment coordinators directly. Upload and download coordinators
delegate preparation and backend selection to `AttachmentTransferRouter`. Its `TransferExecutor`
executes the attempt and keeps pause, resume and discard bound to the selected provider. Each
coordinator owns its operations; only uploads have a FIFO. Downloads run independently.

One coroutine processes the upload coordinator's private command channel. It serializes short
state changes, including requests made from callbacks, without custom draining code or locks.
Preparation and byte transfer run in separate attempt jobs on the feature-owned IO scope.

Download attempts are registered before their jobs start. Short state changes use the coordinator's
lock; callbacks run outside it. A replacement waits for the previous job's cleanup before using the
same destination. Native pause/resume keeps the existing provider call when supported; otherwise
the coordinator cancels and restarts it. Explicit cancellation disables result delivery immediately,
even when backend cleanup is still running.

The service removes completed upload and download tasks before invoking their result and completion
callbacks. Coordinators report results; they do not remove tasks or complete worker listeners.
Pausing an upload ends its worker attempt without failing the attachment or discarding its native
transfer or shared result. Resume schedules a replacement worker, even if the previous worker is
still finishing. That worker rejoins through upload, preserving native resume and shared membership.
`FileTransferHelper` publishes state and persistence updates. Uploads and downloads have feature-owned
IO scopes; cancelling either does not cancel unrelated application work.

### Class locations

Paths below are relative to `com/sceyt/chatuikit`:

| Package | Classes and purpose |
| --- | --- |
| `filetransfer` | `SceytChatUIKitFileTransfer`: customer provider and destination configuration |
| `filetransfer` | `FileUploadRequest` and `FileDownloadRequest` with shared `FileTransferType`: requests with required attachments; `FileTransferProvider.prepareUpload`: metadata preparation hook; uploads return the remote reference |
| `filetransfer/defaults` | `DefaultFileTransferProvider`, `DefaultFileTransferDestinationProvider`: built-in backend and attachment destination policy |
| `persistence/file_transfer` | `FileTransferServiceImpl`, `TransferTask`: task registration, upload/download completion and worker listeners; `FileTransferHelper`: state and persistence publication |
| `persistence/logicimpl` | `AttachmentUploadCoordinator`, `AttachmentDownloadCoordinator`, `AttachmentThumbCoordinator`: attachment upload, download and local-thumbnail handling |
| `persistence/logicimpl` | `UploadPreparer`: checksum reuse and media preparation, without task registration or transfer scheduling |
| `persistence/logicimpl` | `AttachmentTransferRouter`: prepares requests and calls the configured provider; `TransferExecutor`: executes the attempt and controls the selected provider |
| `filetransfer/encryption` | UIKit crypto contracts, SDK adapter, and standard encryption metadata mapping |
| `filetransfer/download` | `ChatSdkFileDownloader`: native download lifecycle, progress, and controls |
| `persistence/logicimpl` | `FileTransferUtility`, `OkHttpDownloader`: SDK upload and HTTP download bridges |

## Configuration

Configure file transfer before starting chat transfers:

```kotlin
SceytChatUIKit.fileTransfer.provider = CustomerFileTransferProvider()
SceytChatUIKit.fileTransfer.destinationProvider = CustomerDestinationProvider

SceytChatUIKit.config.attachmentTransferConfig = AttachmentTransferConfig(
    imageDownloadDirectoryName = "Images",
    videoDownloadDirectoryName = "Videos",
    fileDownloadDirectoryName = "Files",
)

SceytChatUIKit.config.videoAttachmentResizeConfig = VideoResizeConfig(
    shortSideThreshold = 480,
    bitrateCoefficient = 0.09f,
)
```

`videoAttachmentResizeConfig` controls video preparation before upload. The output bitrate is
resolved in order: `bitrate`, then `bitrateCoefficient`, then `quality`. Only the first non-null
one applies, so leave the earlier ones null to use a later one.

Do not replace the provider while operations are active.

## Upload behavior

For a normal upload, the UI kit:

1. Adds the attachment to the sequential upload queue.
2. Reuses a completed upload when checksum reuse is enabled and a valid reference exists from an upload in the same mode. An encrypted reuse also restores the saved provider metadata.
3. Reuses prepared media, or resizes an image or transcodes a video when required.
4. Calls `provider.prepareUpload(request)` and merges and saves its returned metadata entries before proceeding.
5. Creates one `FileUploadRequest` with `type = PLAIN` or `ENCRYPTED`, selected by `encryptionEnabled`; it carries the prepared source and attachment with saved metadata.
6. Calls the provider, receives its URL, and owns progress, attachment persistence, completion, failure and queue advancement.

Preparation runs inside the admitted upload's coroutine, so a queued upload does not prepare early.
`UploadPreparer` receives an attachment and preparation callbacks, then returns the prepared
attachment or a reused URL. It does not own tasks, shared groups, or provider operations.
Ordinary uploads remain sequential and never implicitly join a shared group. Explicit shared uploads
run concurrently with that queue.

For shared attachments with the same original source, the UI kit starts one physical upload and distributes
its progress and result to the logical transfer tasks. Pausing one task does not pause the physical
upload while another task remains active. The physical upload is paused only after every task in
the shared group is paused. Resuming any task resumes the existing physical operation when
possible, otherwise the UI kit restarts it.

Deleting one participant leaves the others running; deleting the last cancels the physical operation.
If deletion leaves only paused participants, the coordinator retains the transfer with native pause
when supported, otherwise it cancels and restarts on resume. An already-paused transfer is kept as-is.
A successful result is retained by paused participants for their later resume. A finished or failed
group does not prevent a later share from starting. Sequential resumes wait for their queue turn.

Requests contain a required `attachment`. Uploads also carry `isSharedUpload`. Customer providers
use the request's source, destination, URL and operation ID for execution.

### App-owned pre-upload data

```kotlin
interface FileTransferProvider {
    suspend fun prepareUpload(request: FileUploadRequest): Map<String, String> = emptyMap()

    // upload(), download(), pause(), resume(), cancel()
}
```

Override the method on the configured provider. The default returns an empty map, which skips a metadata
write. There is no separate preparer property on `SceytChatUIKitFileTransfer`.

The hook receives the prepared source, after resizing/transcoding. UIKit merges the returned keys
and string values directly into attachment metadata, replacing matching keys and preserving other
fields. It waits for that database write before calling `upload()`. Preparation or persistence
failure prevents upload. There is no wrapper key.

On retry or app restart, the provider reads its saved keys from `request.attachment.metadata`.
Return those entries unchanged while the source remains valid; create new parameters when it
changes. Shared callers save the same entries while preserving their own captions and media data.
A completed checksum match skips both provider methods. Plain uploads reuse only plain URLs.
Encrypted uploads reuse only encrypted URLs, and UIKit merges and saves the metadata entries that
were returned for that upload, so receivers get the matching keys. Disable
`preventDuplicateAttachmentUpload` if a send needs fresh provider preparation every time.

Neither upload nor download requests have a separate metadata field. The provider reads and
validates its own values in `request.attachment.metadata`. It can serialize structured data into
a string under its own key. This supports encryption formats and keys that differ from UIKit's
standard `sceyt.enc` attachment key.

## Download behavior

The destination provider selects the local file before the transfer provider is called. It must return the
same destination for the same attachment, and different paths for different attachments. Partial
downloads survive pause and returned interruptions. Ordinary terminal failures delete the partial
destination, preserving the existing policy.

The UI kit skips the provider when a complete local file already exists. Otherwise it creates a
`FileDownloadRequest`, suppresses duplicate in-flight downloads, and owns transfer state and
result delivery.

Downloads for different messages remain concurrent. There is no download FIFO. The coordinator
does not automatically submit another failed call. Native performs network retries; once it waits
for connectivity, UIKit ends the attempt as `Interrupted` and the user can tap download to retry.

## Encrypted files and the Chat SDK

The SDK owns file keys,
the encryption format, its 64 KiB records and authentication. Applications use UIKit's
`FileEncryption`, `FileEncryptionSession` and `FileEncryptionCheckpoint` contracts.
`ChatSdkFileEncryption` maps these to SDK types internally and exposes the SDK-generated attachment key.
UIKit does not implement the file cipher or calculate encrypted record boundaries.

Enable encryption before starting transfers:

```kotlin
SceytChatUIKit.fileTransfer.apply {
    encryptionEnabled = true
    // Set provider here when using customer storage instead of the Chat SDK uploader.
}
```

The SDK's `PreparedUpload.cipher.attachmentKey` is already Base64URL and carries the key and attachment
ID. The provider returns that opaque string through `prepareUpload()`; UIKit stores it unchanged.
Base64 is not key protection: anyone who can read the attachment key can decrypt the file.

### Upload call chain

```text
AttachmentUploadCoordinator.performUpload
  -> AttachmentTransferRouter.upload
  -> UploadPreparer.prepare
       -> prepare media
       -> provider.prepareUpload(request)
       -> UIKit merges the returned map into attachment metadata and saves it
  -> FileUploadRequest(sourceFile, attachment, type = PLAIN or ENCRYPTED)
  -> FileTransferProvider.upload
       default encrypted route:
         FileEncryption.restoreEncryption -> SDK session -> ChatClient.uploadEncrypted
       custom encrypted route:
         FileEncryption.prepareEncryption/restoreEncryption -> read ranges -> customer backend
  <- FileTransferResult.Success(url)
  -> save the uploaded reference with the attachment's prepared metadata
```

The provider opens and closes the encryption session used for byte transfer. Its preparation hook
creates or reuses the token so UIKit saves it before upload. The router neither creates
ciphertext readers nor interprets encryption metadata. The default native uploader releases its session
on success, error or cancellation. It uses the bundled SDK adapter; another encryption
implementation requires a compatible custom provider.

The native upload is a single request. It streams encrypted bytes and does not need a complete
encrypted temporary file. A failed or cancelled native upload restarts as a whole upload; UIKit
does not claim durable multipart resume for the Sceyt chat server.

The default provider returns `mapOf(FileEncryptionMetadata.FIELD to session.attachmentKey)`. UIKit saves
the attachment key as a string alongside existing attachment metadata:

```json
{
  "tmb": "blur-thumbnail",
  "szw": 1280,
  "szh": 203,
  "sceyt.enc": "<SDK-generated-base64url-attachment-key>"
}
```

The SDK creates and restores the attachment key. UIKit passes it through to metadata. The file
itself stays binary ciphertext. The attachment's `fileSize` remains its plaintext size. Dimensions,
blur thumbnails and customer fields are preserved, including for shared uploads. The URL and matching
metadata are saved together, including in the checksum cache. An encrypted checksum match reuses that
URL with its saved metadata, whatever keys the provider chose, and is never reused as a plain URL.

Each encrypted file needs its own session. A video's preview is a separate attachment with its own
session and key; see [Video previews](#video-previews).

### Custom encryption sessions and multipart recovery

A custom provider can call `SceytChatUIKit.fileTransfer.encryption.prepareEncryption(file)`,
return `session.attachmentKey` under its own metadata key, and save the checkpoint's header and data locally.
After a restart, it rebuilds `FileEncryptionCheckpoint(savedKey, savedHeader, localData)` and restores the session:

```kotlin
val session = encryption.restoreEncryption(preparedFile, checkpoint)
try {
    val bytes = session.read(ciphertextOffset, length)
    val updated = session.checkpoint()
    // Persist technical recovery data before the PUT, then its ETag after acceptance.
} finally {
    session.close()
}
```

Both offsets and lengths address ciphertext, including encryption overhead. Reads are exact and
repeatable; SDK encryption records need not align with backend multipart parts.

`FileEncryptionCheckpoint` contains `attachmentKey`, `header`, and opaque SDK `data`. The attachment
metadata holds the key. The default provider atomically saves the 72-byte header, recovery data,
and key fingerprint in app-private storage. It retains this record after an interruption and
removes it on success or explicit cancellation.

When a preparer supplied all required encryption information, a custom upload can return
`FileTransferResult.Success(url)`; UIKit preserves its saved metadata entries. The custom provider
validates its own parameters. There is no encryption-specific upload result handling in UIKit.

Explicit upload abandonment calls `provider.cancel(operationId)` and cancels the attempt's
coroutine. The provider cleans up retained backend state, including inactive sessions after a
restart. Pause and recoverable interruptions retain that state.

A Waafi adapter using this hook returns its parameters as string values and restores them from
`request.attachment.metadata`. Its private journal stores upload IDs, part URLs, ETags, and
the encryption header and checkpoint data. It reads the key from attachment metadata.

### Download call chain

```text
AttachmentDownloadCoordinator.performDownload
  → AttachmentTransferRouter.download
  → FileDownloadRequest(type selected by encryptionEnabled, attachment)
  → FileTransferProvider.download(final plaintext destination)
       default:
         ChatSdkFileDownloader
           → PLAIN: MediaDownload.download
           → ENCRYPTED: read required enc, then MediaDownload.downloadEncrypted
           → native HTTP, authentication, decryption, and file writing
       custom:
         follow request.type, interpret the app object, and execute its download/decryption
  ← final usable plaintext path
  → UIKit saves the attachment's local path and downloaded state
```

The router selects the request type from configuration without parsing keys or choosing a decryption
algorithm. The default native downloader reads UIKit's `sceyt.enc` attachment key. Applications can interpret
different data in their own provider.

`encryptionEnabled` controls both directions: true selects `ENCRYPTED`, false selects `PLAIN`.
Encrypted requests with missing or invalid encryption metadata fail before downloading, without a
plain fallback. Older plain attachments therefore cannot be downloaded while encryption is enabled.
Existing complete local files continue to be reused.

Native downloads write verified plaintext into `destination.part` and save completed ranges in
`destination.part.json`, then publish `destination`. They do not keep a complete ciphertext copy.
The coordinator reuses complete local files; the adapter removes incomplete final files left by
older implementations before starting native download.

The SDK owns network retries, decryption, partial files, and resume checkpoints. Coroutine
cancellation pauses native writers before releasing the handle and keeps resumable data.
Explicit cancellation removes the saved native operation by ID, including after app restart.
Playback still waits for the completed local file; streaming-player integration is separate work.

The device regression `nativeRedirectDoesNotForwardChatCredentialsToAnotherOrigin` currently
exposes a native SDK issue: chat headers are forwarded across an origin-changing redirect.
This needs a separate SDK fix; the pre-upload change does not alter redirect handling.

## Provider contract

Operation IDs are stable, opaque identifiers generated by the UI kit:

- Upload: `upload:<messageTid>`
- Download: `download:<messageTid>`
- Video preview upload: `upload:<messageTid>:preview`
- Video preview download: `download:<messageTid>:preview`

A provider may map an operation ID to a different backend identifier, but must use the operation
ID for UI-kit pause and resume lookup.

UIKit supports one media/file attachment per message; an optional link preview and a video's
`preview` attachment are not tracked file transfers. Tasks and downloads use `messageTid` as their identity, so attachment ID and URL
updates do not change the running operation. Repeated requests for one message share one task.
Completion removes only that task, leaving other messages and newer attempts registered.

`upload()` and `download()` are suspend functions. Each call returns a `FileTransferResult` when it
succeeds, fails, or is interrupted; coroutine cancellation remains separate:

- Report progress through `FileTransferCallback.onProgress` as a value from `0f` to `100f`.
- Return `FileTransferResult.Success(url)` for uploads. Supply metadata through `prepareUpload()`.
  Downloads return
  `FileTransferResult.Success(destinationPath)`.
- Return `FileTransferResult.Failure(error)` for a terminal failure. UIKit deletes the partial
  download, preserving the existing failure policy.
- Return `FileTransferResult.Interrupted(error)` when the attempt ends recoverably, for example
  after network loss. Stop reading/writing before returning, but retain resumable backend state.
  UIKit preserves the partial download, reports through its existing error callback and releases
  the upload queue turn. It does not cancel the provider or schedule an automatic download retry.
  Pending uploads in `ErrorUpload` remain eligible for the existing resend-on-reconnect flow.
  A later attempt uses the same operation ID and destination.
- Cancel the matching backend work when the coroutine is cancelled, and propagate
  `CancellationException` instead of returning it as a failed result.

The default native downloader retries recoverable connection, response-read, and HTTP failures.
When native reports `WAITING_FOR_NETWORK`, the adapter returns `Interrupted`, stops native writers,
and retains verified ranges. The existing `ErrorDownload` state shows the download icon; tapping it
starts another attempt using the saved ranges. Terminal failures return `Failure` and discard native partial files.
Custom providers can return `Interrupted` as described above. The SDK upload bridge reports its
SDK errors as `Failure`.

Custom providers must return `FileTransferResult<String>` for both uploads and downloads before
adopting this unreleased API. Custom multipart providers retain backend checkpoints on recoverable interruptions.
Custom storage must not silently fall back to the default Sceyt backend.

`pause(operationId)` returns `true` only when that exact physical operation was paused and remains
available to `resume(operationId)`. `resume(operationId)` returns `true` only when the same
suspended provider call continues. Returning `false` tells the UI kit to cancel and restart the
operation using its normal ordering rules.

The coordinator only consults native pause/resume after provider work starts. Pausing preparation
cancels its coroutine, even if the provider would optimistically return `true` for an unknown ID.

## Unreleased migration notes

This branch intentionally changes public APIs from `dev`. Recompile applications and libraries
that call the changed APIs; previously compiled JVM callers are not binary-compatible.

### Transfer service and task ownership

| Previous API or behavior | Migration |
| --- | --- |
| `FileTransferListeners` and `FileTransferService.setCustomListener()` | Removed. Configure storage through `SceytChatUIKit.fileTransfer.provider` instead of replacing attachment coordination. |
| `upload`, `uploadSharedFile`, and `download` accept a caller-created `TransferTask` | Use `upload` or `download` with `configureTask: TransferTask.() -> Unit`. For shared uploads, call `upload` with `shared = true`. Each method returns the task registered by the service. |
| `addTransferTask(task)` | Removed. Start the transfer through the service, which owns registration. |
| `removeTransferTask(messageTid: Long)` | Pass the exact `TransferTask` instead; removal must not remove a newer attempt or another attachment. |
| `findOrCreateTransferTask(attachment)` only creates a task when absent | It now also registers the task. Use `findTransferTask(attachment)` for lookup without registration. |
| Three-argument `resume` | An optional `channelId` parameter is added. Supply the message's channel ID when resuming uploads so channel cleanup can cancel the replacement worker. |
| No explicit service cancellation methods | `cancelUpload(attachment)` and `cancelAllTransfers()` are now part of the service contract. Custom service implementations must implement them. |

Calling `task.uploadResultCallback` directly only publishes state and persistence updates. It no
longer removes the task or completes the worker's listeners. Customer providers must return
`FileTransferResult.Success`, `Failure`, or `Interrupted`; the service then owns task completion
and listener notification. Attach consumer completion listeners through `configureTask` instead
of creating or registering replacement tasks.

### Provider and utility APIs

The file-only provider is new relative to `dev`. For integrations with earlier unreleased
versions of this branch:

- `FileTransferTransport` is renamed to `FileTransferProvider`, and `DefaultFileTransferTransport`
  to `DefaultFileTransferProvider`. Configure it through `SceytChatUIKit.fileTransfer.provider`
  instead of `SceytChatUIKit.fileTransfer.transport`.
- `FileTransferProvider.upload` and `download` return `FileTransferResult<String>`.
  Upload success contains the remote reference; download success contains the plaintext path.
  `FileUploadResult` and its `encryptionInfo` field are removed.
- `FileTransferCallback` now exposes `onProgress(Float)`; `FileTransferEvent` is removed.
- Return `FileTransferResult.Interrupted(error)` instead of emitting the former `WaitingForNetwork` event.

Other API changes from `dev`:

- `FileTransferLogic` is removed. Configure custom storage through `SceytChatUIKit.fileTransfer.provider`.
- `FileTransferService.redownload(attachment)` remains available to reset a finished attachment
  with a missing local file and start its download again. This method already exists on `dev`.
- `PauseOrResumeTransferUseCase` takes only `Context` and `FileTransferService`; the
  `AttachmentInteractor` constructor parameter is removed. Upload resume always goes through the service.
- `FileTransferUtility.uploadFile` is now a suspend function accepting a source `File`.
  `downloadFile` accepts `FileDownloadRequest`, and cancellation uses `cancelDownload(Call)`.
  The attachment-based callback overloads and upload/download pause/resume helpers are removed.
- `OkHttpDownloader` uses request-based downloads and cancels the returned `Call`, not a message ID.
  Its `pauseDownload`, `resumeDownload`, `cancelAllDownloads`, `isDownloading`,
  `getActiveDownloadsCount`, and `getActiveDownloads` methods are removed. Use
  `FileTransferService` for attachment-level transfer control.
- Custom storage adapters must preserve backend state when returning `Interrupted` and use
  the file-transfer provider contract instead of the old service hooks.

Encryption additions to this branch:

- `FileUploadRequest` is one data class. Both requests use `FileTransferType.PLAIN` or `.ENCRYPTED`; both upload modes receive an explicit prepared plaintext `sourceFile`. Direct requests default to `PLAIN`.
- `FileTransferProvider.download` receives the final plaintext destination and owns decryption.
- Both requests contain a required `attachment`; `FileTransferContext` and `AttachmentTransferContext` are removed.
- Separate upload/download types are replaced by `FileTransferType`. The executing provider follows
  the requested mode and interprets saved attachment/app metadata for encryption parameters.
- Override `FileTransferProvider.prepareUpload` to return `Map<String, String>`. UIKit merges the entries into attachment metadata and saves it before upload. Providers read their keys from `request.attachment.metadata` on upload, download, and retry.
- `upload` returns only the uploaded reference in `FileTransferResult<String>`; encryption data is prepared and saved beforehand.
- Implement `FileTransferProvider.cancel` to clean up retained backend uploads on explicit cancellation.
- Custom `PersistenceAttachmentLogic` implementations must implement `updateUploadedAttachment`
  and save the uploaded URL and metadata together.
- `FileTransferService.uploadPersisted`, UIKit's upload journal and `TransportCheckpointStore` are removed.
- Native downloads own networking, verification, decryption, partial files, and resume checkpoints.
  `FileDownloadRequest.headers` supplies per-download HTTP headers. SDK types stay behind UIKit's adapter.

### Media and worker APIs

| Public API | Migration |
| --- | --- |
| `VideoResizeConfig` | The constructor now uses `shortSideThreshold`, `quality`, nullable `frameRate` and `bitrate`, and `bitrateCoefficient`, replacing the old `dimensionThreshold` / `compressionQuality` parameters. |
| `transcodeVideo` | Pass `config: VideoResizeConfig` instead of `quality`. The function now returns the request's destination `File?`; the result callback still supplies the completed path. |
| `VideoTranscodeHelper.cancel` | Pass the destination `File` returned by `transcodeVideo`, not the original source path or `File(sourcePath)`. |
| `CustomVideoCompressor.start` | Now returns `Job` instead of `Unit`. Recompile previously built JVM consumers, even if they ignore the return value. |
| `UploadAndSendAttachmentWorkManager.schedule` | Adds `resumePausedUpload: Boolean = false`. Use `true` only for an explicit user resume, not an unrelated worker rerun. |
| `UploadAndSendAttachmentWorkManager.cancelWorksByTag` | Now returns `Operation`. Await its completion when cleanup must finish before deleting message data. |

For `VideoResizeConfig`, bitrate selection is `bitrate`, then `bitrateCoefficient`, then `quality`.
Set `bitrateCoefficient = null` as well as leaving `bitrate = null` to select only a quality preset;
the default coefficient otherwise takes precedence over `quality`.

### Checksum-cache migration

Checksum sampling now seeks to absolute file offsets. For files at least 3 MiB (3,145,728 bytes),
stored checksums may no longer match, so duplicate-upload detection may miss once per file and
upload it again. Existing sent-message URLs remain valid; this does not delete or corrupt files.

## Lifecycle

UI-kit transfer cancellation cancels the owning coroutines. A provider must treat coroutine
cancellation as cancellation of its physical backend operation. This is also how active transfers
are stopped during logout cleanup.

## Video previews

Every outgoing video message also carries an attachment of type `preview`: a JPEG frame of the
video (longest side 800 px). It is uploaded through the same provider (`prepareUpload`, then
`upload`), so it gets its own encryption key and URL. Recipients download it next to the video and
show it as the video's thumbnail until the video itself is downloaded.

The video links to its preview through its metadata: `pvw` holds the preview attachment's URL.
The link is added when the message is sent, and recipients use it to find the video's preview.

```json
{ "type": "video", "metadata": { "szw": 3810, "szh": 1962, "tmb": "…", "dur": 10, "pvw": "<preview url>" } }
{ "type": "preview", "url": "<preview url>", "metadata": { "sceyt.enc": "<preview key>" } }
```

The preview is stored like a link attachment: its own row with its own `url`, `filePath` and
metadata, and no transfer-state row. The video keeps the message's only transfer state, so video
progress, pause and resume are unchanged. The preview is never shown as a separate item.

| Side | Preview state |
|---|---|
| Sender | The row is written after the preview upload succeeds. A row with a URL means uploaded. |
| Recipient | `filePath` is written after the download succeeds. A missing file is downloaded again. |

The upload worker uploads the preview first, then the video:

- If the preview upload fails, the message fails and the video shows the retry state.
- If the video upload fails, the saved preview is kept.
- A retry uploads only the part that is missing.
- If no frame can be extracted from the video, the video is sent without a preview.

Forwarded messages keep the original preview attachment and do not create a new one.
