package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.stickydate

import android.graphics.Canvas
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.DateSeparatorViewHolder

class DateStickHeaderItemDecoration4(
        private val mListener: StickyHeaderInterface,
        private var rv: StickyHeaderListener,
) : RecyclerView.ItemDecoration() {
    private var mStickyHeaderHeight = 0
    private var oldHeader: StycyDateView? = null
    private var oldDateSeparatorViewHolder: DateSeparatorViewHolder? = null
    private var bind: StycyDateView? = null

    init {
        /* rv.addRVScrollListener(onScrollStateChanged = { recyclerView: RecyclerView, newState: Int ->
             if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                 startAoutHideTImer()
             } else {
                 show()
             }
         })*/
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)
        val topChild = parent.getChildAt(0) ?: return
        val topChildPosition = parent.getChildAdapterPosition(topChild)
        if (topChildPosition == RecyclerView.NO_POSITION) {
            oldHeader?.let { it.isVisible = false }
            return
        }
        val currentHeader = rv.getHeaderViewForItem(topChildPosition, parent)
        fixLayoutSize(parent, currentHeader)
        val contactPoint = currentHeader.bottom
        val childInContact = getChildInContact(parent, contactPoint, topChildPosition)


        val dateSeparatorViewHolder = (parent.getChildViewHolder(topChild) as? DateSeparatorViewHolder)
        var skipDrawHeader = false
        if (dateSeparatorViewHolder != null) {
            val diff = topChild.bottom - (currentHeader.bottom - currentHeader.paddingBottom)
            if (diff <= 0) {
                dateSeparatorViewHolder.showHide(false)
            } else {
                if (topChildPosition == 0) {
                    skipDrawHeader = true
                }
                dateSeparatorViewHolder.showHide(true)
            }
            oldDateSeparatorViewHolder = dateSeparatorViewHolder
        } else {
            if (topChildPosition == 0) {
                skipDrawHeader = true
            }
            oldDateSeparatorViewHolder?.showHide(true)
            oldDateSeparatorViewHolder = null
        }

        if (childInContact != null) {
            val pos = parent.getChildAdapterPosition(childInContact)
            if (mListener.isHeader(pos)) {
                moveHeader(c, currentHeader, childInContact)
                return
            }
        }

        val verticalOffset = parent.computeVerticalScrollOffset()
        if (verticalOffset > parent.paddingTop && !skipDrawHeader) {

            drawHeader(c, currentHeader)
            oldHeader = currentHeader
        } else {
            currentHeader.isVisible = false
            oldHeader?.isVisible = false
        }
    }

    private fun getHeaderViewForItem(headerPosition: Int, parent: RecyclerView): StycyDateView {
        val header = bind ?: StycyDateView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            bind = this
        }
        mListener.bindHeaderData(header, headerPosition)
        return header
    }

    private fun drawHeader(c: Canvas, header: StycyDateView) {
        /*  c.save()
          c.translate(0f, 0f)
          header.draw(c)
          header.setCanvas(c)
          c.restore()*/
        rv.needShow()
    }

    private fun moveHeader(c: Canvas, currentHeader: View, nextHeader: View) {
        /* c.save()
         c.translate(0f, (nextHeader.top - (currentHeader.height + 10)).toFloat())
         currentHeader.draw(c)
         c.restore()
 */
        rv.move((nextHeader.top - (currentHeader.height + 10)).toFloat())
    }

    private fun getChildInContact(parent: RecyclerView, contactPoint: Int, currentHeaderPos: Int): View? {
        var childInContact: View? = null
        for (i in 0 until parent.childCount) {
            var heightTolerance = 0
            val child = parent.getChildAt(i)

            //measure height tolerance with child if child is another header
            if (currentHeaderPos != i) {
                val isChildHeader = mListener.isHeader(parent.getChildAdapterPosition(child))
                if (isChildHeader) {
                    heightTolerance = mStickyHeaderHeight - child.height
                }
            }

            //add heightTolerance if child top be in display area
            var diff = 10
            val childBottomPosition: Int = if (child.top > 0) {
                child.bottom + heightTolerance
            } else {
                diff *= -1
                child.bottom
            }

            if (childBottomPosition > contactPoint + diff) {
                Log.i("sdfsdfdsfsdfsd", "getChildInContact: childBottomPosition $childBottomPosition top-${child.top} point-${contactPoint + diff} bottom-${child}  pos $currentHeaderPos")
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
        // Specs for parent (RecyclerView)
        val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.UNSPECIFIED)

        val topBottomPadding = 10
        view.setPadding(view.paddingLeft, topBottomPadding, view.paddingRight, topBottomPadding);

        // Specs for children (headers)
        val childWidthSpec = ViewGroup.getChildMeasureSpec(widthSpec, parent.paddingLeft + parent.paddingRight, view.layoutParams.width)
        val childHeightSpec = ViewGroup.getChildMeasureSpec(heightSpec, parent.paddingTop + parent.paddingBottom, view.layoutParams.height)
        view.measure(childWidthSpec, childHeightSpec)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight.also { mStickyHeaderHeight = it })
    }
}