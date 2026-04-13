package com.sceyt.chatuikit.presentation.components.channel_info.media.adapter

import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.withTranslation
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

class MediaStickHeaderItemDecoration(
    private val mListener: StickyHeaderInterface<*>
) : RecyclerView.ItemDecoration() {
    private var mStickyHeaderHeight = 0
    private var oldHeader: View? = null

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)
        val topChild = parent.getChildAt(0) ?: return
        val topChildPosition = parent.getChildAdapterPosition(topChild)
        if (topChildPosition == RecyclerView.NO_POSITION) {
            oldHeader?.let { drawHeader(c, it) }
            return
        }
        val currentHeader = getHeaderViewForItem(topChildPosition, parent)
        fixLayoutSize(parent, currentHeader.root)
        val contactPoint = currentHeader.root.bottom
        val childInContact = getChildInContact(parent, contactPoint, topChildPosition)

        if (childInContact != null) {
            val isHeader = mListener.isHeader(parent.getChildAdapterPosition(childInContact))
            if (isHeader) {
                moveHeader(c, currentHeader.root, childInContact)
                return
            }
        }
        oldHeader = currentHeader.root
        drawHeader(c, currentHeader.root)
    }

    private fun getHeaderViewForItem(
        headerPosition: Int,
        parent: RecyclerView
    ): ViewBinding {
        return mListener.bindHeaderData(parent, headerPosition)
    }

    private fun drawHeader(c: Canvas, header: View) {
        c.withTranslation(0f, 0f) {
            header.draw(this)
        }
    }

    private fun moveHeader(c: Canvas, currentHeader: View, nextHeader: View) {
        c.withTranslation(0f, (nextHeader.top - currentHeader.height).toFloat()) {
            currentHeader.draw(this)
        }
    }

    private fun getChildInContact(
        parent: RecyclerView,
        contactPoint: Int,
        currentHeaderPos: Int
    ): View? {
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
            val childBottomPosition: Int = if (child.top > 0) {
                child.bottom + heightTolerance
            } else {
                child.bottom
            }
            if (childBottomPosition > contactPoint) {
                if (child.top <= contactPoint) {
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
        val heightSpec =
            View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.UNSPECIFIED)

        // Specs for children (headers)
        val childWidthSpec = ViewGroup.getChildMeasureSpec(
            widthSpec,
            parent.paddingLeft + parent.paddingRight,
            view.layoutParams.width
        )
        val childHeightSpec = ViewGroup.getChildMeasureSpec(
            heightSpec,
            parent.paddingTop + parent.paddingBottom,
            view.layoutParams.height
        )
        view.measure(childWidthSpec, childHeightSpec)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight.also { mStickyHeaderHeight = it })
    }

    interface StickyHeaderInterface<T : ViewBinding> {

        /**
         * This method gets called by [MediaStickHeaderItemDecoration] to setup the header View.
         * @param recyclerView View. Header to set the data on.
         * @param headerPosition int. Position of the header item in the adapter.
         */
        fun bindHeaderData(recyclerView: RecyclerView, headerPosition: Int): T

        /**
         * This method gets called by [MediaStickHeaderItemDecoration] to verify whether the item represents a header.
         * @param itemPosition int.
         * @return true, if item at the specified adapter's position represents a header.
         */
        fun isHeader(itemPosition: Int): Boolean
    }
}