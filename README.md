# Sceyt Chat UIKit for Android
The Sceyt Chat UIKit is a comprehensive toolkit designed for chat integration.
With prebuilt and customizable UI components, it allows for quick integration of a fully-featured
chat into your Android application with minimal coding.

![Sceyt Chat UIKit](https://us-ohio-api.sceyt.com/user/api/v1/files/8lwox2ge93/bc039a600a2717188892c9c2e35438b981be7e3ca36f6ece23c5db8f169fff4de828ee9ba29267e57252f07d6d48/android.webp)


## Table of Contents
* [Features](#features)
* [Requirements](#requirements)
* [Installation](#installation)
* [Usage](#usage)
* [Customization](#customization)
* [Examples](#customization)
* [Proguard](#proguard)
* [License](#license)

## Features
- **Offline Support:** Automatically stores messages and new chats when offline, and synchronizes them upon reconnection.
- **Photo & Video resizer:** On-device Photo and Video resizer for faster delivery with adjustable quality parameters.
- **Voice Messages:** Built-in support for voice message recording and play back.
- **Light, Dark Mode:**  Supports both themes, adapting to user preferences for a consistent experience.

## Requirements
Minimal Android SDK version:
-  API 19 ("KitKat"; Android 4.4) or later.

## Installation
1. Add the following line to the `build.gradle` file for your project:

```groovy
allprojects {
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```
This will enable your project to use libraries from Maven Central.

2. Add the following dependency to your app's build.gradle file:

```groovy
dependencies {
    implementation 'com.sceyt:sceyt-chat-android-uikit:1.5.7'
}
```
## Usage

Before starting the integration, it is highly recommended to explore our [example apps](https://github.com/sceyt/sceyt-chat-android-uikit/tree/dev/examples) to observe how Sceyt Chat UIKit is initialized and utilized in real applications. These examples provide valuable insights into the integration process.

1. To initialize the UI Kit, add the following code in your Application class with the following parameters:

- `clientId` - a unique identifier for your client
- `appId` - your application id
- `host` - your application API URL
- `enableDatabase` - specifies whether to enable the local database for caching data

```kotlin
import android.app.Application
import com.sceyt.sceytchatuikit.SceytUIKitInitializer
import java.util.UUID

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        SceytUIKitInitializer(this).initialize(
            clientId = UUID.randomUUID().toString(),
            appId = "8lwox2ge93",
            host = "https://us-ohio-api.sceyt.com",
            enableDatabase = true)
    }
}
```
> **Note:** If you're utilizing the [Koin](https://insert-koin.io/) dependency injection, ensure you initialize Sceyt after the Koin initialization. Incorrect sequencing can lead to unexpected behavior or runtime errors.

Make sure that your application class defined in your AndroidManifest.xml:

```xml
<application
    ...
    android:name=".MyApplication"
    ...
    >
</application>
```
2. After initializing the Sceyt Chat UIKit and setting up the configuration, the next step is to establish a connection to Sceyt Chat API.

```kotlin
fun connectToChatClient(){
    val token = "Your token"
    SceytKitClient.connect(token)
}
```

## Customization

### Basic
Customizing the appearance of the Sceyt Chat UIKit is easy and allows you to tailor it to your application's design. You can customize fonts, colors, icons, and any component, including channel and message cells, message input text box, and many more.

These following customizations can be applied during the Sceyt Chat UIKit initialization.

Here's how you can customize various aspects:

```kotlin
// Set the primary accent color for the SceytKit UI elements to enhance visual appeal.
SceytKitConfig.sceytColorAccent = R.color.colorAccent

// Set avatar colors in SceytKit to assign a color array for default user avatars and channel icons.
SceytKitConfig.avatarColors = arrayOf("#FFC107", "#FF9800", "#FF5722", "#795548")

// Set incoming and outgoing message bubble colors in SceytKit.
MessagesStyle.incBubbleColor = R.color.colorAccent
MessagesStyle.outBubbleColor = R.color.colorAccent
```

To get more about customization, you check our [Sceyt Demo application](https://github.com/sceyt/sceyt-chat-android-uikit/tree/dev/examples/SceytDemoApp).

## Proguard

If you are using Proguard with this library, make sure to add the following rules to your proguard-rules file:

``` groovy
# Keep all necessary classes in 'com.sceyt.chat' package and its subpackages

-keep class com.sceyt.sceytchatuikit.** { *; }
```

These rules will ensure that all classes in the specified packages and their sub-packages are not obfuscated by Proguard.


## License

[MIT License](LICENSE).
