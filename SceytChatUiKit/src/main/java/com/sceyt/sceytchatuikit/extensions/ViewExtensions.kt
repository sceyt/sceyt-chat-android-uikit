package com.sceyt.sceytchatuikit.extensions

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.sceytchatuikit.presentation.common.ClickAvailableData
import com.sceyt.sceytchatuikit.shared.utils.ViewEnabledUtils
import kotlinx.coroutines.*
import kotlin.math.roundToInt

fun View.addPaddings(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0) {
    setPadding(paddingLeft + left, paddingTop + top, paddingRight + right, paddingBottom + bottom)
}

fun pxToDp(px: Float): Float {
    val densityDpi = Resources.getSystem().displayMetrics.densityDpi
    return px / (densityDpi / 160f)
}

fun dpToPx(dp: Float): Int {
    val density = Resources.getSystem().displayMetrics.density
    return (dp * density).roundToInt()
}

fun View.screenWidthPx() = resources.displayMetrics.widthPixels

fun View.screenHeightPx() = resources.displayMetrics.heightPixels

fun Fragment.screenHeightPx() = resources.displayMetrics.heightPixels

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

fun EditText.setTextAndMoveSelectionEnd(text: String?) {
    text?.let {
        setText(text)
        setSelection(selectionEnd)
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

fun View.setOnClickListenerDisableClickViewForWhile(disableDuration: Long = 1000, onClickCallBack: (View) -> Unit) {
    setOnClickListener {
        ViewEnabledUtils.disableClickViewForWhile(it, disableDuration)
        onClickCallBack.invoke(it)
    }
}

fun TextView.setTextViewDrawableColor(@ColorRes color: Int) {
    for (drawable in compoundDrawables) {
        if (drawable != null)
            drawable.colorFilter = PorterDuffColorFilter(context.getCompatColor(color), PorterDuff.Mode.SRC_IN)
    }
    for (drawable in compoundDrawablesRelative) {
        if (drawable != null)
            drawable.colorFilter = PorterDuffColorFilter(context.getCompatColor(color), PorterDuff.Mode.SRC_IN)
    }
}

fun setTextViewsDrawableColor(texts: List<TextView>, @ColorInt color: Int) {
    texts.forEach {
        it.compoundDrawables.forEach { drawable ->
            drawable?.let {
                drawable.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            }
        }
        it.compoundDrawablesRelative.forEach { drawable ->
            drawable?.let {
                drawable.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            }
        }
    }
}