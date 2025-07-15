package com.sceyt.chatuikit.extensions

import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.math.abs


fun RecyclerView.isItemCompletelyDisplaying(position: Int): Boolean {
    if (adapter?.itemCount != 0) {
        val firstVisibleItemPosition = ((layoutManager) as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
        val lastVisibleItemPosition = ((layoutManager) as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
        if (firstVisibleItemPosition == position || lastVisibleItemPosition == position)
            return true
    }
    return false
}

fun RecyclerView.isLastItemDisplaying(): Boolean {
    if (adapter?.itemCount != 0) {
        val lastVisibleItemPosition = ((layoutManager) as LinearLayoutManager).findLastVisibleItemPosition()
        if (lastVisibleItemPosition != RecyclerView.NO_POSITION && lastVisibleItemPosition == adapter?.itemCount?.minus(1)) {
            return true
        }
    }
    return false
}

fun RecyclerView.isLastCompletelyItemDisplaying(): Boolean {
    if (adapter?.itemCount != 0) {
        val lastVisibleItemPosition = ((layoutManager) as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
        if (lastVisibleItemPosition != RecyclerView.NO_POSITION && lastVisibleItemPosition == adapter?.itemCount?.minus(1)) {
            return true
        }
    }
    return false
}

fun RecyclerView.isFirstItemDisplaying(): Boolean {
    if (adapter?.itemCount != 0) {
        val firstVisibleItemPosition = ((layoutManager) as LinearLayoutManager).findFirstVisibleItemPosition()
        if (firstVisibleItemPosition != RecyclerView.NO_POSITION && firstVisibleItemPosition == 0) {
            return true
        }
    }
    return false
}


fun RecyclerView.needLoadMore(limit: Int, dy: Int): Boolean {
    if (adapter?.itemCount == 0 || dy > 0) return false
    val firstVisibleItemPosition = ((layoutManager) as LinearLayoutManager).findFirstVisibleItemPosition()
    return firstVisibleItemPosition != RecyclerView.NO_POSITION && firstVisibleItemPosition < limit
}

fun RecyclerView.isFirstCompletelyItemDisplaying(): Boolean {
    if (adapter?.itemCount != 0) {
        val firstItemPosition = (layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
        if (firstItemPosition != RecyclerView.NO_POSITION && firstItemPosition == 0) {
            return true
        }
    }
    return false
}

fun RecyclerView.getFirstVisibleItemPosition(): Int {
    if (adapter?.itemCount != 0) {
        val position = (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        if (position != RecyclerView.NO_POSITION)
            return position
    }
    return RecyclerView.NO_POSITION
}

fun RecyclerView.getFirstCompletelyVisibleItemPosition(): Int {
    if (adapter?.itemCount != 0) {
        val position = (layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
        if (position != RecyclerView.NO_POSITION)
            return position
    }
    return RecyclerView.NO_POSITION
}

fun RecyclerView.getLastVisibleItemPosition(): Int {
    if (adapter?.itemCount != 0) {
        val position = (layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
        if (position != RecyclerView.NO_POSITION)
            return position
    }
    return RecyclerView.NO_POSITION
}

fun RecyclerView.lastCompletelyVisibleItemPosition(): Int {
    if (adapter?.itemCount != 0) {
        val position = (layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
        if (position != RecyclerView.NO_POSITION)
            return position
    }
    return RecyclerView.NO_POSITION
}

fun RecyclerView.lastVisibleItemPosition(): Int {
    if (adapter?.itemCount != 0) {
        val position = (layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
        if (position != RecyclerView.NO_POSITION)
            return position
    }
    return RecyclerView.NO_POSITION
}

fun RecyclerView.centerVisibleItemPosition(): Int {
    val layoutManager = layoutManager as LinearLayoutManager
    val recyclerViewCenterX = width / 2
    val recyclerViewCenterY = height / 2

    var closestPosition = RecyclerView.NO_POSITION
    var closestDistance = Int.MAX_VALUE

    for (i in layoutManager.findFirstVisibleItemPosition()..layoutManager.findLastVisibleItemPosition()) {
        val itemView = layoutManager.findViewByPosition(i)
        if (itemView != null) {
            val itemViewWidth = itemView.width
            val itemViewHeight = itemView.height
            val itemViewLeft = itemView.left
            val itemViewTop = itemView.top

            // Calculate the center points of the itemView
            val itemViewCenterX = itemViewLeft + itemViewWidth / 2
            val itemViewCenterY = itemViewTop + itemViewHeight / 2

            // Calculate the distance of the itemView's center to the RecyclerView's center
            val distance = (abs((recyclerViewCenterX - itemViewCenterX).toDouble()) + abs((recyclerViewCenterY - itemViewCenterY).toDouble())).toInt()

            // Update the closest item if this item is closer
            if (distance < closestDistance) {
                closestDistance = distance
                closestPosition = i
            }
        }
    }
    return closestPosition
}

fun RecyclerView.checkIsNotVisibleItem(position: Int): Boolean {
    return ((layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() > position
            || (layoutManager as LinearLayoutManager).findLastVisibleItemPosition() < position)
}


fun RecyclerView.awaitAnimationEnd(callback: () -> Unit) {
    post {
        if (itemAnimator == null)
            callback.invoke()
        else itemAnimator?.isRunning { callback.invoke() }
    }
}


fun RecyclerView.addPagerSnapHelper() {
    if (onFlingListener == null)
        PagerSnapHelper().attachToRecyclerView(this)
}

fun RecyclerView.awaitToScrollFinish(position: Int, delay: Boolean = false, callback: (Int) -> Unit) {
    if (!checkIsNotVisibleItem(position)) {
        if (delay)
            Handler(Looper.getMainLooper()).postDelayed({ callback.invoke(scrollState) }, 100)
        else callback.invoke(scrollState)
    } else {
        addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                    callback.invoke(scrollState)
                    removeOnScrollListener(this)
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                    callback.invoke(scrollState)
                    removeOnScrollListener(this)
                }
            }
        })
    }
}


inline fun RecyclerView.addRVScrollListener(
        crossinline onScrollStateChanged: (recyclerView: RecyclerView, newState: Int) -> Unit = { _, _ -> },
        crossinline onScrolled: (recyclerView: RecyclerView, dx: Int, dy: Int) -> Unit = { _, _, _ -> },
) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            onScrollStateChanged(recyclerView, newState)
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (dx == 0 && dy == 0)
                return
            onScrolled(recyclerView, dx, dy)
        }
    })
}

fun RecyclerView.getLinearLayoutManager(): LinearLayoutManager {
    return layoutManager as LinearLayoutManager
}

fun RecyclerView.ViewHolder.bindPosition(cb: (Int) -> Unit) {
    bindingAdapterPosition.let {
        if (it != RecyclerView.NO_POSITION)
            cb.invoke(it)
    }
}

fun RecyclerView.notifyItemChangedSafety(position: Int, payload: Any?) {
    try {
        adapter?.notifyItemChanged(position, payload)
    } catch (ex: Exception) {
        println(ex.message)
    }
}

fun RecyclerView.smoothSnapToPosition(position: Int, snapMode: Int = LinearSmoothScroller.SNAP_TO_START) {
    val smoothScroller = object : LinearSmoothScroller(this.context) {
        override fun getVerticalSnapPreference(): Int = snapMode
        override fun getHorizontalSnapPreference(): Int = snapMode
    }
    smoothScroller.targetPosition = position
    layoutManager?.startSmoothScroll(smoothScroller)
}

fun DiffUtil.DiffResult.dispatchUpdatesToSafety(recyclerView: RecyclerView) {
    recyclerView.adapter?.let { adapter ->
        recyclerView.post {
            dispatchUpdatesTo(adapter)
        }
    }
}

suspend fun DiffUtil.DiffResult.dispatchUpdatesToSafetySuspend(
        recyclerView: RecyclerView,
) = suspendCancellableCoroutine {
    recyclerView.adapter?.let { adapter ->
        recyclerView.post {
            dispatchUpdatesTo(adapter)
            it.resumeWith(Result.success(Unit))
        }
    } ?: run {
        it.resumeWith(Result.success(Unit))
    }
}

fun RecyclerView.getChildTopByPosition(position: Int): Int {
    val layoutManager = layoutManager
            ?: return -1
    val childView = layoutManager.findViewByPosition(position)
            ?: return -1 // View for the position is not currently laid out or doesn't exist
    return childView.top
}

fun RecyclerView.isThePositionVisible(position: Int): Boolean {
    val layoutManager = layoutManager
            ?: return false
    val firstVisiblePosition = (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
    val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
    return position in firstVisiblePosition..lastVisiblePosition
}