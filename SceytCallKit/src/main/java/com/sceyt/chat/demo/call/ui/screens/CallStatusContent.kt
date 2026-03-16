package com.sceyt.chat.demo.call.ui.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.sceyt.chat.call.R
import com.sceyt.chat.demo.call.manager.CallUiState

@Composable
internal fun CallStatusContent(
    callState: CallUiState,
    duration: String,
    fontSize: TextUnit = 16.sp
) {
    val text = when (callState.phase) {
        CallUiState.CallPhase.Incoming -> stringResource(R.string.incoming_call)
        CallUiState.CallPhase.Outgoing ->
            stringResource(
                if (callState.isRemoteRinging)
                    R.string.ringing else R.string.calling
            )

        CallUiState.CallPhase.Connecting -> stringResource(R.string.connecting)
        CallUiState.CallPhase.Connected -> duration
        CallUiState.CallPhase.Reconnecting -> stringResource(R.string.reconnecting)

        else -> return
    }

    Text(
        text = text,
        color = Color.White,
        fontSize = fontSize,
        fontWeight = FontWeight.Normal
    )
}
