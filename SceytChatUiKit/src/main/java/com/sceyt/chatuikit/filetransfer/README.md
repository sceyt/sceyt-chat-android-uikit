# File transfer architecture

The UI kit owns attachment-transfer decisions. A customer transport only uploads or downloads
the prepared file described by a request.

## Responsibilities

| UI kit | Customer transport |
| --- | --- |
| Local-file and checksum checks | Upload bytes to customer storage |
| Upload ordering and duplicate suppression | Download bytes from customer storage |
| Image resizing and video transcoding | Report normalized progress |
| Shared-upload coalescing | Pause or resume a native transfer when supported |
| Transfer state, persistence, callbacks, and cleanup | Cancel backend work when its coroutine is cancelled |
| Download destination selection | Return the remote reference or local destination path |

The transport must not create its own upload queue or implement shared-upload policy.

## Ownership

```text
FileTransferServiceImpl / TransferTask
  ├─ AttachmentUploadCoordinator
  │    upload FIFO, shared participants, pause/resume and attempt lifetime
  │    └─ AttachmentTransferRouter.upload
  │         ├─ UploadPreparer: checksum reuse, resizing and transcoding
  │         ├─ Chat SDK FileEncryptionProvider.prepareUpload / restoreUpload
  │         └─ FileTransferTransport.upload
  ├─ AttachmentDownloadCoordinator
  │    file checks, destination, duplicate suppression and download attempts
  │    └─ AttachmentTransferRouter.download
  │         ├─ Plain: FileTransferTransport.download
  │         └─ Encrypted: download temporary ciphertext, then decrypt locally
  │              └─ SDK PlaintextReader → verified local file
  └─ AttachmentThumbCoordinator
       local thumbnail extraction and caching
```

The service calls the three attachment coordinators directly. Upload and download coordinators
delegate preparation and backend selection to `AttachmentTransferRouter`. Each coordinator owns
its operations; only uploads have a FIFO. Downloads run independently.

One coroutine processes the upload coordinator's private command channel. It serializes short
state changes, including requests made from callbacks, without custom draining code or locks.
Preparation and byte transfer run in separate attempt jobs on the feature-owned IO scope.

Download attempts are registered before their jobs start. Short state changes use the coordinator's
lock; callbacks run outside it. A replacement waits for the previous job's cleanup before using the
same destination. Native pause/resume keeps the existing transport call when supported; otherwise
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
| `filetransfer` | `SceytChatUIKitFileTransfer`: customer transport and destination configuration |
| `filetransfer` | `FileUploadRequest.Plain`, `FileUploadRequest.Encrypted`, `FileDownloadRequest`: transport inputs; `AttachmentTransferContext`: optional attachment information; `TransportCheckpointStore`: opaque backend resume state |
| `filetransfer/defaults` | `DefaultFileTransferTransport`, `DefaultFileTransferDestinationProvider`: built-in backend and attachment destination policy |
| `persistence/file_transfer` | `FileTransferServiceImpl`, `TransferTask`: task registration, upload/download completion and worker listeners; `FileTransferHelper`: state and persistence publication |
| `persistence/logicimpl` | `AttachmentUploadCoordinator`, `AttachmentDownloadCoordinator`, `AttachmentThumbCoordinator`: attachment upload, download and local-thumbnail handling |
| `persistence/logicimpl` | `UploadPreparer`: checksum reuse and media preparation, without task registration or transfer scheduling |
| `persistence/logicimpl` | `AttachmentTransferRouter`: selects plain/encrypted execution and preserves that selection for the attempt |
| `filetransfer/encryption` | Key envelopes, upload-session storage and writing SDK-verified plaintext to a local file |
| `persistence/logicimpl` | `FileTransferUtility`, `OkHttpDownloader`: SDK upload and HTTP download bridges |

## Configuration

Configure file transfer before starting chat transfers:

