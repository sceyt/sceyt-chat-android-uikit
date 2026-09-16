# Sceyt Chat Connection

`SceytChatConnectionManager` manages Chat SDK connection, token renewal, and application
foreground/background behavior.

Create one application-scoped manager and reuse it for the lifetime of the process. Initialize
the Sceyt Chat SDK before calling `connect`.

## Token provider

Use `ChatTokenProvider` when the application already has its own authenticated API client:

```kotlin
val connectionManager = SceytChatConnectionManager(
    context = applicationContext,
    tokenProvider = ChatTokenProvider { userId ->
        backend.getChatToken(userId)
    }
)

connectionManager.connect(userId)
```

Use `connectAndAwait` when an operation must wait until the SDK is connected, such as login or
background notification handling:

```kotlin
val result = connectionManager.connectAndAwait(userId, timeoutMillis = 10_000L)
```

When using Sceyt Chat UIKit, provide this behavior once during application setup:

```kotlin
SceytChatUIKit.chatConnectionProvider = ChatConnectionProvider { timeoutMillis ->
    connectionManager.connectAndAwait(currentUserId, timeoutMillis)
}
```

`ChatConnectionProvider` is used by UI Kit background workers and notification actions. Its
`Result` must succeed only after the Chat SDK is connected.

For a simple HTTP endpoint, use `HttpChatTokenProvider`:

```kotlin
val connectionManager = SceytChatConnectionManager(
    context = applicationContext,
    tokenProvider = HttpChatTokenProvider(
        endpoint = "https://example.com/chat/token"
    )
)
```

The HTTP provider sends `GET https://example.com/chat/token?user=<userId>` and expects:

```json
{
  "token": "<jwt>"
}
```

Use `userIdQueryParameter` to change the `user` query parameter name. Use a custom
`ChatTokenProvider` if the endpoint requires another HTTP method or response format. Optional
headers can be supplied through the `headers` parameter.

## Token reuse and recovery

JWTs with a valid `exp` claim are stored in application-private preferences and reused until
they are close to expiration. The default expiry leeway is 30 seconds and can be changed with
`ChatConnectionConfig.tokenExpirationLeewaySeconds`. Tokens without an `exp` claim are accepted
but are not persisted.

The preference file is named `sceyt_chat_connection.xml`. If the host application enables Android
backup, exclude this shared-preferences file from its backup rules so bearer tokens are not copied
to cloud backup or device transfer.

For Android 12 and newer, add the exclusion to `res/xml/data_extraction_rules.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="sharedpref" path="sceyt_chat_connection.xml" />
    </cloud-backup>
    <device-transfer>
        <exclude domain="sharedpref" path="sceyt_chat_connection.xml" />
    </device-transfer>
</data-extraction-rules>
```

For Android 11 and older, add the exclusion to `res/xml/backup_rules.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <exclude domain="sharedpref" path="sceyt_chat_connection.xml" />
</full-backup-content>
```

Reference both files from the host application's manifest:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules" />
</manifest>
```

If the application already has backup-rule files, add only these `exclude` entries to the existing
rules.

Token-expiration callbacks always request a fresh token. A connection failure with SDK error code
`1021` also clears the saved token and retries once. Override `tokenRefreshErrorCodes` when another
SDK version or backend uses different token-related error codes.

## Background behavior

The default policy keeps the Chat SDK connected. To disconnect after the application moves to the
background:

```kotlin
val config = ChatConnectionConfig(
    backgroundConnectionPolicy = BackgroundConnectionPolicy.Disconnect(
        delayMillis = 30_000L
    )
)
```

An explicit `connect(userId)` still works while the application is in the background, for example
when processing a push notification or incoming call.

## Logout

The connection provider is intentionally responsible only for connecting. The application should
clear the connection manager when UI Kit logout completes:

```kotlin
SceytChatUIKit.logOut { result ->
    connectionManager.logout()
    // Continue application logout handling with result.
}
```
