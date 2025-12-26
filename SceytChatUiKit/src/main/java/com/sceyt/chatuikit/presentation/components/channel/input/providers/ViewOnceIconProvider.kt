package com.sceyt.chatuikit.presentation.components.channel.input.providers

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.ActionsPosition
import com.sceyt.chatuikit.data.models.channels.InputAction
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.presentation.components.channel.input.data.InputState

const val INPUT_ACTION_VIEW_ONCE_ID = "view_once"
const val INPUT_ACTION_VIEW_ONCE_SELECTED_ID = "view_once_selected"

class ViewOnceIconProvider(
    private val isViewOnceSelected: () -> Boolean = { false },
    private val onActionClick: (InputAction) -> Unit = {}
) : InputActionsProvider {

    override fun getActions(
        context: Context,
        inputState: InputState
    ): List<InputAction> {
        val shouldShowViewOnce = when (inputState) {
            is InputState.Attachments if (inputState.count == 1) -> true
            is InputState.TextWithAttachments if (inputState.attachmentsCount == 1) -> true
            else -> false
        }

        return if (shouldShowViewOnce) {
            val isSelected = isViewOnceSelected()
            listOf(
                InputAction(
                    id = if (isSelected) INPUT_ACTION_VIEW_ONCE_SELECTED_ID else INPUT_ACTION_VIEW_ONCE_ID,
                    position = ActionsPosition.TRAILING,
                    icon = if (isSelected) {
                        context.getCompatDrawable(R.drawable.sceyt_ic_view_once_selected)
                    } else {
                        context.getCompatDrawable(R.drawable.sceyt_ic_view_once)
                    },
                    onClick = onActionClick
                )
            )
        } else emptyList()
    }
}