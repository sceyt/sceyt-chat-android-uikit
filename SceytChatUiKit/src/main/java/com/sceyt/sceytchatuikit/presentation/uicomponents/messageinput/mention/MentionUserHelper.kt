package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention

import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import kotlin.collections.set

object MentionUserHelper {

    fun initMentionMetaData(body: String, mentionUsers: List<MentionUserData>): String {
        if (body.isEmpty() || mentionUsers.isEmpty()) return ""
        val items = mutableMapOf<String, MentionUserMetaDataPayLoad>()
        mentionUsers.forEach {
            val name = it.toString()
            body.indexOf(name).let { index ->
                if (index != -1)
                    items[it.id] = MentionUserMetaDataPayLoad(index, name.length)
            }
        }
        return Gson().toJson(items)
    }

    fun buildWithMentionedUsers(context: Context, body: String, metaData: String?, mentionUsers: Array<User>?): SpannableString {
        metaData ?: return SpannableString(body)
        try {
            val empMapType = object : TypeToken<Map<String, MentionUserMetaDataPayLoad>>() {}.type
            val data: Map<String, MentionUserMetaDataPayLoad> = Gson().fromJson(metaData, empMapType)

            val newBody = SpannableStringBuilder(body)
            data.entries.sortedByDescending { it.value.loc }.forEach {
                val mentionUser = mentionUsers?.find { mentionUser -> mentionUser.id == it.key }
                var name = mentionUser?.let { user ->
                    SceytKitConfig.userNameBuilder?.invoke(user) ?: user.getPresentableName()
                } ?: it.key
                name = "@$name"


                val end = it.value.loc + it.value.len
                if (end > newBody.length)
                    for (i in 0 .. end - newBody.length)
                        newBody.append(" ")

                newBody.replace(it.value.loc, end, name)
                newBody.setSpan(ForegroundColorSpan(context.getCompatColor(SceytKitConfig.sceytColorAccent)), it.value.loc, it.value.loc + name.length, 0)
            }

            return SpannableString.valueOf(newBody)

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return SpannableString(body)
    }

    data class MentionUserMetaDataPayLoad(
            val loc: Int,
            val len: Int
    )
}