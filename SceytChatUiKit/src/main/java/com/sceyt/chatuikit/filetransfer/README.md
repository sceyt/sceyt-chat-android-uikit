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
  └─ FileTransferLogicImpl (routing and feature-owned scope)
     └─ AttachmentUploadCoordinator / AttachmentDownloadCoordinator
          metadata, checksum, preparation, destination, UI and persistence
          └─ FileUploadCoordinator / FileDownloadCoordinator
               ordering, participants, pause/resume/cancel and attempt lifetime
               └─ FileTransferTransport
                    storage transport only
```

Each task holds its upload or download subscription. The subscription references the physical
operation directly. Each file coordinator has one operation registry; only uploads also have a FIFO.
The attachment adapters have no separate job maps, paused sets or shared-membership collections.

Short control transitions are serialized, while preparation and transport run in the feature-owned
IO scope. Subscriptions are bound before work starts. Completed operations leave admission tracking
before callbacks run, and events from replaced attempts are ignored. Attachment adapters remove
the completed task and notify its completion listeners; `FileTransferHelper` only publishes state
and persistence updates.

### Class locations

Paths below are relative to `com/sceyt/chatuikit`:

| Package | Classes and purpose |
| --- | --- |
| `filetransfer` | `SceytChatUIKitFileTransfer`: customer transport and destination configuration |
| `filetransfer` | `FileUploadRequest`, `FileDownloadRequest`, `FileTransferContext`: file-only transport input; `AttachmentTransferContext`: optional attachment information |
| `filetransfer` | `FileUploadCoordinator`, `FileDownloadCoordinator`: physical operations and their participants |
| `filetransfer` | `FileTransferSubscription`, `FileTransferObserver`: one participant's controls and notifications |
| `filetransfer` | `FileTransferControl`: serializes short coordinator state changes |
| `filetransfer/defaults` | `DefaultFileTransferTransport`, `DefaultFileTransferDestinationProvider`: built-in backend and attachment destination policy |
| `persistence/file_transfer` | `FileTransferServiceImpl`, `TransferTask`: task registration, callbacks and bound subscriptions; `FileTransferHelper`: state and persistence publication |
| `persistence/logicimpl` | `FileTransferLogicImpl`: entry point; attachment upload/download adapters and `AttachmentThumbCoordinator`: attachment-specific decisions |
| `persistence/logicimpl` | `FileTransferUtility`, `OkHttpDownloader`: SDK upload and HTTP download bridges |

`FileTransferControl` is internal to each file coordinator, not a global transfer manager. It queues
state changes so concurrent or reentrant callbacks cannot modify an operation halfway through another
change. Callbacks run outside its lock, and coroutine starts wait until queued state changes finish.
Preparation and byte transfer do not run inside those state changes.

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
Ordinary uploads remain sequential and never implicitly join a shared group. Explicit shared uploads
run concurrently with that queue.

For shared attachments with the same original source, the UI kit starts one physical upload and distributes
its progress and result to the logical transfer tasks. Pausing one task does not pause the physical
upload while another task remains active. The physical upload is paused only after every task in
the shared group is paused. Resuming any task resumes the existing physical operation when
possible, otherwise the UI kit restarts it.

Deleting one participant leaves the others running; deleting the last cancels the physical operation.
A successful result is retained by paused participants for their later resume. A finished or failed
group does not prevent a later share from starting. Sequential resumes wait for their queue turn.

Requests contain an optional `FileTransferContext`. UIKit supplies an `AttachmentTransferContext`
with an attachment snapshot and informational `isSharedUpload` flag. The file coordinators and
default backend never inspect it. Customer transports may read it, but must use the request's
source, destination, URL and operation ID for the actual transfer.

## Download behavior

The destination provider selects the local file before the transport is called. It must return the
same destination for the same attachment, and different paths for different attachments. Partial
downloads survive pause and reported network waits. Ordinary terminal errors delete the partial
destination, preserving the existing policy.

The UI kit skips the transport when a complete local file already exists. Otherwise it creates a
`FileDownloadRequest`, suppresses duplicate in-flight downloads, and owns transfer state and
result delivery.

Downloads remain concurrent, including different attachments in the same message. There is no
download FIFO and no automatic download retry.

## Transport contract

Operation IDs are stable, opaque identifiers generated by the UI kit:

- Upload: `upload:<messageTid>`
- Download: `download:<messageTid>:id:<attachmentId>` when an attachment ID is assigned;
  otherwise `download:<messageTid>:url:<url>`.

A transport may map an operation ID to a different backend identifier, but must use the operation
ID for UI-kit pause and resume lookup.

Downloads include the attachment identity so different attachments in one message can transfer
independently. An assigned ID keeps pause and resume stable if the URL changes. When the ID is
null or zero, the opaque URL identifies the attachment and must stay unchanged during an active
transfer. Repeated requests for the same attachment share one task. Completion removes only that
task, leaving other attachments and newer attempts registered.

`upload()` and `download()` are suspend functions. Each call must remain suspended until it
succeeds, fails, or is cancelled:

- Report progress through `FileTransferCallback` as a value from `0f` to `100f`.
- Report a network-blocked operation with `FileTransferEvent.WaitingForNetwork`. The UI kit treats
  this event as terminal for the current attempt, cancels the transport call, reports an error and
  releases its upload queue turn. A later retry starts a new attempt; this event does not schedule
  an automatic download retry.
- Return a non-blank remote reference from upload.
- Return the destination path from download.
- Throw on terminal failure.
- Cancel the matching backend work when the coroutine is cancelled.

`pause(operationId)` returns `true` only when that exact physical operation was paused and remains
available to `resume(operationId)`. `resume(operationId)` returns `true` only when the same
suspended transport call continues. Returning `false` tells the UI kit to cancel and restart the
operation using its normal ordering rules.

The coordinator only consults native pause/resume after transport work starts. Pausing preparation
cancels its coroutine, even if the transport would optimistically return `true` for an unknown ID.

## Lifecycle

UI-kit transfer cancellation cancels the owning coroutines. A transport must treat coroutine
cancellation as cancellation of its physical backend operation. This is also how active transfers
are stopped during logout cleanup.

## Future optional uploads

A video poster can use an ordinary concurrent file upload alongside the main upload. The media
workflow will own its optional retry and metadata application, and tie its lifetime to the physical
video operation rather than its first participant. The file engine needs no thumbnail role or flag.
Poster failure must not fail or delay the video. This workflow is not implemented in this refactor;
a file-only test verifies that a failing concurrent upload does not block the main upload or FIFO.
