package com.sceyt.chatuikit.presentation.components.global_search.input

import android.animation.ArgbEvaluator
import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.text.Editable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.databinding.SceytGlobalSearchQueryInputViewBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.hideSoftInput
import com.sceyt.chatuikit.extensions.setBackgroundTint
import com.sceyt.chatuikit.extensions.showSoftInput
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchActivity
import com.sceyt.chatuikit.presentation.components.global_search.displayName
import com.sceyt.chatuikit.styles.search.GlobalSearchInputStyle

class GlobalSearchQueryInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {
    companion object {
        const val SHARED_TRANSITION_NAME = GlobalSearchActivity.SHARED_TRANSITION_NAME
    }

    private val binding = SceytGlobalSearchQueryInputViewBinding.inflate(
        LayoutInflater.from(context), this
    )
    private var style: GlobalSearchInputStyle? = null

    private var queryChangedListener: ((String) -> Unit)? = null
    private var clearClickListener: (() -> Unit)? = null
    private var emptyDeleteListener: (() -> Unit)? = null
    private var suppressQueryChanged = false
    private var selectedMemberId: String? = null
    private var isSelectedMemberRemovalPending = false
    private var chipStyleAnimator: ValueAnimator? = null

    init {
        if (isInEditMode) {
            setBackgroundResource(R.drawable.sceyt_bg_corners_10)
            backgroundTintList =
                ColorStateList.valueOf(getCompatColor(SceytChatUIKit.theme.colors.surface1Color))
        }
        elevation = 0f
        ViewCompat.setTransitionName(this, SHARED_TRANSITION_NAME)

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

    fun applyStyle(style: GlobalSearchInputStyle) {
        this.style = style
        style.searchInputStyle.apply(
            editText = binding.input,
            inputRoot = this,
            searchIconImage = binding.icSearch,
            clearIconImage = binding.icClear
        )
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
            binding.selectedUserContainer.isVisible &&
                    shouldBeVisible &&
                    nextMemberId == selectedMemberId &&
                    isPendingRemoval != previousPendingRemoval

        if (nextMemberId == selectedMemberId &&
            binding.selectedUserContainer.isVisible == shouldBeVisible &&
            binding.name.text?.toString() == nextName &&
            isPendingRemoval == previousPendingRemoval
        ) {
            restoreQuery(currentQuery)
            updateClearVisibility()
            return
        }

        selectedMemberId = nextMemberId
        binding.name.text = nextName

        if (member != null) {
            style?.let { inputStyle ->
                inputStyle.avatarRenderer.render(
                    context,
                    from = member,
                    style = inputStyle.avatarStyle,
                    avatarView = binding.avatar
                )
            }
        }
        setSelectedUserContainerVisibility(shouldBeVisible)
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

    fun setSelectedUserContainerVisibility(visible: Boolean) {
        if (binding.selectedUserContainer.isVisible == visible) return
        layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
            setAnimateParentHierarchy(false)
            setDuration(150L)
        }
        binding.selectedUserContainer.isVisible = visible
    }

    fun focusInput() {
        context.showSoftInput(binding.input)
    }

    private fun updateClearVisibility() {
        binding.icClear.isVisible =
            binding.input.text?.isNotEmpty() == true || binding.selectedUserContainer.isVisible
    }

    private fun requireStyle(): GlobalSearchInputStyle {
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
            style.selectedUserChipPendingBackgroundStyle.backgroundColor
        } else {
            style.selectedUserChipBackgroundStyle.backgroundColor
        }
        val toBackground = if (isPendingRemoval) {
            style.selectedUserChipPendingBackgroundStyle.backgroundColor
        } else {
            style.selectedUserChipBackgroundStyle.backgroundColor
        }
        val fromText = if (wasPendingRemoval) {
            style.selectedUserChipPendingTextStyle.color
        } else {
            style.selectedUserChipTextStyle.color
        }
        val toText = if (isPendingRemoval) {
            style.selectedUserChipPendingTextStyle.color
        } else {
            style.selectedUserChipTextStyle.color
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
        val bgStyle = if (isPendingRemoval)
            style.selectedUserChipPendingBackgroundStyle else style.selectedUserChipBackgroundStyle
        val textStyle = if (isPendingRemoval)
            style.selectedUserChipPendingTextStyle else style.selectedUserChipTextStyle
        bgStyle.apply(binding.selectedUserContainer)
        textStyle.apply(binding.name)
    }

    private fun applyChipColors(
        backgroundColor: Int,
        textColor: Int,
    ) {
        binding.selectedUserContainer.setBackgroundTint(backgroundColor)
        binding.name.setTextColor(textColor)
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