package com.sceyt.chatuikit.presentation.components.global_search

import android.graphics.Color
import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.presentation.components.global_search.chats.ChatsSearchFragment
import com.sceyt.chatuikit.styles.StyleRegistry
import com.sceyt.chatuikit.styles.search.GlobalSearchStyle
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class GlobalSearchActivityStyleTest {

    private lateinit var controller: ActivityController<StyleLifecycleActivity>
    private val styleIds = mutableSetOf<String>()

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        val intent = GlobalSearchActivity.createIntent(context)
            .setClass(context, StyleLifecycleActivity::class.java)
        controller = Robolectric.buildActivity(StyleLifecycleActivity::class.java, intent).setup()
        styleIds += controller.get().currentStyle().styleId
    }

    @After
    fun tearDown() {
        val activity = controller.get()
        styleIds += activity.currentStyle().styleId
        if (!activity.isDestroyed) {
            activity.finish()
            controller.pause().stop().destroy()
        }
        styleIds.forEach { StyleRegistry.unregister(it) }
        GlobalSearchSessionRegistry.unregister(SESSION_ID)
    }

    @Test
    fun `recreation preserves the activity style and restored tab styling`() {
        val originalStyle = controller.get().currentStyle()
        assertThat(currentTab().currentStyle()).isSameInstanceAs(originalStyle)

        repeat(2) {
            controller.recreate()
            styleIds += controller.get().currentStyle().styleId

            val tab = currentTab()
            assertThat(tab.arguments?.getString(GlobalSearchActivity.STYLE_ID_KEY))
                .isEqualTo(originalStyle.styleId)
            assertThat(tab.currentStyle().chatsPageStyle.backgroundColor).isEqualTo(Color.MAGENTA)
            assertThat(controller.get().currentStyle()).isSameInstanceAs(originalStyle)
            assertThat(tab.currentStyle()).isSameInstanceAs(originalStyle)
            assertThat(StyleRegistry.getTyped<GlobalSearchStyle>(originalStyle.styleId))
                .isSameInstanceAs(originalStyle)
        }
    }

    @Test
    fun `finishing after recreation removes the original registered style`() {
        val originalStyleId = controller.get().currentStyle().styleId
        controller.recreate()
        val currentStyleId = controller.get().currentStyle().styleId
        styleIds += currentStyleId
        assertThat(StyleRegistry.getTyped<GlobalSearchStyle>(originalStyleId)).isNotNull()

        controller.get().finish()
        controller.pause().stop().destroy()

        assertThat(StyleRegistry.getTyped<GlobalSearchStyle>(originalStyleId)).isNull()
        assertThat(StyleRegistry.getTyped<GlobalSearchStyle>(currentStyleId)).isNull()
    }

    private fun currentTab() = controller.get().supportFragmentManager.fragments
        .filterIsInstance<StyleTabFragment>().single()

    class StyleLifecycleActivity : GlobalSearchActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(R.style.SceytAppTheme)
            super.onCreate(savedInstanceState)
        }

        override fun buildStyle() = super.buildStyle().let {
            it.copy(chatsPageStyle = it.chatsPageStyle.copy(backgroundColor = Color.MAGENTA))
        }

        override fun provideTabs() = listOf(GlobalSearchTab.Chats)

        override fun createFragment(tab: GlobalSearchTab) = StyleTabFragment().apply {
            arguments = Bundle().apply {
                putString(STYLE_ID_KEY, style.styleId)
                putString(GlobalSearchSessionResolver.SESSION_ID_KEY, SESSION_ID)
            }
        }

        override fun initViews() {
            binding.viewPager.adapter = createPagerAdapter()
        }

        override fun observeState() = Unit

        fun currentStyle() = style
    }

    class StyleTabFragment : ChatsSearchFragment() {
        // Keep the real fragment style lookup and rendering without loading search results.
        override fun initViews() = Unit
        override fun observeState() = Unit

        fun currentStyle() = style
    }

    companion object {
        private const val SESSION_ID = "global-search-style-test"
    }
}
