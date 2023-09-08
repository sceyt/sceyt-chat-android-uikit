# Sceyt Chat SimpleApp

A simple Android app to demonstrate the Sceyt Chat Android UI Kit in its most basic and intuitive form. This repository serves as a practical starting point for those looking to integrate or understand the capabilities of the UI components in a chat application.

 ## Table of contents

* [Creating a new Android Project](#creating-a-new-android-project)
* [Project Setup and Configuration](#project-setup-and-configuration)
* [Initialize Sceyt Chat Android UI Kit](#initialize-sceyt-chat-android-ui-kit)
* [Creating a Chat Experience](#creating-a-chat-experience)
* [Customization](#customization)

* [License](#license)

## Creating a new Android Project

**Prerequisites**

- Android Studio installed on your system.
- An active internet connection for downloading dependencies.

1. Open Android Studio and create a new project.
2. For the purpose of this demo, select the "Empty Views Activity" template.
3. Name your project and package.
4. Select your preferred language - Kotlin (recommended) or Java.
5. Set the Minimum SDK to 21 (or higher).

![](https://us-ohio-api.sceyt.com/user/api/v1/files/8lwox2ge93/a77357ab56193de54fec5ceda269255c61c89231f5854c352e0192441d12e4f3f1960ec2e280b9ac1d4c86ea9dd0/Screenshot%202023-09-08%20at%2012.37.12.png)

## Project Setup and Configuration

**Adding MavenCentral and JitPack Repositories**

1. Open the settings.gradle.
2. In the **allprojects** section, update the repositories block with **MavenCentral** and **JitPack**:

```scss
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url "https://jitpack.io" }
        // Other repositories if any
    }
}
```

**Enabling Data and View Binding**

1. Open the build.gradle (Module: app).
2. Inside the **'android'** block, add the sections for both **'dataBinding'** and **'viewBinding'**:

```groovy
android {
    ...
    buildFeatures {
        viewBinding true
        dataBinding true
    }
}
```

**Adding Sceyt Chat Android UI Kit**

Next, add the [Sceyt Chat Android UI Kit](https://github.com/sceyt/sceyt-chat-android-uikit) in the app module's build.gradle file and sync:

```groovy
dependencies {
    ...
    implementation 'com.sceyt:sceyt-chat-android-uikit:1.5.1'
}
```
## Initialize Sceyt Chat Android UI Kit

To initialize the UI Kit, add the following code in your Application class with the following parameters:

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

**Displaying Channel List**

Sceyt provides a low-level client, and convenient UI components to help you quickly build your messaging interface. In this section, we'll be using the UI components to quickly display a channel list.

First, open the `activity_main.xml`, and change the contents of the file to the following to display a full screen `ChannelsListView`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center"
    android:orientation="vertical"
    tools:context=".MainActivity">

    <com.sceyt.sceytchatuikit.presentation.uicomponents.channels.ChannelsListView
        android:id="@+id/channelsListView"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```
Next, open the `MainActivity` and replace the file's contents with the following code:

```kotlin
class MainActivity : AppCompatActivity() {
    private val channelsViewModel: ChannelsViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(ActivityMainBinding.inflate(layoutInflater).also {
            binding = it
        }.root)

        // Step 1 - Connect the Sceyt chat client with the token and user
        SceytKitClient.connect("token", "testUser1")

        // Step 2 - Connect the ChannelsViewModel to the ChannelsListView
        channelsViewModel.bind(binding.channelsListView, this)
        binding.channelsListView.setChannelClickListener(ChannelClickListeners.ChannelClickListener {
            // TODO - start conversaion activity
        })
    }
}
```

> **Note:** To generate your own token you can use the token generator following this link: https://docs.sceyt.com/chat/api/application/


## Creating a Chat Experience

Next, let's create a chat conversation screen.

Create a new Empty Views Activity _(New -> Activity -> Empty Views Activity)_ and name it `ConversationActivity`.

> **Note:** Make sure that `ConversationActivity` is added to your manifest.

Open the `activity_conversation.xml` and change the layout to the following:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/sceyt_color_bg"
    tools:context=".ConversationActivity">

    <com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.ConversationHeaderView
        android:id="@+id/headerView"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:layout_constraintTop_toTopOf="parent" />

    <com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.MessagesListView
        android:id="@+id/messagesListView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:clipToPadding="false"
        android:orientation="vertical"
        android:paddingBottom="10dp"
        app:layout_constraintBottom_toTopOf="@+id/messageInputView"
        app:layout_constraintTop_toBottomOf="@+id/headerView" />

    <com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.MessageInputView
        android:id="@+id/messageInputView"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:layout_constraintBottom_toBottomOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

After, replace the code in `ConversationActivity` with this following:

```kotlin
import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sceyt.chat.demo.databinding.ActivityConversationBinding
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.extensions.asActivity
import com.sceyt.sceytchatuikit.extensions.launchActivity
import com.sceyt.sceytchatuikit.extensions.overrideTransitions
import com.sceyt.sceytchatuikit.extensions.parcelable
import com.sceyt.sceytchatuikit.persistence.filetransfer.*
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels.MessageListViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels.bindings.bind

class ConversationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConversationBinding
    private val viewModel: MessageListViewModel by viewModels {
        MyViewModelFactory()
    }
    private lateinit var channel: SceytChannel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(ActivityConversationBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        getDataFromIntent()

        viewModel.bind(binding.messagesListView, lifecycleOwner = this)
        viewModel.bind(binding.messageInputView, null, lifecycleOwner = this)
        viewModel.bind(binding.headerView, null, lifecycleOwner = this)
    }

    private fun getDataFromIntent() {
        channel = requireNotNull(intent.parcelable(CHANNEL))
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun newInstance(context: Context, channel: SceytChannel) {
            context.launchActivity<ConversationActivity> {
                putExtra(CHANNEL, channel)
            }
            context.asActivity().overrideTransitions(R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold, true)
        }
    }

    inner class MyViewModelFactory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val channel: SceytChannel = requireNotNull(intent.parcelable(CHANNEL))
            val conversationId = channel.id

            @Suppress("UNCHECKED_CAST")
            return MessageListViewModel(conversationId, false, channel) as T
        }
    }
}
```

Lastly, launch the `ConversationActivity` when you tap a channel in the list. Open `MainActivity` and replace the TODO in the `setChannelClickListener` with the following code:

```kotlin
binding.channelsListView.setChannelClickListener(ChannelClickListeners.ChannelClickListener {
    ConversationActivity.newInstance(this, it.channel)
})
```
The `ConversationActivity` will be launched when you tap a channel in the channel list.

## Customization

There are two ways to customize the Channel list:
1. Using `XML attributes`.
2. Using `ChannelStyle`.

To customize channels list using `XML attributes` you need to add your custom attributes to `ChannelsListView` in your layout file.

For example, if you want to change the background color of the channels list, you can add the following attribute to your `ChannelsListView`:

```xml
  <com.sceyt.sceytchatuikit.presentation.uicomponents.channels.ChannelsListView
        android:id="@+id/channelsListView"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        
        app:sceytUiChannelListBackgroundColor="@color/colorAccent" />
```

To customize channels list using `ChannelStyle` you need to update properties of `ChannelStyle` object.

For example, if you want to change the background color of the channels list, you can do it like this:

```kotlin
  ChannelStyle.backgroundColor = R.color.purple_200
```
> **Note:** Make sure you need to update `ChannelStyle` before binding `ChannelsListView` to `ChannelsViewModel`.
