package com.sceyt.chat.ui.extencions

import android.animation.AnimatorSet
import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.Resources
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
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

fun startObjectAnimDuoChat(context: Context, duration: Long, view: View) {
    val yourProfileWidth = 100
    val yourProfileHeight = 100
    val moveUpY = ObjectAnimator.ofFloat(view, "translationY", yourProfileWidth / 3.8f)
    val moveUpX = ObjectAnimator.ofFloat(view, "translationX", yourProfileHeight / 2.2f)
    moveUpX.duration = duration
    moveUpY.duration = duration
    val scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 0.42f)
    val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 0.42f)
    scaleDownX.duration = duration
    scaleDownY.duration = duration
    val moveUp = AnimatorSet()
    moveUp.play(moveUpY).with(moveUpX).with(scaleDownX).with(scaleDownY)
    moveUp.start()
}

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
        delayMillis: Long,
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

fun EditText.debounceWithLength1(cb: (CharSequence?) -> Unit, textChanged: ((CharSequence?) -> Unit)? = null) {
    findViewTreeLifecycleOwner()?.lifecycleScope?.let {
        callbackFlow {
            val listener = doOnTextChanged { text, _, _, _ ->
                textChanged?.invoke(text)
                trySend(text)
            }
            awaitClose { removeTextChangedListener(listener) }
        }.map { charSequence ->
            if (charSequence?.trim()?.length == 1) {
                cb(charSequence)
            }
            charSequence
        }.debounce(300)
            .onEach { text ->
                text?.let { charSequence ->
                    cb(charSequence)
                }
            }.launchIn(it)
    }
}

fun SearchView.debounce(cb: (CharSequence?) -> Unit, textChanged: ((CharSequence?) -> Unit)? = null, duration: Long = 300) {
    findViewTreeLifecycleOwner()?.lifecycleScope?.let {
        callbackFlow {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    textChanged?.invoke(query)
                    trySend(query)
                    this@debounce.clearFocus()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    textChanged?.invoke(newText)
                    trySend(newText)
                    return true
                }
            })
            awaitClose { }
        }.debounce(duration)
            .onEach { text ->
                text?.let { charSequence ->
                    cb(charSequence)
                }
            }.launchIn(it)
    }
}

fun AppBarLayout.changeAppBarAlpha(view: View) {
    addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, _ ->
        val offsetAlpha: Float = appBarLayout!!.y / totalScrollRange
        view.alpha = 1 - offsetAlpha * -1
    })
}

fun EditText.debounce(cb: (CharSequence?) -> Unit, textChanged: ((CharSequence?) -> Unit)? = null, duration: Long = 300) {
    findViewTreeLifecycleOwner()?.lifecycleScope?.let {
        callbackFlow {
            val listener = doOnTextChanged { text, _, _, _ ->
                textChanged?.invoke(text)
                trySend(text)
            }
            awaitClose { removeTextChangedListener(listener) }
        }.debounce(duration)
            .onEach { text ->
                text?.let { charSequence ->
                    cb(charSequence)
                }
            }.launchIn(it)
    }
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
