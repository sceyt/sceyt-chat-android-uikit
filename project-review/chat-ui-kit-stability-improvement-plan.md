# Chat UI Kit Stability Improvement Plan

## Purpose

This plan turns the Chat UI Kit audit findings into ordered implementation work. It starts with small, test-backed fixes and then moves to the components that require refactoring.

Reported issues provide regression scenarios. The longer-term work still targets component responsibility, testability, and state consistency rather than only the known bugs.

## Working rules

### Existing logic before refactoring

- Add characterization tests for existing behavior wherever the current design allows useful tests.
- Cover successful behavior, failure paths, and state transitions that must remain unchanged.
- Do not delay a necessary refactor only to force tests through code that has no usable test seam. In that case, first introduce the smallest seam that allows behavior to be controlled, then add the test.

### Reported issues

For each issue:

1. Reproduce the issue with a focused automated test.
2. Identify the responsibility that causes the failure.
3. If the fix is local and does not require refactoring, make the smallest fix immediately.
4. If the fix requires a wider responsibility change, keep the regression scenario with that refactoring work.
5. Run the focused test and the related component suite before completing the change.

The test and fix should be delivered together so the main branch remains green.

### Refactoring

- Move one responsibility at a time.
- Keep behavior unchanged unless a regression test defines the intended correction.
- Give each state one owner. Views render state and forward actions; they do not reconcile SDK, database, and network results.
- Preserve the existing public API unless a separate API change is reviewed.
- Use line coverage as supporting information only. Completion depends on covered behavior, transitions, races, and failure paths.

## Implementation order

| Step | Work | Result |
| --- | --- | --- |
| 0 | Establish the baseline | Existing tests pass and active branches are reviewed |
| 1 | Fix duplicate channels in channel search | Search results contain one item per stable identity |
| 2 | Fix channels disappearing from the channel list | Reusable `ChannelsCache` state cannot remove or replace channels from another list scope |
| 3 | Refactor file transfer | Upload and download behavior follows a tested state machine |
| 4 | Refactor the messages list | Message rows and header state have testable owners |
| 5 | Plan and implement local unread calculation | The team agrees on the correct solution before implementation starts |
| 6 | Improve channel info as it changes | New changes are testable and protected from regression |

Small fixes discovered during any step can be completed immediately when a focused regression test and a local fix are sufficient. Changes that require a responsibility redesign move into the relevant refactoring step.

Replacing channel synchronization is deferred. It requires a proper synchronization mechanism and ordering contract with the server. Until that mechanism is available, this plan does not attempt to replace the current sync flow or add a temporary client-only workaround.

Channel-member pagination reconciliation is also deferred. It requires the server and client to use the same stable sort key, direction, and deterministic tie-breaker. Until that contract is available, this plan does not replace the current member-loading workaround.

## Step 0 — Establish the baseline

### Actions

- Select the implementation base commit and run the existing Chat UI Kit tests.
- Record the current input, event, and expected output for each component being changed.
- Review relevant existing work against the selected base before reimplementing it.
- Reuse only changes that still match the intended behavior and have focused tests. Do not adopt a complete change set without reviewing its diff and tests.

### Exit checks

- The selected base passes its current targeted tests.
- Relevant existing branch changes have a keep, rewrite, or discard decision.
- Pre-existing test failures are recorded separately from failures introduced by this work.

## Step 1 — Fix duplicate channels in channel search

This is the first fast fix because the search ViewModel already has focused unit-test support.

### Actions

1. Add a failing test for the reported duplicate scenario.
2. Also cover overlapping pages and the same channel returned more than once by a result source.
3. Use channel ID as the stable identity when merging channel results.
4. Keep pagination offsets based on the number of source data items consumed, not rendered headers or the size after deduplication.
5. Reject results belonging to an older search session after the query changes.
6. Make the smallest production change that passes these cases.

### Exit checks

- A channel ID appears at most once in the rendered search state.
- Repeated or overlapping pages do not add duplicates.
- Deduplication does not skip the next source page.
- A stale query result cannot update the active query.
- `ChannelsSearchViewModelTest` and the related global-search tests pass.

