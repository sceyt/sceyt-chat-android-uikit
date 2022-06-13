package com.sceyt.chat.ui.presentation.uicomponents.searchinput

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.SceytSearchViewBinding
import com.sceyt.chat.ui.extensions.getCompatColor
import com.sceyt.chat.ui.presentation.uicomponents.searchinput.listeners.SearchInputClickListeners
import com.sceyt.chat.ui.presentation.uicomponents.searchinput.listeners.SearchInputClickListenersImpl
import com.sceyt.chat.ui.presentation.uicomponents.searchinput.listeners.SearchInputEventListeners
import com.sceyt.chat.ui.presentation.uicomponents.searchinput.listeners.SearchInputEventListenersImpl
import com.sceyt.chat.ui.sceytconfigs.SearchInputViewStyle
import com.sceyt.chat.ui.utils.binding.BindingUtil


class SearchInputView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr), SearchInputClickListeners.ClickListeners,
        SearchInputEventListeners.EventListeners {

    private companion object {
        private const val TYPING_DEBOUNCE_MS = 300L
    }

    private var binding: SceytSearchViewBinding

    private val debounceInitDelegate = lazy { DebounceHelper(TYPING_DEBOUNCE_MS) }
    private val debounceHelper by debounceInitDelegate

    private val clickListeners = SearchInputClickListenersImpl(this)
    private var eventListeners = SearchInputEventListenersImpl(this)
    private var debouncedInputChangedListener: InputChangedListener? = null
    private var inputChangedListener: InputChangedListener? = null
    private var querySubmitListener: InputTextSubmitListener? = null

    private val query: String
        get() = binding.input.text.toString().trim()

    private var disableDebouncedSearchDuringTyping = false

    init {
        isSaveFromParentEnabled = false
        binding = SceytSearchViewBinding.inflate(LayoutInflater.from(context), this, true)
        if (!isInEditMode)
            BindingUtil.themedBackgroundColor(this, R.color.sceyt_color_bg)

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.SearchInputView)
            SearchInputViewStyle.updateWithAttributes(context, a)
            a.recycle()
        }
        init()
    }

    private fun init() {
        binding.setUpStyle()

        binding.input.doAfterTextChanged { query ->
            binding.icClear.isVisible = (query?.length ?: 0) > 0
            onQueryChanged(query.toString())
        }

        binding.input.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    clickListeners.onKeyboardSearchClick()
                    true
                }
                else -> false
            }
        }

        binding.icClear.setOnClickListener {
            clickListeners.onClearClick(it)
        }
    }

    private fun SceytSearchViewBinding.setUpStyle() {
        icSearch.setImageResource(SearchInputViewStyle.searchIcon)
        icClear.setImageResource(SearchInputViewStyle.clearIcon)
        input.setTextColor(context.getCompatColor(SearchInputViewStyle.textColor))
        input.hint = SearchInputViewStyle.hintText
        input.setHintTextColor(context.getCompatColor(SearchInputViewStyle.hintTextColor))
        disableDebouncedSearchDuringTyping = SearchInputViewStyle.disableDebouncedSearch
        if (!isInEditMode)
            BindingUtil.themedBackgroundTintColor(rootLayout, SearchInputViewStyle.backgroundColor)
    }

    private fun handleClearClick() {
        binding.input.setText("")
        if (disableDebouncedSearchDuringTyping)
            eventListeners.onSearchSubmitted("")
    }

    private fun onQueryChanged(newQuery: String) {
        inputChangedListener?.onInputChanged(newQuery)
        if (!disableDebouncedSearchDuringTyping)
            debounceHelper.submit {
                eventListeners.onSearchSubmittedByDebounce(newQuery)
            }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (debounceInitDelegate.isInitialized())
            debounceHelper.shutdown()
    }

    internal fun setDebouncedTextChangeListener(inputChangedListener: InputChangedListener) {
        debouncedInputChangedListener = inputChangedListener
    }

    internal fun setOnQuerySubmitListener(listener: InputTextSubmitListener) {
        querySubmitListener = listener
    }

    fun setTextChangedListener(listener: InputChangedListener) {
        inputChangedListener = listener
    }

    fun setEventListener(listener: SearchInputEventListeners) {
        eventListeners.setListener(listener)
    }

    fun setCustomEventListener(listener: SearchInputEventListenersImpl) {
        eventListeners = listener
    }

    fun interface InputChangedListener {
        fun onInputChanged(query: String)
    }

    fun interface InputTextSubmitListener {
        fun onQueryTextSubmit(query: String)
    }

    override fun onClearClick(view: View) {
        handleClearClick()
    }

    override fun onKeyboardSearchClick() {
        eventListeners.onSearchSubmitted(query)
    }

    //Event listeners
    override fun onSearchSubmitted(query: String) {
        querySubmitListener?.onQueryTextSubmit(query)
    }

    override fun onSearchSubmittedByDebounce(query: String) {
        debouncedInputChangedListener?.onInputChanged(query)
    }
}