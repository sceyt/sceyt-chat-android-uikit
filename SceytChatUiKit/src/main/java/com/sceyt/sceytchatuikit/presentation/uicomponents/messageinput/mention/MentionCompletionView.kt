package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.mentionsrc.CharacterTokenizer
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.mentionsrc.TokenCompleteTextView


class MentionCompletionView : TokenCompleteTextView<MentionUserData> {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    init {
        setTokenizer(CharacterTokenizer(listOf(), ""))
    }

    override fun getViewForObject(obj: MentionUserData): View {
        val token = MentionUserView(context)
        token.text = obj.getMentionedName()
        return token
    }

    fun setMentionUsers(mentionUsers: List<MentionUserData>) {
        initWithObjects(mentionUsers)
    }

    override fun selectionChanges(selStart: Int, selEnd: Int) {

    }

    override fun defaultObject(completionText: String): MentionUserData? {
        return null
    }
}