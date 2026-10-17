package com.sceyt.chatuikit.presentation.custom_views

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.bumptech.glide.request.Request
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.presentation.custom_views.AvatarView.DefaultAvatar
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AvatarViewTest {

    @Test
    fun `clearing recycled request keeps initials drawable empty`() {
        val avatarView = AvatarView(RuntimeEnvironment.getApplication())
        val stalePlaceholder = ColorDrawable(Color.RED)
        val request = mock<Request> {
            on { clear() } doAnswer {
                avatarView.setImageDrawable(stalePlaceholder)
            }
        }
        DrawableImageViewTarget(avatarView).request = request
        avatarView.setDefaultAvatar(DefaultAvatar.FromInitials("Alice Brown"))

        avatarView.setImageUrl(null)

        assertThat(avatarView.drawable).isNull()
        verify(request).clear()
    }

    @Test
    fun `rebinding same url does not clear active request`() {
        val avatarView = AvatarView(RuntimeEnvironment.getApplication())
        val imageUrl = "https://example.com/avatar.jpg"
        AvatarView::class.java.getDeclaredField("imageUrl").apply {
            isAccessible = true
            set(avatarView, imageUrl)
        }
        val request = mock<Request>()
        DrawableImageViewTarget(avatarView).request = request

        avatarView.setImageUrl(imageUrl)

        verify(request, never()).clear()
    }
}
