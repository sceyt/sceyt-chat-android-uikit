package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.extensions.screenHeightPx
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.presentation.common.MaxHeightLinearLayoutManager
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapters.metions.UserViewHolderFactory
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapters.metions.UsersAdapter
import com.sceyt.sceytchatuikit.shared.utils.ViewUtil

class MentionUserContainer @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {
    private var mentionUsersAdapter: UsersAdapter? = null
    private var mentionUserAnimation: ValueAnimator? = null
    private var userClickListener: UsersAdapter.ClickListener? = null

    var recyclerView: RecyclerView? = null
        private set

    fun initWithMessageInputView(view: MessageInputView): MentionUserContainer {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(RecyclerView(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM
                setMargins(0, 0, 0, view.height)
            }
            background = ContextCompat.getDrawable(context, R.drawable.sceyt_bg_mention_users)
            layoutManager = MaxHeightLinearLayoutManager(context, (screenHeightPx() * 0.4f).toInt())
            recyclerView = this
        })
        return this
    }

    fun setMentionList(users: List<SceytMember>) {
        if (users.isEmpty() && mentionUsersAdapter == null) return

        if (mentionUsersAdapter == null) {
            mentionUsersAdapter = UsersAdapter(users.toArrayList(), UserViewHolderFactory(context) {
                userClickListener?.onClick(it)
                hide()
            })

            with(recyclerView ?: return) {
                adapter = mentionUsersAdapter
                itemAnimator = null
                if (itemDecorationCount == 0)
                    addItemDecoration(FirstLastItemDecoration(20))
            }
        } else mentionUsersAdapter?.notifyUpdate(users)

        with(recyclerView ?: return) {
            if (users.isNotEmpty()) {
                mentionUserAnimation?.cancel()
                val fromHeight = if (height <= 1) 1 else height
                mentionUserAnimation = ViewUtil.expandHeight(this, from = fromHeight, duration = 300)
                isVisible = true
            } else hide()
        }
    }

    private fun hide() {
        with(recyclerView ?: return) {
            mentionUserAnimation?.cancel()
            mentionUserAnimation = ViewUtil.collapseHeight(this, to = 0, duration = 200) {
                isVisible = false
                mentionUsersAdapter = null
            }
        }
    }

    fun setUserClickListener(listener: UsersAdapter.ClickListener) {
        userClickListener = listener
    }

    fun onInputSizeChanged(height: Int) {
        recyclerView?.updateLayoutParams<MarginLayoutParams> {
            setMargins(0, 0, 0, height)
        }
    }

    class FirstLastItemDecoration(private val verticalPadding: Int) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            super.getItemOffsets(outRect, view, parent, state)
            val position = parent.getChildAdapterPosition(view)

            if (position == 0)
                outRect.top = verticalPadding

            if (position == parent.adapter?.itemCount?.minus(1))
                outRect.bottom = verticalPadding
        }
    }
}