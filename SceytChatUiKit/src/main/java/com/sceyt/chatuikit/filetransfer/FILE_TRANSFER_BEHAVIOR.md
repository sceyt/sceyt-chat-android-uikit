# File transfer behavior

The UI kit owns transfer decisions. A customer transport only uploads or downloads the prepared
file and reports progress or failure.

## Upload

```text
request -> queue -> reuse check -> prepare -> transport -> save result -> next
```

- Normal uploads run sequentially.
- Duplicate requests for the same `messageTid` are suppressed.
- A valid local/checksum result skips the transport.
- Images are resized and videos are transcoded before transport upload.
- Failure completes the current task and advances the queue.

## Shared upload

Attachments shared from the same source use one physical upload.

- Progress and result are delivered to every active logical task.
- Pausing one task does not stop the upload while another task is active.
- The physical upload pauses only when every shared task is paused.
- Resuming one task resumes the physical upload or restarts it when native resume is unavailable.
- A paused task reuses an already completed shared result when resumed.

## Video poster

The video poster is a separate `TransferRole.Thumbnail` upload.

- It runs asynchronously beside the main video upload.
- It retries only while the main upload is active.
- Its failure never fails or delays the video upload.
- Main and poster operations use different operation IDs.
- Pause and resume also cover an active poster, but a poster that cannot pause or resume is
  cancelled on its own and the video keeps its physical operation.
- If the poster upload has already succeeded, its URL continues to be persisted after the video finishes.

## Download

```text
request -> choose destination -> reuse local file or deduplicate -> transport -> result
```

- A complete local file skips the transport.
- Only one active download is allowed for an operation ID.
- The destination is stable so a partial file can be reused after restart.
- A video poster download is optional and runs separately from the main video download.

## Pause and resume

- `pause()` or `resume()` returning `true` means the same physical operation continues later.
- Returning `false` or throwing makes the UI kit cancel and restart through its normal rules.
- Late progress is ignored while an operation is paused.
- `WaitingForNetwork` ends the current attempt and releases its queue or deduplication slot.

## Cleanup

Cancelling all transfers clears active jobs, upload queues, shared groups, paused state, download
deduplication, and registered tasks. The transport must cancel its backend work when its coroutine
is cancelled.
