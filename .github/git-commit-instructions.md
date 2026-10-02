Generate a Git commit message based on the provided code changes.

Use this format:

<type>(<sdk>): <short summary>

- <change 1>
- <change 2>
- <change 3>
- Version: `<version>`

SDK mapping:
- SceytChatConnection SDK → `ChatConnection`
- SceytChatUiKit SDK → `ChatUiKit`

Rules:
- Use Conventional Commits for the first line, such as `feat`, `fix`, `refactor`, `test`, `chore`, `perf`, or `docs`.
- When the change belongs to one of the SDKs, always include the mapped SDK name as the commit scope:
    - `feat(ChatConnection): ...`
    - `fix(ChatUiKit): ...`
- Determine the SDK scope from the files/modules changed.
- Use exactly these scope names:
    - `ChatConnection`
    - `ChatUiKit`
- Do not use the `Sceyt` prefix in commit scopes.
- If multiple SDKs are affected, include both:
    - `feat(ChatConnection, ChatUiKit): ...`
- If the change does not belong specifically to either SDK, omit the scope:
    - `chore: ...`

Commit summary rules:
- Keep the first line concise.
- Describe the main purpose or behavioral change.
- Do not include version numbers in the summary.
- Do not end the summary with a period.
- Use lowercase after the type/scope unless capitalization is required for a proper name or API.

Commit body rules:
- Add a blank line after the summary.
- Add bullet points for important changes.
- Each bullet must start with `- `.
- Use concise present-tense or imperative wording.
- Describe meaningful behavior or implementation changes.
- Mention tests when tests were added or updated.
- Avoid vague bullets such as `update code`, `fix issues`, or `make improvements`.

Version update rules:
- When an SDK version is changed in `Config.kt`, Gradle configuration, version catalog, or another version configuration file, do not include the version number in the summary.
- Mention the version update in the body.
- Add the final version as a separate last bullet:

`- Version: \`<version>\``

- Always use the new/final version after the change.
- If only the SDK version changed, use `chore`.
- If a feature or bug fix also includes a version bump, use `feat` or `fix` and mention the version only in the body.

Example — ChatUiKit feature:

feat(ChatUiKit): keep unread state synchronized after channel updates

- observe unread changes from channel events
- refresh affected channel items when unread state changes
- prevent stale unread counters after reopening the channel
- update tests for unread-state synchronization

Example — ChatConnection fix:

fix(ChatConnection): prevent duplicate message sends after reconnect

- avoid resending messages that are already being processed
- keep pending message state synchronized during reconnection
- clear stale sending state after failures
- update reconnect and pending-message tests

Example — ChatUiKit version bump:

chore(ChatUiKit): bump SDK version

- update SDK version in configuration
- Version: `2.1.523006-SNAPSHOT`

Example — change affecting both SDKs:

fix(ChatConnection, ChatUiKit): synchronize reconnect message state

- expose updated pending message state after reconnect
- consume connection state changes in the UI Kit
- prevent stale pending messages from remaining visible
- update integration tests for reconnect behavior

Do not include explanations before or after the commit message.

Return only the commit message.