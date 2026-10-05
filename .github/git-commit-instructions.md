Generate a Git commit message based on the provided code changes.

Use this format:

<type>(<sdk>): <short summary>

- <change 1>
- <change 2>
- <change 3>
- Version: `<version>`

SDK mapping:
- `Connection`, `ChatConnection`, or artifact `sceyt-chat-android-connection` → `ChatConnection`
- `UiKit`, `ChatUiKit`, or artifact `sceyt-chat-android-uikit` → `ChatUiKit`

Scope detection rules:
- Determine the SDK scope from the actual changed code, not only from the file path or containing module.
- For version changes in shared configuration files, inspect the specific object/block whose `version` value changed.
- If the changed version is inside `object UiKit`, use `ChatUiKit`.
- If the changed version is inside `object Connection` or the corresponding connection SDK config block, use `ChatConnection`.
- Prefer `artifactId` and the enclosing config object over the filename when determining the SDK.
- `sceyt-chat-android-uikit` always maps to `ChatUiKit`.
- The UI Kit version must never be classified as `ChatConnection` just because the configuration file is shared by both SDKs.

Examples:

Changed code:

```kotlin
object UiKit {
    const val artifactId = "sceyt-chat-android-uikit"
    const val version = "2.1.52302508-SNAPSHOT"
}
```

Correct commit:

chore(ChatUiKit): bump SDK version

- update UiKit version in configuration
- Version: `2.1.52302508-SNAPSHOT`

Incorrect:

chore(ChatConnection): bump SDK version

- update SDK version in configuration
- Version: `2.1.52302508-SNAPSHOT`

General rules:
- Use Conventional Commits such as `feat`, `fix`, `refactor`, `test`, `chore`, `perf`, or `docs`.
- Use exactly these SDK scopes:
  - `ChatConnection`
  - `ChatUiKit`
- Do not use the `Sceyt` prefix.
- If both SDKs are changed, use:
  - `feat(ChatConnection, ChatUiKit): ...`
- If the change is not specific to either SDK, omit the scope.

Version update rules:
- Do not include the version number in the summary.
- If only a version changes, use `chore`.
- Mention the changed SDK/config block in the body where useful.
- Add the final version as the last bullet:
  - `- Version: \`<version>\``

Do not include explanations before or after the commit message.
Return only the commit message.