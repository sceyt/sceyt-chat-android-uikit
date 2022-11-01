package com.sceyt.sceytchatuikit.extensions

import android.os.Handler
import android.os.Looper
import android.view.ViewTreeObserver
import androidx.recyclerview.widget.*

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
    if (firstVisibleItemPosition != RecyclerView.NO_POSITION && firstVisibleItemPosition < limit) {
        return true
    }
    return false
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

fun RecyclerView.checkIsNotCompletelyVisibleItem(position: Int): Boolean {
    return ((layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition() > position
            || (layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition() < position)
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
    if (!checkIsNotCompletelyVisibleItem(position)) {
        if (delay)
            Handler(Looper.getMainLooper()).postDelayed({ callback.invoke(scrollState) }, 100)
        else callback.invoke(scrollState)
    } else {
        addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    callback.invoke(newState)
                    removeOnScrollListener(this)
                }
            }
        })
    }
}


inline fun RecyclerView.addRVScrollListener(crossinline onScrollStateChanged: (recyclerView: RecyclerView, newState: Int) -> Unit = { _, _ -> },
                                            crossinline onScrolled: (recyclerView: RecyclerView, dx: Int, dy: Int) -> Unit = { _, _, _ -> }) {
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

fun RecyclerView.runWhenReady(action: () -> Unit) {
    if (!isComputingLayout)
        action()
    else {
        val globalLayoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                action()
                viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        }
        viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
    }
}

fun DiffUtil.DiffResult.dispatchUpdatesToSafety(recyclerView: RecyclerView) {
    recyclerView.adapter?.let { adapter->
        recyclerView.runWhenReady {
            dispatchUpdatesTo(adapter)
        }
    }
}