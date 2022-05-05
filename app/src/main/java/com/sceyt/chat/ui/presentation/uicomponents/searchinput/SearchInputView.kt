package com.sceyt.chat.ui.presentation.uicomponents.searchinput

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.SceytUiSearchViewBinding
import com.sceyt.chat.ui.extensions.getCompatColor
import com.sceyt.chat.ui.extensions.getCompatDrawable
import com.sceyt.chat.ui.sceytconfigs.SearchInputViewStyle
import com.sceyt.chat.ui.utils.BindingUtil


class SearchInputView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private companion object {
        private const val TYPING_DEBOUNCE_MS = 300L
    }

    private var binding: SceytUiSearchViewBinding

    private val debounceInitDelegate = lazy { DebounceHelper(TYPING_DEBOUNCE_MS) }
    private val debounceHelper by debounceInitDelegate

    private var debouncedInputChangedListener: InputChangedListener? = null
    private var inputChangedListener: InputChangedListener? = null
    private var querySubmitListener: InputTextSubmitListener? = null

    private val query: String
        get() = binding.input.text.toString().trim()

    private var disableDebouncedSearchDuringTyping = false

    init {
        isSaveFromParentEnabled = false
        binding = SceytUiSearchViewBinding.inflate(LayoutInflater.from(context), this, true)
        BindingUtil.themedBackgroundColor(this, R.color.whiteThemed)

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.SearchInputView)
            SearchInputViewStyle.updateWithAttributes(context, a)
            a.recycle()
        }
        init()
    }

    private fun init() {
        binding.setWithStyle()

        binding.input.doAfterTextChanged { query ->
            binding.icClear.isVisible = query?.length ?: 0 > 0
            onQueryChanged(query.toString())
        }

        binding.input.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    querySubmitListener?.onQueryTextSubmit(query)
                    true
                }
                else -> false
            }
        }

        binding.icClear.setOnClickListener {
            binding.input.setText("")
            if (disableDebouncedSearchDuringTyping)
                querySubmitListener?.onQueryTextSubmit("")
        }
    }

    private fun SceytUiSearchViewBinding.setWithStyle() {
        icSearch.setImageResource(SearchInputViewStyle.searchIcon)
        icClear.setImageResource(SearchInputViewStyle.clearIcon)
        input.setTextColor(context.getCompatColor(SearchInputViewStyle.textColor))
        input.hint = SearchInputViewStyle.hintText
        input.setHintTextColor(context.getCompatColor(SearchInputViewStyle.hintTextColor))
        rootLayout.background = context.getCompatDrawable(R.drawable.bg_search_view)
        disableDebouncedSearchDuringTyping = SearchInputViewStyle.disableDebouncedSearch
    }

    private fun onQueryChanged(newQuery: String) {
        inputChangedListener?.onInputChanged(newQuery)
        if (!disableDebouncedSearchDuringTyping)
            debounceHelper.submit {
                debouncedInputChangedListener?.onInputChanged(newQuery)
            }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (debounceInitDelegate.isInitialized())
            debounceHelper.shutdown()
    }

    fun setDebouncedTextChangeListener(inputChangedListener: InputChangedListener) {
        debouncedInputChangedListener = inputChangedListener
    }

    fun setOnQuerySubmitListener(listener: InputTextSubmitListener) {
        querySubmitListener = listener
    }

    /**
     * Listener that exposes a handle when the input changes.
     */
    fun interface InputChangedListener {
        /**
         * Handle when the input changes.
         * @param query The current query value.
         */
        fun onInputChanged(query: String)
    }

    /**
     * Listener that exposes a handle when the input text submitted.
     */
    fun interface InputTextSubmitListener {
        /**
         * Handle when the input submit.
         * @param query The current query value.
         */
        fun onQueryTextSubmit(query: String)
    }
}