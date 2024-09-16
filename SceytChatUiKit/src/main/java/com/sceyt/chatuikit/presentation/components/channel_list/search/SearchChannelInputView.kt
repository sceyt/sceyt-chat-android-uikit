package com.sceyt.chatuikit.presentation.components.channel_list.search

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytSearchViewBinding
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.hideSoftInput
import com.sceyt.chatuikit.extensions.setBackgroundTint
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.SceytDatabase
import com.sceyt.chatuikit.presentation.common.DebounceHelper
import com.sceyt.chatuikit.presentation.components.channel_list.search.listeners.click.SearchInputClickListeners
import com.sceyt.chatuikit.presentation.components.channel_list.search.listeners.click.SearchInputClickListenersImpl
import com.sceyt.chatuikit.presentation.components.channel_list.search.listeners.event.SearchInputEventListeners
import com.sceyt.chatuikit.presentation.components.channel_list.search.listeners.event.SearchInputEventListenersImpl
import com.sceyt.chatuikit.styles.SearchChannelInputStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class SearchChannelInputView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), SearchInputClickListeners.ClickListeners,
        SearchInputEventListeners.EventListeners, SceytKoinComponent {

    private companion object {
        private const val TYPING_DEBOUNCE_MS = 300L
    }

    private val binding: SceytSearchViewBinding
    private val style: SearchChannelInputStyle
    private val debounceInitDelegate = lazy { DebounceHelper(TYPING_DEBOUNCE_MS, this) }
    private val debounceHelper by debounceInitDelegate

    private val clickListeners = SearchInputClickListenersImpl(this)
    private var eventListeners = SearchInputEventListenersImpl(this)
    private var debouncedInputChangedListener: InputChangedListener? = null
    private var inputChangedListener: InputChangedListener? = null
    private var querySubmitListener: InputTextSubmitListener? = null

    private val query: String
        get() = binding.input.text.toString().trim()


    init {
        binding = SceytSearchViewBinding.inflate(LayoutInflater.from(context), this)
        style = SearchChannelInputStyle.Builder(context, attrs).build()
        init()
    }

    private fun init() {
        binding.applyStyle()

        binding.input.doAfterTextChanged { query ->
            binding.icClear.isVisible = (query?.length ?: 0) > 0
            onQueryChanged(query.toString())
        }

        binding.input.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    clickListeners.onKeyboardSearchClick()
                    hideSoftInput()
                    true
                }

                else -> false
            }
        }

        binding.root.setOnLongClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val appDatabase: SceytDatabase by inject()
                appDatabase.clearAllTables()
            }
            Toast.makeText(context, "Database was cleared", Toast.LENGTH_SHORT).show()
            return@setOnLongClickListener false
        }

        binding.icClear.setOnClickListener {
            clickListeners.onClearClick(it)
        }
    }

    private fun SceytSearchViewBinding.applyStyle() {
        icSearch.setImageDrawable(style.searchIcon)
        icClear.setImageDrawable(style.clearIcon)
        input.setTextColor(style.textColor)
        input.hint = style.hintText
        input.setHintTextColor(style.hintTextColor)
        root.background = context.getCompatDrawable(R.drawable.sceyt_bg_corners_10)
        root.setBackgroundTint(style.backgroundColor)
    }

    private fun handleClearClick() {
        binding.input.text = null
        eventListeners.onSearchSubmitted("")
    }

    private fun onQueryChanged(newQuery: String) {
        inputChangedListener?.onInputChanged(newQuery)
        debounceHelper.submit {
            eventListeners.onSearchSubmittedByDebounce(newQuery)
        }
    }

    fun setDebouncedTextChangeListener(inputChangedListener: InputChangedListener) {
        debouncedInputChangedListener = inputChangedListener
    }

    fun setOnQuerySubmitListener(listener: InputTextSubmitListener) {
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

    fun clearSearchAndFocus() {
        binding.input.text = null
        binding.input.clearFocus()
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