## Step 2 — Fix channels disappearing from the channel list

The issue is already reproduced. Its root cause is the reusable `ChannelsCache`. Use the existing reproduction as the acceptance test and fix cache ownership and reuse instead of adding a list-level workaround.

### Actions

1. Keep the existing disappearing-channel reproduction as the primary regression test.
2. Define the complete immutable key that identifies one cached channel-list scope, including every configuration value that changes list membership or ordering.
3. Ensure data loaded for one scope cannot clear, replace, or be returned as the data for another scope.
4. When a channel list is reopened or its configuration changes, reuse cache data only when the complete scope key matches. Otherwise initialize the list from its own database/query result.
5. Apply cache additions, updates, deletions, and window replacements only to scopes for which the channel and operation are valid.
6. Keep cache mutation and reads atomic so a list cannot observe a partially cleared or rebuilt window.
7. Add focused `ChannelsCacheTest` cases for two simultaneous list configurations, reopening a list, clearing one scope, and replacing one loaded window.
8. Run the existing channel-list pagination and sync-reload tests to verify that the cache fix preserves their behavior.

### Exit checks

- Reusing `ChannelsCache` cannot make a channel disappear from an active list.
- Clearing or replacing one cached scope does not modify another scope.
- Reopening a list with the same scope returns its valid cached window.
- Opening a list with a different scope cannot receive an incompatible cached window.
- Cache events update only the lists for which the event is valid.
- The existing disappearing-channel reproduction passes without a ViewModel workaround.
- The related channel-list tests pass together.

## Step 3 — Refactor file transfer

Before moving code, add tests for behavior that can already be controlled through the current transfer interfaces. Fix isolated bugs first when their correction does not require the refactor.

### Characterization tests

- Queue order and advancement after success or failure.
- Upload preparation and progress delivery.
- Download progress and completion.
- Pause, resume, cancel, and retry.
- Duplicate requests for the same operation.
- Shared-file uploads and callback delivery.
- Thumbnail generation and recovery behavior.

### Refactoring sequence

1. Define the allowed upload and download state transitions.
2. Give a transfer coordinator sole ownership of queue order and the active operation.
3. Keep resize, transcode, and thumbnail preparation behind preparation interfaces.
4. Keep SDK/network calls behind a transfer interface.
5. Persist transfer changes through one state store.
6. Make terminal handling idempotent so success, failure, or cancellation advances the queue once.
7. On failure, clear the active operation, preserve retry data, and allow the next queued transfer to start.
8. Review existing file-transfer work and reuse only the boundaries that satisfy this state model.

### Required regression tests

- Upload fails before the transport task is registered and can be retried once.
- One failed upload does not block the queue.
- Pause/resume works during preparation and transport.
- Duplicate requests do not create duplicate operations.
- Transfer progress and completion remain correct when the visible message row is recycled.
- `PendingDownload → Downloading → Downloaded` shows progress and then the final local media.

### Exit checks

- State-machine and queue tests use controlled dispatchers and deterministic fakes.
- Each terminal path releases the active operation exactly once.
- Messages, search results, and media screens consume the same persisted transfer state.
- The old all-in-one responsibilities are removed only after their replacements are tested.

## Step 4 — Refactor the messages list

Add tests around the current logic before extracting responsibilities. Existing working behavior must remain covered throughout the refactor.

### Characterization tests

- Initial load, next/previous paging, and live message insertion.
- Message edit, deletion, reaction, selection, and reply updates.
- Transfer progress and final attachment rendering.
- Scroll commands and unread indicators.
- Header updates from channel, connection, and presence events.
- View-holder recycling for text, media, progress, and selection state.

### Refactoring sequence

1. Define immutable render state for message rows and the list header.
2. Move list merging and update decisions into a dedicated state owner.
3. Keep `MessagesListView` responsible for rendering and forwarding UI actions.
4. Make adapter items use stable identity.
5. Make view holders render only the supplied state and clear state from previously bound items.
6. Move header title and subtitle selection into a testable resolver.
7. Keep transfer-state mapping shared with the file-transfer implementation.
8. Extract one responsibility at a time while preserving the public API.

