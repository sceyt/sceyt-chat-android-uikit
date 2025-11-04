package com.sceyt.chatuikit.presentation.components.create_poll.adapters

import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemCreatePollOptionBinding
import com.sceyt.chatuikit.extensions.setCursorAndHandleColorRes
import com.sceyt.chatuikit.extensions.showSoftInput
import com.sceyt.chatuikit.presentation.components.create_poll.PollOptionItem
import com.sceyt.chatuikit.styles.create_poll.CreatePollStyle

class PollOptionViewHolder(
        private val binding: SceytItemCreatePollOptionBinding,
        private val style: CreatePollStyle,
        private val onTextChanged: (PollOptionItem, String) -> Unit,
        private val onRemoveClick: (PollOptionItem) -> Unit,
        private val onNextClick: (PollOptionItem) -> Unit,
        private val onOptionClick: (EditText, PollOptionItem) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    private var currentOption: PollOptionItem? = null
    private var textWatcher: TextWatcher? = null
    private var delayRunnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())

    init {
        applyStyle()
        binding.tvOption.setRawInputType(InputType.TYPE_CLASS_TEXT)
    }


    fun bind(option: PollOptionItem) = with(binding) {
        currentOption = option

        with(tvOption) {
            setText(option.text)
            imeOptions = option.keyboardAction

            // Add editor action listener for "Next" action
            addOnEditorActionListener(option)

            // Set focusable based on whether this is the current option
            isFocusable = option.isCurrent
            isFocusableInTouchMode = option.isCurrent

            // Place cursor at the end and show keyboard if this is the current option
            if (option.isCurrent) {
                setSelection(option.text.length)
                context.showSoftInput(binding.tvOption)

                // Add or remove onKeyListener based on text content
                if (option.text.isBlank())
                    addRemoveOptionOnKeyListener(option)
            }

            // Remove previous text watcher if exists
            removeTextWatcher()

            // Add new text watcher
            textWatcher = tvOption.doAfterTextChanged {
                onTextChanged(option, it?.toString().orEmpty())
                addRemoveOptionOnKeyListener(option)
            }

            // Set click listener to notify option click
            setOnClickListener {
                onOptionClick(this, option)
            }
        }
    }

    private fun addRemoveOptionOnKeyListener(option: PollOptionItem) {
        // Remove any existing delayed runnable
        removeRunnable()

        // If the option text is empty, add a delayed runnable to set the onKeyListener
        if (binding.tvOption.text.isNullOrEmpty()) {
            // Use handler to delay adding the onKeyListener to prevent immediate deletion
            val newRunnable = Runnable {
                binding.tvOption.addOnKeyListener(option)
            }
            delayRunnable = newRunnable
            handler.postDelayed(newRunnable, 100)
        } else {
            removeOnKeyListener()
        }
    }

    private fun EditText.addOnKeyListener(option: PollOptionItem) {
        setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                if (text.isNullOrEmpty()) {
                    onRemoveClick(option)
                    return@setOnKeyListener true
                }
            }
            false
        }
    }

    private fun EditText.addOnEditorActionListener(option: PollOptionItem) {
        setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                onNextClick(option)
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun removeOnKeyListener() {
        binding.tvOption.setOnKeyListener(null)
    }

    private fun removeOnEditorActionListener() {
        binding.tvOption.setOnEditorActionListener(null)
    }

    private fun removeTextWatcher() {
        textWatcher?.let { watcher ->
            binding.tvOption.removeTextChangedListener(watcher)
        }
        textWatcher = null
    }

    private fun removeRunnable() {
        delayRunnable?.let {
            handler.removeCallbacks(it)
        }
        delayRunnable = null
    }

    fun unbind() {
        removeTextWatcher()
        removeRunnable()
        removeOnKeyListener()
        removeOnEditorActionListener()
        currentOption = null
    }

    private fun applyStyle() = with(binding) {
        style.optionInputTextStyle.apply(tvOption, root)
        icDrag.setImageDrawable(style.dragIcon)
        tvOption.setCursorAndHandleColorRes(SceytChatUIKit.theme.colors.accentColor)
    }
}

