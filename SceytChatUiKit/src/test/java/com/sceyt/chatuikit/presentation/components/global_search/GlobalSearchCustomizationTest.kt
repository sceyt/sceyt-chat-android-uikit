package com.sceyt.chatuikit.presentation.components.global_search

import android.graphics.Color
import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.recyclerview.widget.RecyclerView
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.presentation.components.channel_list.channels.ChannelListFragment
import com.sceyt.chatuikit.presentation.components.global_search.adapters.GlobalSearchListAdapter
import com.sceyt.chatuikit.presentation.components.global_search.adapters.GlobalSearchSuggestionsAdapter
import com.sceyt.chatuikit.presentation.components.global_search.adapters.GlobalSearchTabsAdapter
import com.sceyt.chatuikit.styles.channel.ChannelItemStyle
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.MessageDeliveryStatusIcons
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.search.GlobalSearchStyle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GlobalSearchCustomizationTest {
    companion object {
        private fun createTestGlobalSearchStyle(): GlobalSearchStyle {
            return GlobalSearchStyle(
                backgroundColor = Color.WHITE,
                dividerColor = Color.LTGRAY,
                searchInputBackgroundColor = Color.WHITE,
                navigationIconColor = Color.BLACK,
                searchIconColor = Color.DKGRAY,
                clearIconColor = Color.DKGRAY,
                searchHintColor = Color.GRAY,
                tabSelectedBackgroundColor = Color.LTGRAY,
                tabSelectedTextColor = Color.BLACK,
                tabUnselectedBackgroundColor = Color.WHITE,
                tabUnselectedTextColor = Color.DKGRAY,
                tabStrokeColor = Color.GRAY,
                chipBackgroundColor = Color.WHITE,
                chipTextColor = Color.DKGRAY,
                chipStrokeColor = Color.GRAY,
                suggestionChipBackgroundColor = Color.LTGRAY,
                suggestionChipTextColor = Color.BLACK,
                suggestionChipIconColor = Color.DKGRAY,
                selectedMemberChipBackgroundColor = Color.BLACK,
                selectedMemberChipTextColor = Color.WHITE,
                selectedMemberChipIconColor = Color.WHITE,
                selectedMemberChipPendingBackgroundColor = Color.DKGRAY,
                selectedMemberChipPendingTextColor = Color.WHITE,
                selectedMemberChipPendingIconColor = Color.WHITE,
                highlightTextColor = Color.BLUE,
                titleTextStyle = TextStyle(color = Color.BLACK),
                subtitleTextStyle = TextStyle(color = Color.DKGRAY),
                metaTextStyle = TextStyle(color = Color.GRAY),
                sectionTextStyle = TextStyle(color = Color.GRAY),
                emptyTitleTextStyle = TextStyle(color = Color.BLACK),
                emptySubtitleTextStyle = TextStyle(color = Color.DKGRAY),
                avatarStyle = AvatarStyle(),
                channelItemStyle = createTestChannelItemStyle()
            )
        }

        private fun createTestChannelItemStyle(): ChannelItemStyle {
            return ChannelItemStyle(
                backgroundColor = Color.TRANSPARENT,
                pinnedChannelBackgroundColor = Color.TRANSPARENT,
                dividerColor = Color.TRANSPARENT,
                linkTextColor = Color.BLUE,
                mutedIcon = null,
                pinIcon = null,
                autoDeletedChannelIcon = null,
                unreadMentionIcon = null,
                messageDeliveryStatusIcons = MessageDeliveryStatusIcons(null, null, null, null, null),
                deliveryStatusIndicatorSize = 0,
                messageDeletedStateText = "",
                subjectTextStyle = TextStyle(color = Color.BLACK),
                lastMessageTextStyle = TextStyle(color = Color.DKGRAY),
                dateTextStyle = TextStyle(color = Color.GRAY),
                lastMessageSenderNameTextStyle = TextStyle(color = Color.BLACK),
                deletedTextStyle = TextStyle(color = Color.GRAY),
                draftPrefixTextStyle = TextStyle(color = Color.GRAY),
                channelEventTextStyle = TextStyle(color = Color.GRAY),
                unreadCountTextStyle = TextStyle(color = Color.WHITE),
                unreadCountMutedStateTextStyle = TextStyle(color = Color.WHITE),
                mentionTextStyle = TextStyle(color = Color.BLUE),
                unreadMentionBackgroundStyle = BackgroundStyle(),
                unreadMentionMutedStateBackgroundStyle = BackgroundStyle(),
                avatarStyle = AvatarStyle(),
                channelTitleFormatter = SceytChatUIKit.formatters.channelNameFormatter,
                channelSubtitleFormatter = SceytChatUIKit.formatters.channelListItemSubtitleFormatter,
                channelDateFormatter = SceytChatUIKit.formatters.channelDateFormatter,
                lastMessageSenderNameFormatter = SceytChatUIKit.formatters.channelLastMessageSenderNameFormatter,
                mentionUserNameFormatter = SceytChatUIKit.formatters.mentionUserNameFormatter,
                reactedUserNameFormatter = SceytChatUIKit.formatters.reactedUserNameFormatter,
                channelEventTitleFormatter = SceytChatUIKit.formatters.channelListChannelEventTitleFormatter,
                unreadCountFormatter = SceytChatUIKit.formatters.unreadCountFormatter,
                lastMessageBodyFormatter = SceytChatUIKit.formatters.channelLastMessageBodyFormatter,
                unsupportedMessageBodyFormatter = SceytChatUIKit.formatters.unsupportedMessageShortBodyFormatter,
                draftMessageBodyFormatter = SceytChatUIKit.formatters.channelDraftLastMessageBodyFormatter,
                attachmentIconProvider = SceytChatUIKit.providers.channelListAttachmentIconProvider,
                presenceStateColorProvider = SceytChatUIKit.providers.presenceStateColorProvider,
                channelAvatarRenderer = SceytChatUIKit.renderers.channelAvatarRenderer
            )
        }
    }

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `custom activity createFragment can replace built in tab fragment`() {
        val activity = InspectableGlobalSearchActivity()

        assertThat(activity.exposeCreateFragment(GlobalSearchTab.Chats))
            .isInstanceOf(TrackingChatsSearchFragment::class.java)
    }

    @Test
    fun `custom activity and fragment adapter factories are used`() {
        val activity = InspectableGlobalSearchActivity()
        val fragment = TrackingChatsSearchFragment()

        activity.exposeCreateTabsAdapter()
        activity.exposeCreateSuggestionsAdapter()
        fragment.exposeCreateListAdapter()

        assertThat(activity.tabsAdapterCreated).isTrue()
        assertThat(activity.suggestionsAdapterCreated).isTrue()
        assertThat(fragment.listAdapterCreated).isTrue()
    }

    @Test
    fun `channel list fragment openGlobalSearch hook can be overridden`() {
        val fragment = TrackingChannelListFragment()

        fragment.triggerOpenGlobalSearch()

        assertThat(fragment.openGlobalSearchCalls).isEqualTo(1)
    }

    @Test
    fun `fragment instance creators inject styleId and sessionId`() {
        val fragment = ChatsSearchFragment.newInstance(
            styleId = "style-id",
            sessionId = "session-id"
        )

        assertThat(fragment.arguments?.getString(GlobalSearchActivity.STYLE_ID_KEY))
            .isEqualTo("style-id")
        assertThat(fragment.arguments?.getString(GlobalSearchActivity.SESSION_ID_KEY))
            .isEqualTo("session-id")
    }

    @Test
    fun `custom fragment can resolve session from arguments without host activity access`() {
        val sessionId = GlobalSearchSessionRegistry.newSessionId()
        val sessionStore = GlobalSearchSessionStore(
            GlobalSearchSessionState(activeTab = GlobalSearchTab.Chats)
        )
        GlobalSearchSessionRegistry.register(sessionId, sessionStore)
        val fragment = ChatsSearchFragment.newInstance(
            styleId = "style-id",
            sessionId = sessionId
        )

        sessionStore.update { it.copy(query = "jam") }

        val session = GlobalSearchSessionResolver.require(fragment.arguments)

        assertThat(session.state.value.query).isEqualTo("jam")
        assertThat(session.state.value.activeTab).isEqualTo(GlobalSearchTab.Chats)
        GlobalSearchSessionRegistry.unregister(sessionId)
    }

    class TrackingChannelListFragment : ChannelListFragment() {
        var openGlobalSearchCalls = 0
            private set

        override fun openGlobalSearch(sourceView: View?) {
            openGlobalSearchCalls++
        }

        fun triggerOpenGlobalSearch() {
            openGlobalSearch(null)
        }
    }

    class InspectableGlobalSearchActivity : GlobalSearchActivity() {
        var tabsAdapterCreated = false
            private set
        var suggestionsAdapterCreated = false
            private set

        override fun provideTabs(): List<GlobalSearchTab> {
            return listOf(GlobalSearchTab.Chats)
        }

        override fun buildStyle(): GlobalSearchStyle = createTestGlobalSearchStyle()

        override fun createFragment(tab: GlobalSearchTab) = TrackingChatsSearchFragment()

        override fun createTabsAdapter(): RecyclerView.Adapter<*> {
            tabsAdapterCreated = true
            return GlobalSearchTabsAdapter(createTestGlobalSearchStyle(), provideTabs()) {}
        }

        override fun createSuggestionsAdapter(): RecyclerView.Adapter<*> {
            suggestionsAdapterCreated = true
            return GlobalSearchSuggestionsAdapter(createTestGlobalSearchStyle()) {}
        }

        fun exposeCreateFragment(tab: GlobalSearchTab) = createFragment(tab)

        fun exposeCreateTabsAdapter() = createTabsAdapter()

        fun exposeCreateSuggestionsAdapter() = createSuggestionsAdapter()
    }
    class TrackingChatsSearchFragment : ChatsSearchFragment() {
        var listAdapterCreated = false
            private set

        override fun createListAdapter(): GlobalSearchListAdapter {
            listAdapterCreated = true
            return object : GlobalSearchListAdapter(
                style = createTestGlobalSearchStyle(),
                onChannelClick = {},
                onMessageClick = { _, _ -> },
                onAttachmentClick = {}
            ) {}
        }

        fun exposeCreateListAdapter() = createListAdapter()
    }
}
