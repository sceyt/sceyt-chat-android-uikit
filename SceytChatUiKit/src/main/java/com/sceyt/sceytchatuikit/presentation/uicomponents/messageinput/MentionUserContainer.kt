package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.extensions.getCompatDrawable
import com.sceyt.sceytchatuikit.extensions.screenHeightPx
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.presentation.common.MaxHeightLinearLayoutManager
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapters.metions.MentionUserViewHolderFactory
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapters.metions.UsersAdapter
import com.sceyt.sceytchatuikit.shared.utils.ViewUtil

class MentionUserContainer @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {
    private var mentionUsersAdapter: UsersAdapter? = null
    private var showAnimation: ValueAnimator? = null
    private var hideAnimation: ValueAnimator? = null
    private var userClickListener: UsersAdapter.ClickListener? = null

    var recyclerView: RecyclerView? = null
        private set

    fun initWithMessageInputView(view: MessageInputView): MentionUserContainer {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(RecyclerView(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0).apply {
                gravity = Gravity.BOTTOM
                setMargins(0, 0, 0, view.height)
            }
            background = context.getCompatDrawable(R.drawable.sceyt_bg_mention_users)
            layoutManager = MaxHeightLinearLayoutManager(context, (screenHeightPx() * 0.4f).toInt())
            recyclerView = this
            isVisible = false
        })
        return this
    }

    fun setMentionList(users: List<SceytMember>) {
        if (users.isEmpty() && mentionUsersAdapter == null) return

        if (mentionUsersAdapter == null) {
            mentionUsersAdapter = UsersAdapter(users.toArrayList(), MentionUserViewHolderFactory(context) {
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

        if (users.isNotEmpty()) {
            show()
        } else hide()
    }

    private fun show() {
        if (showAnimation?.isRunning == true) return
        with(recyclerView ?: return) {
            hideAnimation?.cancel()
            val fromHeight = if (height <= 1 || !isVisible) 1 else height
            showAnimation = ViewUtil.expandHeight(this, from = fromHeight, duration = 200)
            isVisible = true
        }
    }

    private fun hide() {
        if (hideAnimation?.isRunning == true) return
        with(recyclerView ?: return) {
            showAnimation?.cancel()
            hideAnimation = ViewUtil.collapseHeight(this, to = 0, duration = 200) {
                isVisible = false
                mentionUsersAdapter = null
                hideAnimation = null
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