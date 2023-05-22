package com.sceyt.sceytchatuikit.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.*
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.sceytchatuikit.presentation.common.ClickAvailableData
import com.sceyt.sceytchatuikit.shared.utils.ViewEnabledUtils
import kotlinx.coroutines.*
import kotlin.math.min
import kotlin.math.roundToInt

fun View.addPaddings(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0) {
    setPadding(paddingLeft + left, paddingTop + top, paddingRight + right, paddingBottom + bottom)
}

fun View.setMargins(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0) {
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

fun Context.screenWidthPx() = resources.displayMetrics.widthPixels

fun Context.screenHeightPx() = resources.displayMetrics.heightPixels

fun Context.screenPortraitWidthPx() = min(screenWidthPx(), screenHeightPx())

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

fun View.setOnClickListenerDisableClickViewForWhile(disableDuration: Long = 1000, onClickCallBack: (View) -> Unit) {
    setOnClickListener {
        ViewEnabledUtils.disableClickViewForWhile(it, disableDuration)
        onClickCallBack.invoke(it)
    }
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