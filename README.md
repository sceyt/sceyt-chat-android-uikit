# SceytChat Android SDK

The SceytChat Android SDK provides developers with a powerful and flexible chat interface that can be easily integrated into Android apps. With SceytChat, you can easily add one-to-one and group chat functionality to your app. It comes packed with a wide range of features, including message threading, media and file sharing, reactions, user mentions, message search, user and channel blocking, message forwarding and replies, and many more.

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
    }
}
```
This will enable your project to use libraries from Maven Central.

2. Add the following dependency to your app's build.gradle file:

```python
dependencies {
    implementation 'com.sceyt:sceyt-chat-android-sdk:1.0.0'
}
```

3. Sync your project.

4. Add the following permissions to your app's AndroidManifest.xml file:

```php
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
```

## Usage

1. To use the SceytChat Android SDK, you will need to initialize it with your Sceyt application credentials. The initialization requires the following parameters:

    `apiUrl:` The base URL of the Sceyt chat API.

    `appId:` The ID of your Sceyt chat application.

    `context:` The Android context of your application.

```kotlin
    class MyApplication : Application() {
    
        override fun onCreate() {
            super.onCreate()
    
            // Initialize the SceytChat SDK with your credentials
            val apiUrl = "https://us-ohio-api.sceyt.com" // replace with your Sceyt application API URL
            val appId = "8lwox2ge93" // replace with your Sceyt application ID
    
            var chatClient = ChatClient.setup(apiUrl, appId, applicationContext)
        }
    }
```
2. After calling the setup method on the ChatClient object, you can register a ClientListener to be notified when certain events occur,
   such as when the connection to Sceyt is successful, 
   where "listenerName" is used to identify the listener.
   For example:

```kotlin
    chatClient.addClientListener("listenerName", object : ClientListener {
    
        override fun onChangedConnectStatus(connectState: Types.ConnectState, status: Status) {
            // Called when the connection status changes.
        }
    
        override fun onTokenWillExpire(expireTime: Long) {
            // Called when the token is about to expire.
        }
    
        override fun onTokenExpired() {
            // Called when the token has expired.
        }
    })
```
3. You can then use the connect method of the ChatClient instance to connect to the Sceyt chat server, where the token is a JWT token
   signed with your application's private key. Sceyt verifies the token by the public key of the application.

```kotlin
  chatClient.connect(token)
```

4. After connecting to Sceyt, you can create, for example,
   a direct channel with a user using the CreateDirectChannelRequest builder and a User object with the user's ID.
   Specify a ChannelCallback to handle the results. Here's an example:

```kotlin    
    val userId = "someUserId"
    
    CreateDirectChannelRequest(User(userId)).execute(object : ChannelCallback {
    
        override fun onResult(channel: Channel) {
            // The channel has been created successfully.
        }
    
        override fun onError(e: SceytException) {
            // An error occurred while creating the channel.
        }
    })
```

5. Now you can send a message to the Created channel using its ID, like this:

```kotlin
    ChannelOperator.build(channel.id).sendMessage(message, object : MessageCallback {
        override fun onResult(message: Message) {
            // The message has been sent successfully.
        }
        override fun onError(e: SceytException?) {
            // An error occurred while sending the message.
        }
    })
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

See the [LICENSE](LICENSE) file for details.
