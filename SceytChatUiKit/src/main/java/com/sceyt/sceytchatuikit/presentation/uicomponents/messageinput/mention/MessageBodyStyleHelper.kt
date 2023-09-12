package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention

import android.text.SpannableString
import com.google.gson.Gson
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.Meta
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style.BodyStyleRange
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style.MessageStyler

object MessageBodyStyleHelper {

    fun buildWithStyle(body: String, metaData: String?): SpannableString {
        val data = getStyleRanges(metaData) ?: return SpannableString(body)
        return try {
            MessageStyler.appendStyle(body, data)
        } catch (e: Exception) {
            e.printStackTrace()
            SpannableString.valueOf(body)
        }
    }

    private fun getStyleRanges(metaData: String?): List<BodyStyleRange>? {
        metaData ?: return null
        return try {
            val data = Gson().fromJson(metaData, Meta::class.java)
            data.style
        } catch (e: Exception) {
            null
        }
    }
}