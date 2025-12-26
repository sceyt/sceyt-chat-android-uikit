package com.sceyt.chatuikit.presentation.components.channel.messages.preview

import android.animation.ValueAnimator
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.max
import kotlin.math.min

class TextExpandCollapseHelper(
    private val textView: TextView,
    private val scrollView: ScrollView,
    private val containerView: View,
    private val maxExpandedHeightRatio: Float = 0.5f,
    private val collapsedMaxLines: Int = 3,
    private val animDurationMs: Long = 280L,
    private val ellipsis: String = "…"
) {

    private var originalText: String? = null
    private var isExpanded = false
    private var expandingInProgress = false

    fun setText(text: String?) {
        if (text.isNullOrBlank()) {
            originalText = null
            textView.text = ""
            return
        }

        originalText = text
        isExpanded = false
        expandingInProgress = false

        scrollView.scrollTo(0, 0)
        containerView.layoutParams = containerView.layoutParams.apply {
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }

        textView.post {
            applyCollapsed()
        }
    }

    private fun applyCollapsed() {
        val text = originalText ?: return
        val widthPx = textView.width - textView.paddingLeft - textView.paddingRight
        if (widthPx <= 0) return

        isExpanded = false

        val needsTruncate = isMoreThanLines(text, textView.paint, widthPx, collapsedMaxLines)

        if (!needsTruncate) {
            textView.movementMethod = null
            textView.setText(SpannableStringBuilder(text), TextView.BufferType.SPANNABLE)
            textView.isClickable = false
            textView.setOnClickListener(null)
        } else {
            textView.movementMethod = null
            textView.setText(
                buildTruncatedText(
                    fullText = text,
                    paint = textView.paint,
                    widthPx = widthPx,
                    maxLines = collapsedMaxLines
                ), TextView.BufferType.SPANNABLE
            )
            textView.isClickable = true
            textView.setOnClickListener { expand() }
        }
    }

    fun expand() {
        val text = originalText ?: return
        if (isExpanded) return
        
        isExpanded = true
        expandingInProgress = true

        textView.movementMethod = null

        textView.setText(SpannableStringBuilder(text), TextView.BufferType.SPANNABLE)
        textView.isClickable = true

        textView.post { 
            expandingInProgress = false
            if (isExpanded) {
                textView.setOnClickListener { 
                    if (!expandingInProgress) {
                        collapse() 
                    }
                }
            }
        }

        val maxHeight = (textView.resources.displayMetrics.heightPixels * maxExpandedHeightRatio).toInt()

        textView.measure(
            View.MeasureSpec.makeMeasureSpec(textView.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.UNSPECIFIED
        )

        val contentHeight = textView.measuredHeight
        val target = max(1, min(contentHeight, maxHeight))
        val from = max(1, containerView.height)

        animateHeight(containerView, from, target)
    }

    fun collapse() {
        if (!isExpanded) return
        isExpanded = false

        // Remove expanded click first
        textView.setOnClickListener(null)

        scrollView.scrollTo(0, 0)

        applyCollapsed()

        containerView.post {
            containerView.layoutParams = containerView.layoutParams.apply {
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            containerView.measure(
                View.MeasureSpec.makeMeasureSpec(containerView.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.UNSPECIFIED
            )

            val to = max(1, containerView.measuredHeight)
            val from = max(1, containerView.height)

            containerView.layoutParams = containerView.layoutParams.apply {
                height = from
            }
            animateHeight(containerView, from, to)

            containerView.postDelayed({
                if (!isExpanded) {
                    containerView.layoutParams = containerView.layoutParams.apply {
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                }
            }, animDurationMs + 20)
        }
    }

    private fun buildTruncatedText(
        fullText: String,
        paint: TextPaint,
        widthPx: Int,
        maxLines: Int
    ): SpannableStringBuilder {
        var low = 0
        var high = fullText.length
        var best = 0

        while (low <= high) {
            val mid = (low + high) ushr 1
            val candidate = fullText.take(mid).trimEnd() + ellipsis
            if (fitsInLines(candidate, paint, widthPx, maxLines)) {
                best = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        val cut = best.coerceIn(0, fullText.length)
        val truncated = fullText.take(cut).trimEnd()

        return SpannableStringBuilder()
            .append(truncated)
            .append(ellipsis)
    }

    private fun isMoreThanLines(text: String, paint: TextPaint, widthPx: Int, maxLines: Int): Boolean {
        return makeLayout(text, paint, widthPx, Int.MAX_VALUE).lineCount > maxLines
    }

    private fun fitsInLines(text: String, paint: TextPaint, widthPx: Int, maxLines: Int): Boolean {
        val layout = makeLayout(text, paint, widthPx, maxLines)
        if (layout.lineCount > maxLines) return false
        val last = min(maxLines, layout.lineCount) - 1
        return layout.getEllipsisCount(last) == 0
    }

    private fun makeLayout(text: CharSequence, paint: TextPaint, widthPx: Int, maxLines: Int): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, widthPx)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .setLineSpacing(0f, 1f)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .setMaxLines(maxLines)
            .build()
    }

    private fun animateHeight(view: View, fromHeight: Int, toHeight: Int) {
        if (fromHeight == toHeight) return
        ValueAnimator.ofInt(fromHeight, toHeight).apply {
            duration = animDurationMs
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                view.layoutParams = view.layoutParams.apply {
                    height = anim.animatedValue as Int
                }
                view.requestLayout()
            }
            start()
        }
    }
    fun cleanup() {
        textView.setOnClickListener(null)
        textView.movementMethod = null
        originalText = null
    }
}