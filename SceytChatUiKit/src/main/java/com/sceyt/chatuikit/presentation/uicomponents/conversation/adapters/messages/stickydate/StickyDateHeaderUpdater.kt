package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.stickydate

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.extensions.addRVScrollListener
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.DateSeparatorViewHolder
import com.sceyt.chatuikit.sceytstyles.MessagesListViewStyle

class StickyDateHeaderUpdater(
        recyclerView: RecyclerView,
        private var parentView: ViewGroup,
        private val listener: StickyHeaderInterface,
        private val messageItemStyle: MessagesListViewStyle
) {
    private val dateHeaderVerticalPadding = messageItemStyle.messageItemStyle.differentSenderMsgDistance

    init {
        recyclerView.addRVScrollListener(onScrolled = { rv, _, _ ->
            drawHeader(rv, true)
        })

        recyclerView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            drawHeader(recyclerView, false)
        }
    }

    private val headerView: StickyDateHeaderView by lazy { initHeader() }
    private var stickyHeaderHeight = 0
    private var oldDateSeparatorViewHolder: DateSeparatorViewHolder? = null

    private fun drawHeader(rv: RecyclerView, isScrolling: Boolean) {
        val topChild = rv.getChildAt(0) ?: return
        val topChildPosition = rv.getChildAdapterPosition(topChild)
        if (topChildPosition == RecyclerView.NO_POSITION)
            return

        listener.bindHeaderData(headerView, topChildPosition)

        if (isScrolling || topChildPosition == 0)
            headerView.showWithAnim(topChildPosition != 0)
        else headerView.startAutoHide()

        fixLayoutSize(rv, headerView)
        val contactPoint = headerView.bottom
        val childInContact = getChildInContact(rv, contactPoint)

        // Ignore showing header when the first item is a header
        var ignoreShowingHeader = false
        if (topChildPosition == 0)
            ignoreShowingHeader = true

        val viewHolder = rv.getChildViewHolder(topChild)
        val dateSeparatorViewHolder = (viewHolder as? DateSeparatorViewHolder)
        if (dateSeparatorViewHolder != null) {
            val diff = topChild.bottom - (headerView.bottom - headerView.paddingBottom)
            if (diff <= 0) {
                dateSeparatorViewHolder.showHide(false)
                ignoreShowingHeader = false
            } else dateSeparatorViewHolder.showHide(true)

            oldDateSeparatorViewHolder = dateSeparatorViewHolder
        } else {
            oldDateSeparatorViewHolder?.showHide(true)
            oldDateSeparatorViewHolder = null
        }

        if (ignoreShowingHeader) {
            headerView.isVisible = false
            return
        }

        if (childInContact != null) {
            val pos = rv.getChildAdapterPosition(childInContact)
            if (listener.isHeader(pos)) {
                moveHeader(childInContact)
                return
            }
        }

        showHeader()
    }

    private fun initHeader(): StickyDateHeaderView {
        return StickyDateHeaderView(parentView.context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(paddingLeft, dateHeaderVerticalPadding, paddingRight, paddingBottom)
            setStyle(messageItemStyle)
            parentView.addView(this)
        }
    }

    private fun showHeader() {
        headerView.translationY = 0f
        headerView.isVisible = true
    }

    private fun moveHeader(nextHeader: View) {
        headerView.translationY = (nextHeader.top - (headerView.height + dateHeaderVerticalPadding)).toFloat()
    }

    private fun getChildInContact(parent: RecyclerView, contactPoint: Int): View? {
        var childInContact: View? = null
        for (i in 0 until parent.childCount) {
            var heightTolerance: Int
            val child = parent.getChildAt(i)

            val isChildHeader = listener.isHeader(parent.getChildAdapterPosition(child))
            if (isChildHeader) {
                heightTolerance = stickyHeaderHeight - child.height
            } else continue

            //add heightTolerance if child top be in display area
            var diff = dateHeaderVerticalPadding
            val childBottomPosition: Int = if (child.top > 0) {
                child.bottom + heightTolerance
            } else {
                diff *= -1
                child.bottom
            }

            if (childBottomPosition > contactPoint + diff) {
                if (child.top <= contactPoint + diff) {
                    // This child overlaps the contactPoint
                    childInContact = child
                    break
                }
            }
        }
        return childInContact
    }

    /**
     * Properly measures and layouts the top sticky header.
     * @param parent ViewGroup: RecyclerView in this case.
     */
    private fun fixLayoutSize(parent: ViewGroup, view: View) {
        if (stickyHeaderHeight != 0) return // already fixed
        // Specs for parent (RecyclerView)
        val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.UNSPECIFIED)

        // Specs for children (headers)
        val childWidthSpec = ViewGroup.getChildMeasureSpec(widthSpec, parent.paddingLeft + parent.paddingRight, view.layoutParams.width)
        val childHeightSpec = ViewGroup.getChildMeasureSpec(heightSpec, parent.paddingTop + parent.paddingBottom, view.layoutParams.height)
        view.measure(childWidthSpec, childHeightSpec)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight.also { stickyHeaderHeight = it })
    }
}