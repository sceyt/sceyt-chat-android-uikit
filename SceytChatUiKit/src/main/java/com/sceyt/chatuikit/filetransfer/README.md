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
  │    ├─ UploadPreparer
  │    │    checksum reuse, image resizing and video transcoding
  │    └─ FileTransferTransport.upload
  ├─ AttachmentDownloadCoordinator
  │    file checks, destination, duplicate suppression and download attempts
  │    └─ FileTransferTransport.download
  └─ AttachmentThumbCoordinator
       local thumbnail extraction and caching
```

The service calls the three attachment coordinators directly. There is no router, generic file
coordinator, subscription, or observer bridge. Each coordinator owns its operations; only uploads
have a FIFO. Downloads run independently.

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
| `filetransfer` | `FileUploadRequest`, `FileDownloadRequest`, `FileTransferContext`: file-only transport input; `AttachmentTransferContext`: optional attachment information |
| `filetransfer/defaults` | `DefaultFileTransferTransport`, `DefaultFileTransferDestinationProvider`: built-in backend and attachment destination policy |
| `persistence/file_transfer` | `FileTransferServiceImpl`, `TransferTask`: task registration, upload/download completion and worker listeners; `FileTransferHelper`: state and persistence publication |
| `persistence/logicimpl` | `AttachmentUploadCoordinator`, `AttachmentDownloadCoordinator`, `AttachmentThumbCoordinator`: attachment upload, download and local-thumbnail handling |
| `persistence/logicimpl` | `UploadPreparer`: checksum reuse and media preparation, without task registration or transfer scheduling |
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
4. Creates a `FileUploadRequest` containing the prepared source file and optional context.
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
unreleased API. Waafi's S3 transport migration is still pending; it must not fall back to the
default Sceyt transport. Its network-wait state should return `Interrupted` while preserving S3
state, rather than emitting an event and waiting for coroutine cancellation.

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
- Waafi must migrate its transport before adopting this branch and preserve S3 state when returning `Interrupted`.
  Its `FileTransferServiceInitializer`, `WaafiFileTransferService`, and
  `ProvideVideoPostMediaDataUseCase` still use the old hooks or task-registration behavior and
  must be migrated before release. Do not silently fall back to the default Sceyt backend.

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
