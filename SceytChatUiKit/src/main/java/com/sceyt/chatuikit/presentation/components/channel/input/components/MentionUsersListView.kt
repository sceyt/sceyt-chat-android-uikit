package com.sceyt.chatuikit.presentation.components.channel.input.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.screenHeightPx
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.presentation.common.recyclerview.MaxHeightLinearLayoutManager
import com.sceyt.chatuikit.presentation.components.channel.input.MessageInputView
import com.sceyt.chatuikit.presentation.components.channel.input.adapters.metions.MentionUserViewHolderFactory
import com.sceyt.chatuikit.presentation.components.channel.input.adapters.metions.UsersAdapter
import com.sceyt.chatuikit.shared.utils.ViewUtil
import com.sceyt.chatuikit.styles.input.MentionUsersListStyle

class MentionUsersListView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private var mentionUsersAdapter: UsersAdapter? = null
    private var showAnimation: ValueAnimator? = null
    private var hideAnimation: ValueAnimator? = null
    private var userClickListener: UsersAdapter.ClickListener? = null
    private lateinit var style: MentionUsersListStyle

    var recyclerView: RecyclerView? = null
        private set

    fun initWithMessageInputView(view: MessageInputView): MentionUsersListView {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(RecyclerView(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0).apply {
                gravity = Gravity.BOTTOM
                setMargins(0, 0, 0, view.height)
            }
            background = getBackgroundDrawable(style.backgroundColor)
            layoutManager = MaxHeightLinearLayoutManager(context, (screenHeightPx() * 0.4f).toInt())
            recyclerView = this
            isVisible = false
        })
        return this
    }

    fun setMentionList(users: List<SceytMember>) {
        if (users.isEmpty() && mentionUsersAdapter == null) return

        if (mentionUsersAdapter == null) {
            mentionUsersAdapter = UsersAdapter(users.toArrayList(), MentionUserViewHolderFactory(
                context = context,
                style = style,
                listeners = {
                    userClickListener?.onClick(it)
                    hide()
                }
            ))

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

    private fun getBackgroundDrawable(@ColorInt bgColor: Int): LayerDrawable {
        val shadowDrawable = context.getCompatDrawable(R.drawable.sceyt_shadow_124645)
        val cornerRadius = dpToPx(16f).toFloat()
        val outerRadii = floatArrayOf(
            cornerRadius, cornerRadius, // Top-left and top-right corners
            cornerRadius, cornerRadius, // Bottom-left and bottom-right corners
            0f, 0f, 0f, 0f // No corners for bottom left/right
        )

        val shapeDrawable = ShapeDrawable(RoundRectShape(outerRadii, null, null)).apply {
            paint.color = bgColor
        }

        val layers = arrayOf(shadowDrawable, shapeDrawable)
        val layerDrawable = LayerDrawable(layers)
        return layerDrawable
    }

    fun setUserClickListener(listener: UsersAdapter.ClickListener) {
        userClickListener = listener
    }

    internal fun onInputSizeChanged(height: Int) {
        recyclerView?.updateLayoutParams<MarginLayoutParams> {
            setMargins(0, 0, 0, height)
        }
    }

    internal fun setStyle(style: MentionUsersListStyle) {
        this.style = style
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