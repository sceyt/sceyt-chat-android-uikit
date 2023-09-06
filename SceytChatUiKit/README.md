## Android Chat Messaging Tutorial

**Creating a Project**

- Select the Empty Views Activity template
- Select your language - Kotlin (recommended) or Java
- Set the Minimum SDK to 21 (or higher)

![](https://us-ohio-api.sceyt.com/user/api/v1/files/8lwox2ge93/ceb344bc4d2cb7107545f22db66396af491890e0af0ced69321d150e927d27dec5e784296212b66b3e2f4c291877/Screenshot%202023-09-06%20at%2015.01.58.png )


Our SDKs are available from MavenCentral, with some of our dependencies being hosted on Jitpack.
Update your repositories in the settings.gradle file like so:

```plaintext
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url "https://jitpack.io" }
    }
}
```

First, we'll enable [View Binding](https://developer.android.com/topic/libraries/view-binding) and
[Data Binding](https://developer.android.com/topic/libraries/data-binding). Next, we're going to add
the [SceytChat SDK](https://github.com/sceyt/sceyt-chat-android-uikit) to our project dependencies.
Open up the app module's build.gradle script and make the following changes:


**Initialize Sceyt**

```groovy
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'com.sceytsimple'
    compileSdk 33

    defaultConfig {
        applicationId "com.sceytsimple"
        minSdk 21
        targetSdk 33
        versionCode 1
        versionName "1.0"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
                targetCompatibility JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = '1.8'
    }

    // Enable ViewBinding
    buildFeatures {
        viewBinding true
    }

    // Enable DataBinding
    dataBinding{
        enabled = true
    }
}

dependencies {
    //Sceyt Chat
    implementation 'com.sceyt:sceyt-chat-android-uikit:1.1.0'

    implementation 'androidx.core:core-ktx:1.10.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.9.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
}
```

To initialize Sceyt, add following code in your Application class.


Note if you are using dependency injection [Koin](https://insert-koin.io/), please initialize Sceyt after initializing Koin.

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
We are initializing Sceyt with the following parameters:
1. clientId - a unique identifier for your client
2. appId - your application id
3. host - the host of your Sceyt backend
4. enableDatabase - whether to enable the local database for caching data


Make sure that your application class defined in your AndroidManifest.xml like:

```xml
<application
    android:allowBackup="true"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="@xml/backup_rules"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:supportsRtl="true"
    
    android:name=".MyApplication"
    
    android:theme="@style/Theme.SceytSimple"
    tools:targetApi="31">
    <activity
        android:name=".MainActivity"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />

            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
</application>
```

**Displaying a List of Channels**

Sceyt provides a low-level client, and convenient UI components to help you quickly build your messaging interface. In this section, we'll be using the UI components to quickly display a channel list.

First, open up activity_main.xml, and change the contents of the file to the following to display a full screen ChannelsListView:

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
Next, open up MainActivity and replace the file's contents with the following code:

```kotlin
class MainActivity : AppCompatActivity() {
    private val channelsViewModel: ChannelsViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(ActivityMainBinding.inflate(layoutInflater).also {
            binding = it
        }.root)

        // Step 1 - Connect Sceyt chat client
        SceytKitClient.connect("token", "testUser1")

        // Step 2 - Connect the ChannelsViewModel to the ChannelsListView
        channelsViewModel.bind(binding.channelsListView, this)
        binding.channelsListView.setChannelClickListener(ChannelClickListeners.ChannelClickListener {
            // TODO - start conversaion activity
        })
    }
}
```

Let's have a quick look at the source code shown above:

1. We created connect method which connects getting token using getTokenByUserName method and then connecting to Sceyt using SceytKitClient.connect method.
2. We bind our ChannelsListView to the ChannelsViewModel by calling the bind function.


## Creating a Chat Experience

Next, let's create Conversation page.

Create a new Empty Views Activity _(New -> Activity -> Empty Views Activity)_ and name it `ConversationActivity`.
   > **_Note:_** Make sure that `ConversationActivity` is added to your manifest.
   > 
Open up activity_conversation.xml and change the layout to the following:
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
Next, replace the code in `ConversationActivity` with this code:
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

Lastly, we want to launch `ConversationActivity` when you tap a channel in the channel list. 
Open `MainActivity` and replace the TODO in the `setChannelClickListener` with the following code:

```kotlin
binding.channelsListView.setChannelClickListener(ChannelClickListeners.ChannelClickListener {
    ConversationActivity.newInstance(this, it.channel)
})
```
The `ConversationActivity` will be launched when you tap a channel in the channel list, and
the interface will look like this:

<img height="420" src="https://us-ohio-api.sceyt.com/user/api/v1/files/8lwox2ge93/24ea7c09bad476edce5e1a1e3ea8115c53dfc53215b4971d456e3c61778c10c0c6528d8f1d7e5fc925e9dcee46a2/Screenshot_20230906_190932.png" width="200"/>