```kotlin
SceytChatUIKit.fileTransfer.transport = CustomerFileTransferTransport()
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

Do not replace the transport while operations are active.

## Upload behavior

For a normal upload, the UI kit:

1. Adds the attachment to the sequential upload queue.
2. Reuses a completed upload when checksum reuse is enabled and a valid reference exists.
3. Resizes an image or transcodes a video when required.
4. Creates a `FileUploadRequest.Plain` containing the prepared source file and optional context.
5. Calls the transport and owns progress, persistence, completion, failure, and queue advancement.

Preparation runs inside the admitted upload's coroutine, so a queued upload does not prepare early.
`UploadPreparer` receives an attachment and preparation callbacks, then returns the prepared
attachment or a reused URL. It does not own tasks, shared groups, or transport operations.
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

Requests contain an optional `FileTransferContext`. UIKit supplies an `AttachmentTransferContext`
with an attachment snapshot and informational `isSharedUpload` flag. The default backend never
inspects it. Customer transports may read it, but must use the request's
source, destination, URL and operation ID for the actual transfer.

## Download behavior

The destination provider selects the local file before the transport is called. It must return the
same destination for the same attachment, and different paths for different attachments. Partial
downloads survive pause and returned interruptions. Ordinary terminal failures delete the partial
destination, preserving the existing policy.

The UI kit skips the transport when a complete local file already exists. Otherwise it creates a
`FileDownloadRequest`, suppresses duplicate in-flight downloads, and owns transfer state and
result delivery.

Downloads for different messages remain concurrent. There is no download FIFO and no automatic
download retry.

## Encrypted files and the Chat SDK

This worktree uses the locally published Chat SDK (`sceyt = "local"`). The SDK owns file keys,
the encryption format, its 64 KiB records and authentication. UIKit consumes the SDK's public
`FileEncryptionProvider`, `PreparedUpload`, `CiphertextRangeSource` and `PlaintextReader` contracts.
UIKit does not implement the file cipher or calculate encrypted record boundaries.

Enable encryption before starting transfers:

```kotlin
SceytChatUIKit.fileTransfer.apply {
    encryptUploads = true
    // Set transport here when using customer storage instead of the Chat SDK uploader.
}
```

The temporary wrapping-key/provider layer has been removed. UIKit stores the SDK's unique ID
and raw file key directly in metadata. A recipient needs no separate app wrapping key.
Anyone who can read that metadata can obtain the file key; recipient-specific key protection is
not implemented in this prototype.

### Upload call chain

```text
AttachmentUploadCoordinator.performUpload
  → AttachmentTransferRouter.upload
  → UploadPreparer.prepare
  → FileEncryptionProvider.prepareUpload(finalFile, uniqueId)
  → FileEncryptionMetadata.fromEncryptionInfo(PreparedUpload.encryptionInfo)
  → FileUploadRequest.Encrypted(ciphertextSource, checkpointStore)
  → FileTransferTransport.upload
      default: ChatClient.uploadEncrypted(PreparedUpload, ...)
      custom:  ciphertextSource.readCiphertextRange(offset, length) → backend part upload
```

The SDK encrypts only the requested ciphertext ranges, without creating a complete encrypted
temporary file. A transport can request a multipart-sized range; SDK records need not align with
transport parts. Offsets and lengths always address the encrypted object, whose size is
`ciphertextSource.ciphertextSize`, including encryption overhead.

UIKit writes `enc` alongside the existing attachment metadata:

```json
{
  "tmb": "ChgWCYZ4eHiIiAiHiIiAhwg=",
  "szw": 1280,
  "szh": 203,
  "enc": {
    "id": "4b61fd06-bdf7-43e2-bb3e-3a54f20a8a00",
    "v": 1,
    "k": "base64url-file-key"
  }
}
```

`id` comes from `FileEncryptionInfo.data.uniqueId`; `k` is `FileEncryptionInfo.data.key`.
Version 1 identifies `tink-aes256-gcm-hkdf-64k+commit-v1`. The attachment's existing `fileSize`
carries the plaintext size; UIKit updates it from the SDK after preparation, including shared
uploads. On download, `toEncryptionInfo(attachment.fileSize)` reconstructs the SDK's typed
`FileEncryptionData` from these values. No serialized encryption-info JSON is stored in `k`.
The file key is carried in this metadata, rather than appended to the uploaded bytes.
UIKit merges `enc` into each attachment's metadata, preserving dimensions, thumbnails and customer
fields. The upload URL and matching metadata are saved in one database transaction. Encrypted
upload URLs are excluded from the plain-file checksum reuse cache.

### Multipart resume

`UploadSessionStore` separates storage from the SDK range reader. The demo implementation,
`FileUploadSessionStore`, uses atomic JSON files under the app's private `filesDir/upload-sessions`.
The session holds SDK encryption info (including the file key), the SDK checkpoint and an opaque
transport checkpoint. A future Room implementation can replace this store; prototype data need
not be migrated. Older wrapped-key prototype records are not migrated.

Malformed session JSON is discarded without affecting other uploads. The next attempt starts a
fresh SDK encryption session and backend upload; disk read/write errors still surface as failures.

A custom multipart transport must:

1. Read `request.checkpointStore.get()` and restore its backend state when present.
2. Save its newly created backend upload ID before reading/uploading the first part.
3. Read exact ciphertext ranges. UIKit saves the SDK checkpoint before each range is returned.
4. Save completed part numbers and ETags after backend success, awaiting each durable save.
5. Complete the backend upload, persisting the completed remote reference for recovery before
   returning `Success`. On retry after completion, return that saved reference.
6. Stop all source reads before returning or propagating cancellation. UIKit owns source closure.

On a later attempt the router restores the same SDK session when a transport checkpoint exists.
The SDK validates the original file and reproduces the same encrypted bytes. The transport owns
presigned URLs, URL renewal, part sizing, ETags and backend completion/abort behavior.

The default Chat SDK uploader is a single upload request. Its internal reads bypass UIKit's range
wrapper, and it has no multipart backend checkpoint. A cancelled/failed default upload therefore
starts a fresh encryption session on retry. UIKit does not claim durable multipart resume for it.
Successful sessions are released after `FileTransferHelper` confirms persistence through
`FileTransferService.uploadPersisted`.

Explicit cancellation calls `transport.discardUpload(operationId, checkpointData)` before deleting
the local session. This lets a backend abort an interrupted multipart upload after a restart.
The hook is idempotent and schedules cleanup without blocking the caller. Pause and retry do not
discard the session.

### Download call chain

```text
AttachmentDownloadCoordinator.performDownload
  → AttachmentTransferRouter.download
  → metadata.enc.toEncryptionInfo(attachment.fileSize)
  → FileTransferTransport.download(<destination>.sceyt.enc.part)
  → rename completed ciphertext to <destination>.sceyt.enc
  → FileEncryptionProvider.openDownload(FileCiphertextRangeSource, encryptionInfo)
  → PlaintextReader.read(plaintextOffset, length)
      → read encrypted ranges from the local file
      → SDK decrypts and verifies
  → write <destination>.sceyt.part, then rename to destination
  → delete the encrypted temporary file
