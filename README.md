# SceytChat Android UIKit

Sceyt Chat UIKit is a collection of customizable UI components that allows developers to quickly and easily create beautiful and functional chat interfaces for their messaging applications. The UIKit includes a wide range of components, such as message bubbles, avatars, message input fields, and more.

With Sceyt Chat UIKit, developers can customize the look and feel of their chat interface to match the branding and design of their messaging application. The components are built to be easy to use and integrate seamlessly with the Sceyt Chat SDK for Android.

In addition to the UI components, the Sceyt Chat UIKit also includes pre-built functionality such as typing indicators, read receipts, message reactions and many more. This helps to streamline the development process, allowing developers to focus on building a great user experience rather than on implementing basic chat functionality.
## Table of contents

* [Requirements](#requirements)
* [Installation](#installation)
* [Usage](#usage)
* [Proguard](#proguard)
* [License](#license)

## Requirements

Before using the SceytChat Android SDK, you will need the following:

- Android SDK 21 or later
- Java version 8 or later
- Android Studio 4.1 or later

## Installation

1. Add the following line to the `build.gradle` file for your project:

```scss
allprojects {
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```
This will enable your project to use libraries from Maven Central.

2. Add the following dependency to your app's build.gradle file:

```python
dependencies {
    implementation 'com.sceyt:sceyt-chat-android-uikit:1.1.0'
}
```

3. Sync your project.

4. Add the following permissions to your app's AndroidManifest.xml file:

```php
 <uses-permission android:name="android.permission.INTERNET" />
 <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 <uses-permission android:name="android.permission.CAMERA" />
 <uses-permission android:name="android.permission.RECORD_AUDIO" />
 <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

## Usage

1. To use the SceytChat Android UIKit, you will need to initialize it with your Sceyt application credentials. The initialization requires the following parameters:

    `host:` The base URL of the Sceyt chat API.

    `appId:` The ID of your Sceyt chat application.

    `clientId:` The ID of current device.
   
    `enableDatabase:` Enables UIKit database persistence.

```kotlin
    class MyApplication : Application() {
    
        override fun onCreate() {
           super.onCreate()
           
           chatClient = SceytUIKitInitializer(this).initialize(
                   clientId = UUID.randomUUID().toString(),
                   appId = "8lwox2ge93",
                   host = "https://us-ohio-api.sceyt.com",
                   enableDatabase = true)
        }
    }
```

## Proguard

If you are using Proguard with this library, make sure to add the following rules to your proguard-rules file:

```python
# Keep all necessary classes in 'com.sceyt.chat' package and its subpackages

-keep class com.sceyt.chat.models.** { *; }
-keep class com.sceyt.chat.wrapper.** { *; }
-keep class com.sceyt.chat.callback.** { *; }
```

These rules will ensure that all classes in the specified packages and their sub-packages are not obfuscated by Proguard.


## License

[MIT License](LICENSE).
