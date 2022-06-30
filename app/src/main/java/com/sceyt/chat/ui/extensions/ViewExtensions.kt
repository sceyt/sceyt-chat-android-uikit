package com.sceyt.chat.ui.extensions

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.res.Resources
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.*
import kotlin.math.roundToInt


fun View.addPaddings(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0) {
    setPadding(paddingLeft + left, paddingTop + top, paddingRight + right, paddingBottom + bottom)
}


fun setScaleXYAndRemoveAnimationsViews(scaleX: Float = 1f, scaleY: Float = 1f, vararg view: View) {
    view.forEach {
        it.scaleX = scaleX
        it.scaleY = scaleY
        it.clearAnimation()
    }
}

fun pxToDp(px: Float): Float {
    val densityDpi = Resources.getSystem().displayMetrics.densityDpi
    return px / (densityDpi / 160f)
}

fun dpToPx(dp: Float): Int {
    val density = Resources.getSystem().displayMetrics.density
    return (dp * density).roundToInt()
}

fun View.screenWidthPx() = resources.configuration.screenWidthDp.dpToPx()

fun View.screenHeightPx() = resources.configuration.screenHeightDp.dpToPx()

fun Fragment.screenHeightPx() = resources.configuration.screenHeightDp.dpToPx()


fun EditText.setMultiLineCapSentencesAndSendAction() {
    imeOptions = EditorInfo.IME_ACTION_SEND
    setRawInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE)
}

fun EditText.removeFucus() {
    imeOptions = EditorInfo.IME_ACTION_SEND
    setRawInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE)
}


fun View.delayOnLifecycle(
        delayMillis: Long,
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        block: () -> Unit
): Job? = findViewTreeLifecycleOwner()?.let { lifecycleOwner ->
    lifecycleOwner.lifecycle.coroutineScope.launch(dispatcher) {
        delay(delayMillis)
        block()
    }
}

fun View.invokeSuspendInLifecycle(
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        block: suspend () -> Unit
): Job? = findViewTreeLifecycleOwner()?.let { lifecycleOwner ->
    lifecycleOwner.lifecycle.coroutineScope.launch(dispatcher) {
        block()
    }
}

fun View.getLifecycleScope() = findViewTreeLifecycleOwner()?.lifecycleScope

fun Fragment.delayOnLifecycle(
        delayMillis: Long,
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        block: () -> Unit
): Job = lifecycleScope.launch(dispatcher) {
    delay(delayMillis)
    block()
}

fun AppCompatActivity.delayOnLifecycle(
        delayMillis: Long,
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        block: () -> Unit
): Job = lifecycleScope.launch(dispatcher) {
    delay(delayMillis)
    block()
}


fun EditText.setTextAndMoveSelectionEnd(text: String?) {
    text?.let {
        setText(text)
        setSelection(selectionEnd)
    }
}


fun AppBarLayout.changeAppBarAlpha(view: View) {
    addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, _ ->
        val offsetAlpha: Float = appBarLayout!!.y / totalScrollRange
        view.alpha = 1 - offsetAlpha * -1
    })
}


fun ViewGroup.setTransitionListener(startListener: ((transition: LayoutTransition?, container: ViewGroup?, view: View?, transitionType: Int) -> Unit)? = null,
                                    endListener: ((transition: LayoutTransition?, container: ViewGroup?, view: View?, transitionType: Int) -> Unit)? = null,
                                    removeListener: Boolean = true): LayoutTransition.TransitionListener? {
    var listener: LayoutTransition.TransitionListener? = null
    listener = object : LayoutTransition.TransitionListener {
        override fun startTransition(transition: LayoutTransition?, container: ViewGroup?, view: View?, transitionType: Int) {
            startListener?.invoke(transition, container, view, transitionType)
        }

        override fun endTransition(transition: LayoutTransition?, container: ViewGroup?, view: View?, transitionType: Int) {
            endListener?.invoke(transition, container, view, transitionType)
            if (removeListener)
                layoutTransition.removeTransitionListener(listener)
        }
    }
    layoutTransition.addTransitionListener(listener)
    return listener
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