```

This full-file path works when the server returns the whole object with HTTP 200. The network
stage uses the existing downloader. It does not require remote HTTP Range support. The SDK still
uses range reads, but those reads access the completed local ciphertext file.

During decryption the encrypted file and growing plaintext file coexist, using roughly twice the
attachment size temporarily. Only verified plaintext is published. Success removes the encrypted
copy. Network interruptions retain the encrypted partial for retry; pausing decryption retains
the completed encrypted file and verified plaintext position. Local read/write failures,
authentication failure and explicit cancellation remove temporary files after reads and writes stop.

Progress uses 0–90% for network download and 90–100% for SDK verification. Native network pause is
used when available; decryption pause uses cancellation and restart. Playback still waits for the
full verified file. Transports provide `download` to write encrypted bytes; the SDK's local file
range reader handles decryption after the download completes.

## Transport contract

Operation IDs are stable, opaque identifiers generated by the UI kit:

- Upload: `upload:<messageTid>`
- Download: `download:<messageTid>`

A transport may map an operation ID to a different backend identifier, but must use the operation
ID for UI-kit pause and resume lookup.

UIKit supports one media/file attachment per message; an optional link preview is not another
file transfer. Tasks and downloads use `messageTid` as their identity, so attachment ID and URL
updates do not change the running operation. Repeated requests for one message share one task.
Completion removes only that task, leaving other messages and newer attempts registered.

`upload()` and `download()` are suspend functions. Each call returns a `FileTransferResult` when it
succeeds, fails, or is interrupted; coroutine cancellation remains separate:

- Report progress through `FileTransferCallback.onProgress` as a value from `0f` to `100f`.
- Return `FileTransferResult.Success` with a non-blank remote reference from upload, or the
  destination path from download.
- Return `FileTransferResult.Failure(error)` for a terminal failure. UIKit deletes the partial
  download, preserving the existing failure policy.
- Return `FileTransferResult.Interrupted(error)` when the attempt ends recoverably, for example
  after network loss. Stop reading/writing before returning, but retain resumable backend state.
  UIKit preserves the partial download, reports through its existing error callback and releases
  the upload queue turn. It does not cancel the transport or schedule an automatic download retry.
  Pending uploads in `ErrorUpload` remain eligible for the existing resend-on-reconnect flow.
  A later attempt uses the same operation ID and destination.
- Cancel the matching backend work when the coroutine is cancelled, and propagate
  `CancellationException` instead of returning it as a failed result.

The default HTTP downloader returns `Interrupted` for connection and response-read failures,
and HTTP 408, 429, 500, 502, 503, and 504. These responses preserve partial downloads for a later
retry; they do not trigger an automatic retry. Other HTTP errors and destination-write errors
return `Failure`. The SDK upload bridge reports its SDK errors as `Failure`.

Custom transports must implement these `FileTransferResult<String>` return types before adopting this
unreleased API. Waafi's S3 transport returns `Interrupted` for network waits while preserving
its multipart checkpoint. Custom storage must not silently fall back to the default Sceyt backend.

`pause(operationId)` returns `true` only when that exact physical operation was paused and remains
available to `resume(operationId)`. `resume(operationId)` returns `true` only when the same
suspended transport call continues. Returning `false` tells the UI kit to cancel and restart the
operation using its normal ordering rules.

The coordinator only consults native pause/resume after transport work starts. Pausing preparation
cancels its coroutine, even if the transport would optimistically return `true` for an unknown ID.

## Unreleased migration notes

This branch intentionally changes public APIs from `dev`. Recompile applications and libraries
that call the changed APIs; previously compiled JVM callers are not binary-compatible.

### Transfer service and task ownership

| Previous API or behavior | Migration |
| --- | --- |
| `FileTransferListeners` and `FileTransferService.setCustomListener()` | Removed. Configure storage through `SceytChatUIKit.fileTransfer.transport` instead of replacing attachment coordination. |
| `upload`, `uploadSharedFile`, and `download` accept a caller-created `TransferTask` | Pass `configureTask: TransferTask.() -> Unit` instead. Each method returns the task registered by the service. |
| `addTransferTask(task)` | Removed. Start the transfer through the service, which owns registration. |
| `removeTransferTask(messageTid: Long)` | Pass the exact `TransferTask` instead; removal must not remove a newer attempt or another attachment. |
| `findOrCreateTransferTask(attachment)` only creates a task when absent | It now also registers the task. Use `findTransferTask(attachment)` for lookup without registration. |
| Three-argument `resume` | An optional `channelId` parameter is added. Supply the message's channel ID when resuming uploads so channel cleanup can cancel the replacement worker. |
| No explicit service cancellation methods | `cancelUpload(attachment)` and `cancelAllTransfers()` are now part of the service contract. Custom service implementations must implement them. |

Calling `task.uploadResultCallback` directly only publishes state and persistence updates. It no
longer removes the task or completes the worker's listeners. Customer transports must return
`FileTransferResult.Success`, `Failure`, or `Interrupted`; the service then owns task completion
and listener notification. Attach consumer completion listeners through `configureTask` instead
of creating or registering replacement tasks.

### Transport and utility APIs

The file-only transport is new relative to `dev`. For integrations with earlier unreleased
versions of this branch:

- `FileTransferTransport.upload/download` return `FileTransferResult<String>` instead of `Result<String>`.
- `FileTransferCallback` now exposes `onProgress(Float)`; `FileTransferEvent` is removed.
- Return `FileTransferResult.Interrupted(error)` instead of emitting the former `WaitingForNetwork` event.

Other API changes from `dev`:

- `FileTransferLogic` is removed. Configure custom storage through `SceytChatUIKit.fileTransfer.transport`.
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
  the file-transfer transport contract instead of the old service hooks. Waafi's current adapter
  follows this contract; its multipart walkthrough documents the upload and download call chains.

Encryption additions to this branch:

- Handle both sealed `FileUploadRequest` cases; file-based integrations now use `.Plain.sourceFile`.
- `FileTransferTransport.download` receives the encrypted temporary destination for encrypted files.
- Implement `FileTransferTransport.discardUpload` to clean up retained backend uploads on cancellation.
- Custom `PersistenceAttachmentLogic` implementations must implement `updateUploadedAttachment`
  and save the uploaded URL and metadata together.
- `FileTransferService.uploadPersisted` defaults to a no-op; services using UIKit's encrypted upload
  sessions should delegate it to the upload coordinator after the persistence save succeeds.

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

UI-kit transfer cancellation cancels the owning coroutines. A transport must treat coroutine
cancellation as cancellation of its physical backend operation. This is also how active transfers
are stopped during logout cleanup.

## Future optional uploads

A future video-poster workflow can use the file-only transport requests. It must own its optional
retry and metadata application, and tie its lifetime to the physical video upload rather than its
first participant. Poster failure must not fail or delay the video. Poster upload is not implemented
here; current tests cover independent concurrent uploads without blocking the normal upload FIFO.
