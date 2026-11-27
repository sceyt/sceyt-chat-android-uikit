Generate a Git commit message based on the provided code changes.

Use this format:

<type>(<scope>): <short summary>

- <change 1>
- <change 2>
- <change 3>

SDK mapping:
- `Connection`, `ChatConnection`, or artifact `sceyt-chat-android-connection` → `ChatConnection`
- `UiKit`, `ChatUiKit`, or artifact `sceyt-chat-android-uikit` → `ChatUiKit`

Scope detection rules:
- For a demo app version update, use `app(SceytChatDemoApp)` when the changes are limited to `Config.versionCode` and/or `Config.versionName`, which set the demo app version in `examples/SceytDemoApp`.
- Determine the SDK scope from the actual changed code, not only from the file path or containing module.
- For version changes in shared configuration files, inspect the specific object/block whose `version` value changed.
- If the changed version is inside `object UiKit`, use `ChatUiKit`.
- If the changed version is inside `object Connection` or the corresponding connection SDK config block, use `ChatConnection`.
- Prefer `artifactId` and the enclosing config object over the filename when determining the SDK.
- `sceyt-chat-android-uikit` always maps to `ChatUiKit`.
- The UI Kit version must never be classified as `ChatConnection` just because the configuration file is shared by both SDKs.

Examples:

Demo app version update (`Config.versionCode` and `Config.versionName` changed):

```text
app(SceytChatDemoApp): Demo app version

- update version code and name in configuration
- App Version: `1.3.5`
```

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
- Use `app` for demo app version updates. For other changes, use Conventional Commits such as `feat`, `fix`, `refactor`, `test`, `chore`, `perf`, or `docs`.
- Use exactly these SDK scopes:
  - `ChatConnection`
  - `ChatUiKit`
- SDK scopes omit the `Sceyt` prefix.
- If both SDKs are changed, use:
  - `feat(ChatConnection, ChatUiKit): ...`
- Omit the scope for changes outside either SDK unless the demo app version rule applies.

Version update rules:
- Do not include the version number in the summary.
- For demo app version updates, use the summary `Demo app version`. List the changed version fields in the first bullet and end with `App Version:` followed by the resulting `Config.versionName` in backticks.
- If only an SDK version changes, use `chore`.
- Mention the changed SDK/config block in the body where useful.
- Add the final version as the last bullet only when the commit changes that SDK's version:
  - `- Version: \`<version>\``
- Omit the version bullet when the commit does not change a version. Mentioning a version in documentation or a dependency example does not count as changing the SDK version.

Do not include explanations before or after the commit message.
Return only the commit message.
