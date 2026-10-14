package com.sceyt.chatuikit.styles

import android.app.Activity
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.logicimpl.usecases.sceytMessage
import com.sceyt.chatuikit.presentation.components.channel.messages.preview.SelfDestructingVoiceMessageActivity
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelInfoActivity
import com.sceyt.chatuikit.presentation.components.channel_info.ViewPagerAdapter
import com.sceyt.chatuikit.presentation.components.channel_info.media.ChannelInfoMediaFragment
import com.sceyt.chatuikit.presentation.components.channel_info.viewmodel.ChannelInfoViewModel
import com.sceyt.chatuikit.presentation.components.message_info.MessageInfoActivity
import com.sceyt.chatuikit.styles.channel_info.ChannelInfoStyle
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle
import com.sceyt.chatuikit.styles.preview.SelfDestructingVoiceMessageStyle
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.kotlin.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class ActivityStyleRecreationTest {

    private val controllers = mutableListOf<ActivityController<out Activity>>()
    private val registeredIds = mutableSetOf<String>()
    private lateinit var itemStyle: MessageItemStyle
    private lateinit var channelCustomizer: StyleCustomizer<ChannelInfoStyle>
    private lateinit var voiceCustomizer: StyleCustomizer<SelfDestructingVoiceMessageStyle>
    private var voiceActivityInfo: ActivityInfo? = null

    @Before
    fun setUp() {
        itemStyle = MessageItemStyle.Builder(RuntimeEnvironment.getApplication(), null).build()
        channelCustomizer = ChannelInfoStyle.styleCustomizer
        voiceCustomizer = SelfDestructingVoiceMessageStyle.styleCustomizer
    }

    @After
    fun tearDown() {
        controllers.asReversed().forEach {
            if (!it.get().isDestroyed) {
                it.get().finish()
                it.pause().stop().destroy()
            }
        }
        registeredIds.forEach { StyleRegistry.unregister(it) }
        ChannelInfoStyle.styleCustomizer = channelCustomizer
        SelfDestructingVoiceMessageStyle.styleCustomizer = voiceCustomizer
        voiceActivityInfo?.let {
            shadowOf(RuntimeEnvironment.getApplication().packageManager).addOrUpdateActivity(it)
        }
    }

    @Test
    fun `message info recreation retains the supplied message style`() {
        val controller = launchMessageInfo()

        controller.recreate()

        assertThat(StyleRegistry.getTyped<MessageItemStyle>(itemStyle.styleId))
            .isSameInstanceAs(itemStyle)
    }

    @Test
    fun `message info finish releases the supplied message style`() {
        val controller = launchMessageInfo()

        controller.get().finish()
        controller.pause().stop().destroy()

        assertThat(StyleRegistry.getTyped<MessageItemStyle>(itemStyle.styleId)).isNull()
    }

    @Test
    fun `voice preview recreation and finish preserve the message info owners style`() {
        val owner = launchMessageInfo()
        val resolvedStyles = mutableListOf<MessageItemStyle>()
        SelfDestructingVoiceMessageStyle.styleCustomizer = StyleCustomizer { _, style ->
            resolvedStyles += style.messageItemStyle
            style
        }
        val context = RuntimeEnvironment.getApplication()
        val activityInfo = context.packageManager.getActivityInfo(
            ComponentName(context, SelfDestructingVoiceMessageActivity::class.java), 0
        )
        voiceActivityInfo = ActivityInfo(activityInfo)
        activityInfo.theme = R.style.SceytAppTheme
        shadowOf(context.packageManager).addOrUpdateActivity(activityInfo)
        val attachment = SceytAttachment(
            id = 1L, messageId = 42L, messageTid = 42L, userId = null,
            name = "voice", type = "voice", metadata = null, fileSize = 0L,
            createdAt = 0L, url = null, filePath = null, transferState = null,
            progressPercent = null, originalFilePath = null, linkPreviewDetails = null,
        )
        val intent = SelfDestructingVoiceMessageActivity.createIntent(
            context, sceytMessage(), attachment, itemStyle.styleId
        )
        val preview = Robolectric.buildActivity(SelfDestructingVoiceMessageActivity::class.java, intent).setup()
        controllers += preview
        assertThat(resolvedStyles.last()).isSameInstanceAs(itemStyle)

        preview.recreate()

        assertThat(resolvedStyles.last()).isSameInstanceAs(itemStyle)
        assertThat(StyleRegistry.getTyped<MessageItemStyle>(itemStyle.styleId)).isSameInstanceAs(itemStyle)

        preview.get().finish()
        preview.pause().stop().destroy()
        assertThat(StyleRegistry.getTyped<MessageItemStyle>(itemStyle.styleId)).isSameInstanceAs(itemStyle)

        owner.get().finish()
        owner.pause().stop().destroy()
        assertThat(StyleRegistry.getTyped<MessageItemStyle>(itemStyle.styleId)).isNull()
    }

    @Test
    fun `channel info recreation preserves parent and registered tab styles`() {
        val controller = launchChannelInfo()
        val originalStyle = controller.get().currentStyle()
        val original = tabStyles(originalStyle)
        assertThat(StyleRegistry.getTyped<ChannelInfoStyle>(originalStyle.styleId))
            .isSameInstanceAs(originalStyle)

        repeat(2) {
            controller.recreate()
            val currentStyle = controller.get().currentStyle()
            val current = tabStyles(currentStyle)
            registeredIds += currentStyle.styleId
            registeredIds += current.map { it.styleId }
            assertThat(currentStyle).isSameInstanceAs(originalStyle)
            assertThat(StyleRegistry.getTyped<ChannelInfoStyle>(originalStyle.styleId))
                .isSameInstanceAs(originalStyle)
            original.zip(current).forEach { (previous, restored) ->
                assertThat(restored).isSameInstanceAs(previous)
                assertThat(StyleRegistry[previous.styleId]).isSameInstanceAs(previous)
            }
            val media = controller.get().supportFragmentManager.fragments
                .filterIsInstance<StyleMediaFragment>().single()
            assertThat(media.currentStyle()).isSameInstanceAs(original.first())
            assertThat(media.currentStyle().backgroundColor).isEqualTo(Color.MAGENTA)
        }
    }

    @Test
    fun `channel info finish after recreation releases parent and every registered tab style`() {
        val controller = launchChannelInfo()
        val originalStyle = controller.get().currentStyle()
        val original = tabStyles(originalStyle)
        controller.recreate()
        val currentStyle = controller.get().currentStyle()
        val current = tabStyles(currentStyle)
        registeredIds += currentStyle.styleId
        registeredIds += current.map { it.styleId }
        assertThat(StyleRegistry.getTyped<ChannelInfoStyle>(currentStyle.styleId))
            .isSameInstanceAs(currentStyle)

        controller.get().finish()
        controller.pause().stop().destroy()

        assertThat(StyleRegistry[originalStyle.styleId]).isNull()
        assertThat(StyleRegistry[currentStyle.styleId]).isNull()
        (original + current).forEach { assertThat(StyleRegistry[it.styleId]).isNull() }
    }

    private fun launchMessageInfo(): ActivityController<StyleMessageInfoActivity> {
        val context = RuntimeEnvironment.getApplication()
        val intent = MessageInfoActivity.createIntent(context, 42L, 7L, itemStyle)
            .setClass(context, StyleMessageInfoActivity::class.java)
        registeredIds += itemStyle.styleId
        return Robolectric.buildActivity(StyleMessageInfoActivity::class.java, intent).setup()
            .also { controllers += it }
    }

    private fun launchChannelInfo(): ActivityController<StyleChannelInfoActivity> {
        ChannelInfoStyle.styleCustomizer = StyleCustomizer { _, style ->
            style.copy(mediaStyle = style.mediaStyle.copy(backgroundColor = Color.MAGENTA))
        }
        val context = RuntimeEnvironment.getApplication()
        val intent = ChannelInfoActivity.createIntent(context, createChannel(7L, 0L, 0L).copy(type = "group"))
            .setClass(context, StyleChannelInfoActivity::class.java)
        return Robolectric.buildActivity(StyleChannelInfoActivity::class.java, intent).setup().also {
            controllers += it
            registeredIds += it.get().currentStyle().styleId
            registeredIds += tabStyles(it.get().currentStyle()).map { style -> style.styleId }
        }
    }

    private fun tabStyles(style: ChannelInfoStyle) = listOf(
        style.mediaStyle, style.filesStyle, style.voiceStyle, style.linkStyle, style.commonGroupsStyle,
    )

    class StyleMessageInfoActivity : MessageInfoActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(R.style.SceytAppTheme)
            super.onCreate(savedInstanceState)
        }

        override fun loadMessageInfoFragment() = Unit
    }

    class StyleChannelInfoActivity : ChannelInfoActivity() {
        private lateinit var pager: ViewPager2

        override val defaultViewModelProviderFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                mock<ChannelInfoViewModel>(defaultAnswer = Answers.RETURNS_DEEP_STUBS) as T
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(R.style.SceytAppTheme)
            super.onCreate(savedInstanceState)
        }

        override fun setActivityContentView() {
            pager = ViewPager2(this).apply { id = R.id.viewPager }
            setContentView(pager)
        }

        override fun setupPagerAdapter(viewPager: ViewPager2?, tabLayout: TabLayout?) {
            val fragment = StyleMediaFragment().apply {
                arguments = ChannelInfoMediaFragment.newInstance(channel, style.mediaStyle.styleId).arguments
            }
            pager.adapter = ViewPagerAdapter(this, listOf(fragment))
        }

        fun currentStyle() = style
    }

    class StyleMediaFragment : ChannelInfoMediaFragment() {
        // Exercise the real style lookup without loading channel attachments.
        override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) = Unit

        fun currentStyle() = mediaStyle
    }
}
