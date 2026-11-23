# Chat Android UI Kit architecture

| Gradle module | Responsibility |
| --- | --- |
| `:SceytChatUiKit` | Chat screens, view models, SDK-backed repositories, Room storage, notifications, and customization APIs. |
| `:SceytChatConnection` | Connection lifecycle and token handling through `SceytChatConnectionManager` and its chat client abstraction. |
| `:SceytDemoApp` | Example app under `examples/SceytDemoApp`. |
| `:benchmark` | Android performance and startup measurements. |

`SceytChatUIKit.initialize` initializes the Chat SDK and registers the UI Kit's Koin modules. Screens use view models and interactors to load data. Repositories mediate access to the underlying Chat SDK, and persistence manages local state. The host app supplies identity and credentials. It can customize themes, providers, renderers, and navigation through the UI Kit's public API.

See the [file transfer architecture](docs/file-transfer-architecture.md) for attachment handling and the [integration guide](README.md) for host app setup.