### Required regression tests

- Paging and live insertion produce one row per message identity.
- Updates affect the intended message row.
- A recycled media row cannot show another message’s image or progress.
- Presence and connection updates cannot leave an old last-seen value.
- Download failure remains retryable and a later retry renders completion.
- Activity recreation renders the latest state again.

### Exit checks

- State owners are covered with unit tests.
- Row and header rendering are covered with Robolectric tests.
- Espresso covers transfer progress, recycling, and header updates in the real component.
- `MessagesListView` no longer reconciles SDK, persistence, and transfer results.

## Deferred — Replace channel synchronization

Do not implement this work until the server provides the synchronization mechanism and ordering data required to reconcile snapshots with live events correctly.

When that dependency is available, define the server/client contract for snapshot versions, live-event ordering, deletion, reconnect, repeated sync, local pending state, and unread reconciliation before changing the current flow. Existing synchronization-related regression scenarios should remain recorded for that future work.

## Step 5 — Plan and implement local unread calculation

The native marker deadlock is already fixed and remains outside this work. Chat UI Kit still needs a local unread mechanism, but the correct solution has not been defined yet.

### Actions

- Hold a technical planning session with the relevant team members before choosing an implementation.
- Review the current unread behavior, available SDK/server data, marker behavior, persistence model, and known failure scenarios.
- Agree on the expected behavior for online, offline, reconnect, duplicate delivery, message deletion, mark as read, manual mark as unread, and partial local history.
- Identify which parts depend on the deferred server synchronization mechanism.
- Document the selected solution, ownership boundaries, data flow, persistence requirements, migration needs, and test scenarios.
- Review the proposed solution with the team and approve the implementation plan before changing production code.
- Implement the approved solution in small, test-backed changes.

### Exit checks

- The team has agreed on the unread-count behavior and source of truth.
- Server, SDK, UI Kit, persistence, and synchronization responsibilities are documented.
- Dependencies and currently blocked behavior are explicit.
- The approved plan contains verifiable scenarios for normal behavior, failure paths, and event ordering.
- Implementation and tests match the approved design.

## Deferred — Fix channel-member pagination

Do not replace the current member-loading workaround until the server and client use the same stable sort key, direction, and deterministic tie-breaker.

After that server contract is available:

- update the Room query to use the same order;
- compare or replace only the matching ordered page window;
- remove the first-page delete-and-reload workaround;
- test page boundaries, equal primary sort values, additions/removals between pages, reopening a large channel, and failed page requests.

## Step 6 — Improve channel info as it changes

Channel info remains lower priority. Do not perform a broad refactor now.

For each future change:

1. Identify the affected state and side effects.
2. Add a regression or characterization test.
3. Move logic out of the Fragment or view only when its location prevents deterministic testing.
4. Add an Espresso test when the change crosses navigation, permissions, persistence, or another component boundary.
5. Run the related member, media, and channel-mutation tests.

## Testing approach

Add test infrastructure only when an implementation step needs it:

| Behavior | Primary test |
| --- | --- |
| Reducers, state transitions, merging, stale-result rejection | JVM unit test |
| Android view binding and row/header rendering | Robolectric test |
| Transactions, paging queries, and restart persistence | Room instrumented test |
| Customer-visible interaction across real components | Espresso feature test |

Use deterministic fakes for SDK events, transfer progress, connection state, and server pages. UI tests must synchronize on observable state or an idling resource and must not use fixed sleeps. Add a test host Activity and Koin overrides when the first component-level Espresso test is implemented. Add a Gradle Managed Device to CI after that first feature test is stable.

## Definition of done

The plan is complete when:

- the two fast channel-list fixes are protected by regression tests;
- transfer failure, retry, pause, resume, and completion follow one tested state machine;
- message rows and headers render from testable state owners;
- the team-approved local unread solution is implemented and verified according to its agreed plan;
- critical UI flows pass without real network calls or fixed sleeps;
- obsolete workarounds and duplicate state owners are removed after their replacements are verified.

Server-backed synchronization replacement and channel-member pagination reconciliation are not part of the current definition of done. They will be planned after their required server mechanisms are available.
