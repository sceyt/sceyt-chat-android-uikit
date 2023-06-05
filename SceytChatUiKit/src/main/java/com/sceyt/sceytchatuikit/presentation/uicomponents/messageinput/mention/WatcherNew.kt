package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention

import android.text.Annotation
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import androidx.core.text.getSpans

class WatcherNew(private val editText: EditText) : TextWatcher {
    val defaultInputTYPE = editText.inputType
    private var isDragged: Boolean = false
    override fun onTextChanged(sequence: CharSequence, start: Int, before: Int, count: Int) {

    }

    override fun afterTextChanged(inputText: Editable) {
        doLogic(inputText, true)
    }

    private fun doLogic(inputText: Editable?, setSelection: Boolean) {
        inputText ?: return

        val selectionEnd = editText.selectionEnd
        val spans = inputText.getSpans<Annotation>()
        if (spans.isEmpty()) {
            setDefaultType("set input default empty", setSelection)
            return
        }
        if (selectionEnd == 0) return
        for ((index, span) in spans.reversed().withIndex()) {
            val spanEnd = inputText.getSpanEnd(span)

            if (spanEnd > selectionEnd) continue

            if (spanEnd == selectionEnd) {
                setNotSuggestionsType("set input no suggest equal", setSelection)
                break
            } else {
                /* if (inputText.lastIndexOf(' ') > spanEnd) {
                     if (index == spans.lastIndex) {
                         setDefaultType("set input default last index")
                     }
                     continue
                 }*/

                var handledBreak = false
                tt@ for (i in selectionEnd downTo spanEnd) {
                    if (inputText.getOrNull(i) == '\n' || inputText.getOrNull(i) == ' ') {
                        handledBreak = true
                        break@tt
                    }
                }
                if (handledBreak) {
                    setDefaultType("set input default handled break", setSelection)

                } else {
                    setNotSuggestionsType("set input no suggest not handled break", setSelection)
                }
                break
            }
        }
        isDragged = false
    }


    override fun beforeTextChanged(sequence: CharSequence, start: Int, count: Int, after: Int) {

    }

    fun cursorChanged() {
        isDragged = true
    }

    private fun setDefaultType(message: String, setSelection: Boolean) {
        with(editText) {
            if (inputType == InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) {
                val selectionEnd = selectionEnd
                inputType = defaultInputTYPE
                if (setSelection)
                    setSelection(selectionEnd)
                Log.i("fsdfsdfsd", message)
            }
        }
    }

    private fun setNotSuggestionsType(message: String, setSelection: Boolean) {
        with(editText) {
            if (inputType == defaultInputTYPE) {
                val selectionEnd = selectionEnd
                inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                isSingleLine = false
                if (setSelection)
                    setSelection(selectionEnd)
                Log.i("fsdfsdfsd", message)
            }
        }
    }
}