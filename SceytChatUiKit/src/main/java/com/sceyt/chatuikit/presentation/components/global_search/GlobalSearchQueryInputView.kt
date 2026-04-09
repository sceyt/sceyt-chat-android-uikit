package com.sceyt.chatuikit.presentation.components.global_search

import android.animation.ArgbEvaluator
import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.text.Editable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.databinding.SceytGlobalSearchQueryInputViewBinding
import com.sceyt.chatuikit.extensions.hideSoftInput
import com.sceyt.chatuikit.extensions.setBackgroundTint
import com.sceyt.chatuikit.extensions.showSoftInput
import com.sceyt.chatuikit.styles.search.GlobalSearchStyle

open class GlobalSearchQueryInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    companion object {
        const val SHARED_TRANSITION_NAME = GlobalSearchActivity.SHARED_TRANSITION_NAME
    }

    private val binding = SceytGlobalSearchQueryInputViewBinding.inflate(
        LayoutInflater.from(context), this
    )
    private var style: GlobalSearchStyle? = null

    private var queryChangedListener: ((String) -> Unit)? = null
    private var clearClickListener: (() -> Unit)? = null
    private var emptyDeleteListener: (() -> Unit)? = null
    private var suppressQueryChanged = false
    private var selectedMemberId: String? = null
    private var isSelectedMemberRemovalPending = false
    private var chipStyleAnimator: ValueAnimator? = null

    init {
        setBackgroundResource(R.drawable.sceyt_bg_corners_10)
        elevation = 0f
        ViewCompat.setTransitionName(binding.root, SHARED_TRANSITION_NAME)

        binding.layoutDetails.layoutTransition =
            LayoutTransition().let {
                it.enableTransitionType(LayoutTransition.CHANGING)
                it.setAnimateParentHierarchy(false)
                it.setDuration(150L)
                it
            }

        binding.input.apply {
            emptyDeleteListener = {
                this@GlobalSearchQueryInputView.emptyDeleteListener?.invoke()
                true
            }
            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_SEARCH -> {
                        hideSoftInput()
                        true
                    }

                    else -> false
                }
            }
            doAfterTextChanged { text: Editable? ->
                if (!suppressQueryChanged) {
                    queryChangedListener?.invoke(text?.toString().orEmpty())
                }
                updateClearVisibility()
            }
        }

        binding.icClear.setOnClickListener {
            clearClickListener?.invoke()
        }
        updateClearVisibility()
    }

    open fun applyStyle(style: GlobalSearchStyle) {
        this.style = style
        backgroundTintList = ColorStateList.valueOf(style.searchInputBackgroundColor)
        binding.icSearch.setColorFilter(style.searchIconColor)
        binding.icClear.setColorFilter(style.clearIconColor)
        binding.input.apply {
            setHintTextColor(style.searchHintColor)
            style.subtitleTextStyle.apply(this)
        }
        applyChipStyle(isSelectedMemberRemovalPending)
    }

    fun setOnQueryChangedListener(listener: (String) -> Unit) {
        queryChangedListener = listener
    }

    fun setOnClearClickListener(listener: () -> Unit) {
        clearClickListener = listener
    }

    fun setOnEmptyDeleteListener(listener: () -> Unit) {
        emptyDeleteListener = listener
    }

    fun setQuery(query: String) {
        if (binding.input.text?.toString() == query) return
        suppressQueryChanged = true
        binding.input.setText(query)
        binding.input.setSelection(query.length)
        suppressQueryChanged = false
        updateClearVisibility()
    }

    fun setSelectedMember(
        member: SceytUser?,
        isPendingRemoval: Boolean = false,
    ) {
        val currentQuery = binding.input.text?.toString().orEmpty()
        val nextMemberId = member?.id
        val nextName = member?.displayName().orEmpty()
        val shouldBeVisible = member != null
        val previousPendingRemoval = isSelectedMemberRemovalPending
        val shouldAnimateChipState =
            binding.chipSelectedMember.isVisible &&
                    shouldBeVisible &&
                    nextMemberId == selectedMemberId &&
                    isPendingRemoval != previousPendingRemoval

        if (nextMemberId == selectedMemberId &&
            binding.chipSelectedMember.isVisible == shouldBeVisible &&
            binding.chipSelectedMember.text?.toString() == nextName &&
            isPendingRemoval == previousPendingRemoval
        ) {
            restoreQuery(currentQuery)
            updateClearVisibility()
            return
        }

        selectedMemberId = nextMemberId
        binding.chipSelectedMember.text = nextName
        binding.chipSelectedMember.isVisible = shouldBeVisible
        if (shouldBeVisible) {
            if (shouldAnimateChipState) {
                animateChipStyle(previousPendingRemoval, isPendingRemoval)
            } else {
                applyChipStyle(isPendingRemoval)
            }
        } else {
            chipStyleAnimator?.cancel()
            applyChipStyle(isPendingRemoval = false)
        }
        isSelectedMemberRemovalPending = shouldBeVisible && isPendingRemoval
        restoreQuery(currentQuery)
        updateClearVisibility()
    }

    fun focusInput() {
        context.showSoftInput(binding.input)
    }

    private fun updateClearVisibility() {
        binding.icClear.isVisible =
            binding.input.text?.isNotEmpty() == true || binding.chipSelectedMember.isVisible
    }

    private fun requireStyle(): GlobalSearchStyle {
        return checkNotNull(style) {
            "GlobalSearchQueryInputView style must be applied before use."
        }
    }

    private fun animateChipStyle(
        wasPendingRemoval: Boolean,
        isPendingRemoval: Boolean,
    ) {
        val style = requireStyle()
        chipStyleAnimator?.cancel()

        val fromBackground = if (wasPendingRemoval) {
            style.selectedMemberChipPendingBackgroundColor
        } else {
            style.selectedMemberChipBackgroundColor
        }
        val toBackground = if (isPendingRemoval) {
            style.selectedMemberChipPendingBackgroundColor
        } else {
            style.selectedMemberChipBackgroundColor
        }
        val fromText = if (wasPendingRemoval) {
            style.selectedMemberChipPendingTextColor
        } else {
            style.selectedMemberChipTextColor
        }
        val toText = if (isPendingRemoval) {
            style.selectedMemberChipPendingTextColor
        } else {
            style.selectedMemberChipTextColor
        }

        chipStyleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 180L
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                applyChipColors(
                    backgroundColor = ArgbEvaluator().evaluate(
                        fraction,
                        fromBackground,
                        toBackground
                    ) as Int,
                    textColor = ArgbEvaluator().evaluate(fraction, fromText, toText) as Int,
                )
            }
            doOnEnd {
                applyChipStyle(isPendingRemoval)
            }
            start()
        }
    }

    private fun applyChipStyle(isPendingRemoval: Boolean) {
        val style = requireStyle()
        applyChipColors(
            backgroundColor = if (isPendingRemoval) {
                style.selectedMemberChipPendingBackgroundColor
            } else {
                style.selectedMemberChipBackgroundColor
            },
            textColor = if (isPendingRemoval) {
                style.selectedMemberChipPendingTextColor
            } else {
                style.selectedMemberChipTextColor
            }
        )
    }

    private fun applyChipColors(
        backgroundColor: Int,
        textColor: Int,
    ) {
        binding.chipSelectedMember.apply {
            setBackgroundTint(backgroundColor)
            setTextColor(textColor)
        }
    }

    private fun restoreQuery(query: String) {
        if (binding.input.text?.toString() != query) {
            suppressQueryChanged = true
            binding.input.setText(query)
            suppressQueryChanged = false
        }
        binding.input.post {
            val text = binding.input.text?.toString().orEmpty()
            if (text == query) {
                binding.input.setSelection(text.length)
            }
        }
    }
}
