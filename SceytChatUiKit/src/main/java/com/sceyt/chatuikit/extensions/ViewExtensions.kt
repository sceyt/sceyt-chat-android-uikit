package com.sceyt.chatuikit.extensions

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.ColorStateList
import android.content.res.Resources
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.chatuikit.presentation.common.ClickAvailableData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

fun View.addPaddings(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0) {
    setPadding(paddingLeft + left, paddingTop + top, paddingRight + right, paddingBottom + bottom)
}

fun View.setPaddings(
        left: Int = paddingLeft,
        top: Int = paddingTop,
        right: Int = paddingRight,
        bottom: Int = paddingBottom,
) {
    setPadding(left, top, right, bottom)
}

fun View.setMargins(
        left: Int = marginLeft,
        top: Int = marginTop,
        right: Int = marginRight,
        bottom: Int = marginBottom,
) {
    (layoutParams as? ViewGroup.MarginLayoutParams)?.let {
        it.setMargins(left, top, right, bottom)
        layoutParams = it
    }
}

fun View.setMargins(size: Int) {
    setMargins(size, size, size, size)
}

fun pxToDp(px: Float): Float {
    val densityDpi = Resources.getSystem().displayMetrics.densityDpi
    return px / (densityDpi / 160f)
}

fun dpToPx(dp: Float): Int {
    val density = Resources.getSystem().displayMetrics.density
    return (dp * density).roundToInt()
}

fun dpToPxAsFloat(dp: Float): Float {
    val density = Resources.getSystem().displayMetrics.density
    return dp * density
}

fun View.screenWidthPx() = resources.displayMetrics.widthPixels

fun View.screenHeightPx() = resources.displayMetrics.heightPixels

fun View.delayOnLifecycle(
        delayMillis: Long,
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        block: () -> Unit,
): Job? = findViewTreeLifecycleOwner()?.let { lifecycleOwner ->
    lifecycleOwner.lifecycle.coroutineScope.launch(dispatcher) {
        delay(delayMillis)
        block()
    }
}

fun View.invokeSuspendInLifecycle(
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        block: suspend () -> Unit,
): Job? = findViewTreeLifecycleOwner()?.let { lifecycleOwner ->
    lifecycleOwner.lifecycle.coroutineScope.launch(dispatcher) {
        block()
    }
}

fun View.getLifecycleScope() = findViewTreeLifecycleOwner()?.lifecycleScope

fun EditText.setTextAndMoveSelectionEnd(text: CharSequence?) {
    text?.let {
        setText(text)
        setSelection(text.length)
    }
}

fun View.getString(@StringRes resId: Int) = context.getString(resId)

fun View.getString(@StringRes resId: Int, vararg formatArgs: Any?) = context.getString(resId, *formatArgs)

@SuppressLint("ClickableViewAccessibility")
fun SwitchCompat.setOnlyClickable() {
    setOnTouchListener { _, event ->
        if (event.action == MotionEvent.ACTION_UP)
            callOnClick()
        return@setOnTouchListener true
    }
}

fun View.setOnClickListenerAvailable(clockAvailableData: ClickAvailableData, disableDuration: Long = 1000, onClickCallBack: (View) -> Unit) {
    setOnClickListener {
        if (clockAvailableData.isAvailable) {
            clockAvailableData.isAvailable = false
            onClickCallBack.invoke(it)
            Handler(Looper.getMainLooper()).postDelayed({
                clockAvailableData.isAvailable = true
            }, disableDuration)
        }
    }
}

fun View.setOnLongClickListenerAvailable(clockAvailableData: ClickAvailableData, disableDuration: Long = 1000, onClickCallBack: (View) -> Unit) {
    setOnLongClickListener {
        if (clockAvailableData.isAvailable) {
            clockAvailableData.isAvailable = false
            onClickCallBack.invoke(it)
            Handler(Looper.getMainLooper()).postDelayed({
                clockAvailableData.isAvailable = true
            }, disableDuration)
        }
        return@setOnLongClickListener true
    }
}

fun View.setSafeOnClickListener(disableDuration: Long = 1000, onSafeClick: (View) -> Unit) {
    var lastClickTime = 0L
    val lock = Any()
    setOnClickListener { view ->
        val currentTime = System.currentTimeMillis()
        synchronized(lock) {
            if (currentTime - lastClickTime >= disableDuration) {
                lastClickTime = currentTime
                onSafeClick(view)
            }
        }
    }
}

fun ImageView.setTint(@ColorInt color: Int) {
    setColorFilter(color)
}

fun ImageView.setTintColorRes(@ColorRes colorId: Int) {
    setColorFilter(context.getCompatColor(colorId))
}

fun View.setBackgroundTint(@ColorInt color: Int) {
    backgroundTintList = ColorStateList.valueOf(color)
}

fun View.setBackgroundTintColorRes(@ColorRes color: Int) {
    backgroundTintList = ColorStateList.valueOf(context.getCompatColor(color))
}

fun TextView.setTextColorRes(@ColorRes color: Int) {
    setTextColor(context.getCompatColor(color))
}

fun TextView.setHintColorRes(@ColorRes color: Int) {
    setHintTextColor(context.getCompatColor(color))
}

val View.marginHorizontal
    get() = marginStart + marginEnd

fun ProgressBar.setProgressColorRes(@ColorRes colorId: Int) {
    indeterminateDrawable.setTint(context.getCompatColor(colorId))
}

fun ProgressBar.setProgressColor(@ColorInt color: Int) {
    indeterminateDrawable.setTint(color)
}

@Suppress("DEPRECATION")
fun TextPaint.getStaticLayout(title: CharSequence, includePadding: Boolean, textWidth: Int): StaticLayout {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        StaticLayout.Builder.obtain(title, 0, title.length, this, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(includePadding).build()
    } else StaticLayout(title, this, textWidth, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, includePadding)
}

fun View.checkIfInsideFragment(): Fragment? {
    // Check if the context is an activity
    return context.maybeFragmentActivity()?.let {
        findFragment(it)
    }
}

private fun View.findFragment(activity: FragmentActivity): Fragment? {
    val fragmentManager = activity.supportFragmentManager

    // Traverse all fragments and find the one that contains this view
    fragmentManager.fragments.forEach { fragment ->
        if (isViewInFragment(this, fragment)) {
            return fragment
        }
    }
    return null
}

private fun isViewInFragment(view: View, fragment: Fragment): Boolean {
    val fragmentView = fragment.view
    return fragmentView != null && isViewInHierarchy(view, fragmentView)
}

private fun isViewInHierarchy(view: View, parent: View): Boolean {
    if (view == parent) {
        return true
    }
    if (parent is ViewGroup) {
        for (i in 0 until parent.childCount) {
            if (isViewInHierarchy(view, parent.getChildAt(i))) {
                return true
            }
        }
    }
    return false
}

fun View.getScope(): LifecycleCoroutineScope {
    return (findViewTreeLifecycleOwner() ?: context.asComponentActivity()).lifecycleScope
}


fun View.hideSoftInput() {
    val inputMethodManager = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}

fun ViewGroup.setLayoutTransition(
        duration: Long = 200,
        type: Int = LayoutTransition.DISAPPEARING,
) {
    layoutTransition = LayoutTransition().apply {
        disableTransitionType(type)
        setDuration(duration)
    }
}